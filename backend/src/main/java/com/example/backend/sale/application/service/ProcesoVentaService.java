package com.example.backend.sale.application.service;

import com.example.backend.event.infrastructure.persistence.entity.Event;
import com.example.backend.event.infrastructure.persistence.repository.EventRepository;
import com.example.backend.proxy.application.service.ProxyClientService;
import com.example.backend.sale.exception.AsientoNoDisponibleException;
import com.example.backend.sale.exception.VentaFallidaException;
import com.example.backend.sale.infrastructure.client.CatedraApiClient;
import com.example.backend.sale.infrastructure.persistence.entity.Sale;
import com.example.backend.seatSold.infrastructure.persistence.entity.SeatSold;
import com.example.backend.sale.infrastructure.persistence.repository.SaleRepository;
import com.example.backend.seatSold.infrastructure.persistence.repository.SeatSoldRepository;
import com.example.backend.sale.infrastructure.web.dto.*;
import com.example.backend.user.infrastructure.persistence.entity.User;
import com.example.backend.user.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProcesoVentaService {

    private final CatedraApiClient catedraApiClient;
    private final ProxyClientService proxyClientService;
    private final EventRepository eventRepository;
    private final SaleRepository saleRepository;
    private final SeatSoldRepository seatSoldRepository;
    private final UserRepository userRepository;
    private final PendingSaleService pendingSaleService; // <--- INYECTAMOS EL SERVICIO DE REINTENTOS

    public ProcesoVentaService(CatedraApiClient catedraApiClient,
                               ProxyClientService proxyClientService,
                               EventRepository eventRepository,
                               SaleRepository saleRepository,
                               SeatSoldRepository seatSoldRepository,
                               UserRepository userRepository,
                               PendingSaleService pendingSaleService) { // <--- AGREGAR AQUÍ
        this.catedraApiClient = catedraApiClient;
        this.proxyClientService = proxyClientService;
        this.eventRepository = eventRepository;
        this.saleRepository = saleRepository;
        this.seatSoldRepository = seatSoldRepository;
        this.userRepository = userRepository;
        this.pendingSaleService = pendingSaleService; // <--- ASIGNAR AQUÍ
    }

    // --- 1. BLOQUEAR ASIENTOS ---
    public Map<String, Object> bloquearAsientos(BlockRequestDto request) {
        if (request.getEventoId() == null) {
            throw new IllegalArgumentException("Error: eventoId es nulo");
        }
        if (!eventRepository.existsById(request.getEventoId())) {
            throw new RuntimeException("Evento no encontrado localmente");
        }

        Map<String, Object> respuesta = proxyClientService.bloquearAsientos(request);

        if (Boolean.FALSE.equals(respuesta.get("resultado"))) {
            String motivo = (String) respuesta.getOrDefault("descripcion", "No se pudieron bloquear los asientos");
            throw new AsientoNoDisponibleException(motivo);
        }

        return respuesta;
    }

    // --- 2. CONFIRMAR VENTA (Con Reintento) ---
    @Transactional
    public Map<String, Object> confirmarVenta(SaleRequestDto request, String username) {
        // A. Obtener Entidades
        Event evento = eventRepository.findById(request.getEventoId())
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        User usuario = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // B. Preparar Payload Cátedra
        double precioUnitario = (evento.getPrecio() != null) ? evento.getPrecio() : 0.0;
        double precioTotal = precioUnitario * request.getAsientos().size();

        Map<String, Object> payloadCatedra = new HashMap<>();
        payloadCatedra.put("eventoId", evento.getId());
        payloadCatedra.put("fecha", Instant.now().toString());
        payloadCatedra.put("precioVenta", precioTotal);

        List<Map<String, Object>> listaAsientosCatedra = new ArrayList<>();
        for (int i = 0; i < request.getAsientos().size(); i++) {
            SimpleSeatDto asientoDto = request.getAsientos().get(i);
            PersonDto persona = request.getPersonas().get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("fila", asientoDto.getFila());
            map.put("columna", asientoDto.getColumna());
            map.put("persona", persona.getNombre() + " " + persona.getApellido());
            listaAsientosCatedra.add(map);
        }
        payloadCatedra.put("asientos", listaAsientosCatedra);

        // C. Llamada a Cátedra
        Map<String, Object> respuestaCatedra = catedraApiClient.realizarVenta(payloadCatedra);

        if (Boolean.FALSE.equals(respuestaCatedra.get("resultado"))) {
            String motivo = (String) respuestaCatedra.getOrDefault("descripcion", "Rechazado por cátedra");
            throw new VentaFallidaException(motivo);
        }

        // D. Guardado Local con Fallback (Reintento)
        try {
            guardarVentaLocal(evento, usuario, request);
        } catch (Exception e) {
            pendingSaleService.backupSale(request, username);
        }

        return respuestaCatedra;
    }

    // Método auxiliar privado para limpiar el código
    private void guardarVentaLocal(Event evento, User usuario, SaleRequestDto request) {
        Sale venta = new Sale();
        venta.setEvento(evento);
        venta.setUser(usuario);
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado("CONFIRMADA");
        Sale ventaGuardada = saleRepository.save(venta);

        for (int i = 0; i < request.getAsientos().size(); i++) {
            SimpleSeatDto asientoDto = request.getAsientos().get(i);
            PersonDto persona = request.getPersonas().get(i);

            SeatSold asientoVendido = new SeatSold();
            asientoVendido.setVenta(ventaGuardada);
            asientoVendido.setUbicacion("F" + asientoDto.getFila() + "-C" + asientoDto.getColumna());
            asientoVendido.setNombrePersona(persona.getNombre());
            asientoVendido.setApellidoPersona(persona.getApellido());
            seatSoldRepository.save(asientoVendido);
        }
    }

    // --- 3. MÉTODOS DE CONSULTA (RESTORED) ---

    public List<CatedraSaleDto> obtenerHistorialVentas() {
        return catedraApiClient.listarVentasCatedra();
    }

    public CatedraSaleDetailDto obtenerVentaPorId(Long ventaId) {
        return catedraApiClient.obtenerDetalleVenta(ventaId);
    }
}