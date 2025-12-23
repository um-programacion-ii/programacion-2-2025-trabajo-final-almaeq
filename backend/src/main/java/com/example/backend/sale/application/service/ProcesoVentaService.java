package com.example.backend.sale.application.service;

import com.example.backend.event.infrastructure.persistence.entity.Event;
import com.example.backend.event.infrastructure.persistence.repository.EventRepository;
import com.example.backend.sale.exception.AsientoNoDisponibleException;
import com.example.backend.sale.exception.VentaFallidaException;
import com.example.backend.sale.infrastructure.client.CatedraApiClient;
import com.example.backend.sale.infrastructure.persistence.entity.Sale;
import com.example.backend.sale.infrastructure.persistence.repository.SaleRepository;
import com.example.backend.sale.infrastructure.web.dto.*;
import com.example.backend.proxy.application.service.ProxyClientService;
import com.example.backend.seatSold.infrastructure.persistence.entity.SeatSold;
import com.example.backend.seatSold.infrastructure.persistence.repository.SeatSoldRepository;
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

    public ProcesoVentaService(CatedraApiClient catedraApiClient,
                               ProxyClientService proxyClientService,
                               EventRepository eventRepository,
                               SaleRepository saleRepository,
                               SeatSoldRepository seatSoldRepository,
                               UserRepository userRepository) {
        this.catedraApiClient = catedraApiClient;
        this.proxyClientService = proxyClientService;      // ⬅⬅ NUEVO
        this.eventRepository = eventRepository;
        this.saleRepository = saleRepository;
        this.seatSoldRepository = seatSoldRepository;
        this.userRepository = userRepository;
    }


    public Map<String, Object> bloquearAsientos(BlockRequestDto request) {
        // Validaciones básicas locales
        if (request.getEventoId() == null) {
            throw new IllegalArgumentException("Error: eventoId es nulo");
        }
        if (!eventRepository.existsById(request.getEventoId())) {
            throw new RuntimeException("Evento no encontrado localmente");
        }

        // Llamada al Proxy
        Map<String, Object> respuesta = proxyClientService.bloquearAsientos(request);

        // VALIDACIÓN DEL ISSUE: Si resultado es false, lanzamos excepción
        if (Boolean.FALSE.equals(respuesta.get("resultado"))) {
            String motivo = (String) respuesta.getOrDefault("descripcion", "No se pudieron bloquear los asientos");
            throw new AsientoNoDisponibleException(motivo);
        }

        // Si llegó acá, es true. Retornamos la respuesta exitosa.
        return respuesta;
    }


    @Transactional
    public Map<String, Object> confirmarVenta(SaleRequestDto request, String username) {
        // 1. Obtener Entidades Locales
        Event evento = eventRepository.findById(request.getEventoId())
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        User usuario = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Calcular Precio
        double precioUnitario = (evento.getPrecio() != null) ? evento.getPrecio() : 0.0;
        double precioTotal = precioUnitario * request.getAsientos().size();

        // 3. Preparar Payload para Cátedra
        Map<String, Object> payloadCatedra = new HashMap<>();
        payloadCatedra.put("eventoId", evento.getId());
        payloadCatedra.put("fecha", Instant.now().toString());
        payloadCatedra.put("precioVenta", precioTotal);

        List<Map<String, Object>> listaAsientosCatedra = new ArrayList<>();
        for (int i = 0; i < request.getAsientos().size(); i++) {
            SimpleSeatDto asientoDto = request.getAsientos().get(i);
            PersonDto persona = request.getPersonas().get(i);

            Map<String, Object> asientoMap = new HashMap<>();
            asientoMap.put("fila", asientoDto.getFila());
            asientoMap.put("columna", asientoDto.getColumna());
            asientoMap.put("persona", persona.getNombre() + " " + persona.getApellido());

            listaAsientosCatedra.add(asientoMap);
        }
        payloadCatedra.put("asientos", listaAsientosCatedra);

        // 4. Llamar a la Cátedra
        Map<String, Object> respuestaCatedra = catedraApiClient.realizarVenta(payloadCatedra);

        // VALIDACIÓN DEL ISSUE: Si resultado es false, lanzamos excepción
        if (Boolean.FALSE.equals(respuestaCatedra.get("resultado"))) {
            String motivo = (String) respuestaCatedra.getOrDefault("descripcion", "La venta fue rechazada por la cátedra");
            throw new VentaFallidaException(motivo);
        }

        // 5. Si pasamos la validación (es exitoso), guardamos en BD Local
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

        return respuestaCatedra;
    }

    public List<CatedraSaleDto> obtenerHistorialVentas() {
        // Simplemente delegamos la consulta al cliente de la Cátedra
        return catedraApiClient.listarVentasCatedra();
    }

    public CatedraSaleDetailDto obtenerVentaPorId(Long ventaId) {
        return catedraApiClient.obtenerDetalleVenta(ventaId);
    }
}