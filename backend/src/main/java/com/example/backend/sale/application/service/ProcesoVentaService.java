package com.example.backend.sale.application.service;

import com.example.backend.event.infrastructure.persistence.entity.Event;
import com.example.backend.event.infrastructure.persistence.repository.EventRepository;
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


    // CAMBIAR: De boolean a Map<String, Object>
    public Map<String, Object> bloquearAsientos(BlockRequestDto request) {
        if (request.getEventoId() == null) {
            return Map.of("resultado", false, "descripcion", "Error: eventoId es nulo");
        }

        if (!eventRepository.existsById(request.getEventoId())) {
            return Map.of("resultado", false, "descripcion", "Evento no encontrado localmente");
        }

        return proxyClientService.bloquearAsientos(request);
    }

    @Transactional
    public Map<String, Object> confirmarVenta(SaleRequestDto request, String username) {
        // 1. Obtener Entidades Locales (Igual que antes)
        Event evento = eventRepository.findById(request.getEventoId())
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        User usuario = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // CALCULAR PRECIO TOTAL
        // Si el precio es null (por datos viejos), usamos 0.0 para evitar error
        double precioUnitario = (evento.getPrecio() != null) ? evento.getPrecio() : 0.0;
        double precioTotal = precioUnitario * request.getAsientos().size();

        // 2. Preparar Payload
        Map<String, Object> payloadCatedra = new HashMap<>();
        payloadCatedra.put("eventoId", evento.getId());
        payloadCatedra.put("fecha", Instant.now().toString());
        payloadCatedra.put("precioVenta", precioTotal);

        List<Map<String, Object>> listaAsientosCatedra = new ArrayList<>();

        for (int i = 0; i < request.getAsientos().size(); i++) {
            // CORRECCIÓN: Usamos el objeto directo, sin parsear strings
            SimpleSeatDto asientoDto = request.getAsientos().get(i);
            PersonDto persona = request.getPersonas().get(i);

            Map<String, Object> asientoMap = new HashMap<>();
            asientoMap.put("fila", asientoDto.getFila());       // Usar el entero real
            asientoMap.put("columna", asientoDto.getColumna()); // Usar el entero real
            asientoMap.put("persona", persona.getNombre() + " " + persona.getApellido());

            listaAsientosCatedra.add(asientoMap);
        }
        payloadCatedra.put("asientos", listaAsientosCatedra);

        // 3. Llamar a la Cátedra (Igual que antes)
        Map<String, Object> respuestaCatedra = catedraApiClient.realizarVenta(payloadCatedra);
        boolean resultado = (boolean) respuestaCatedra.getOrDefault("resultado", false);

        // 4. Si es exitoso, guardar en Base de Datos Local
        if (resultado) {
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
                // Guardamos como texto "F5-C19" o JSON, según prefieras en tu DB
                asientoVendido.setUbicacion("F" + asientoDto.getFila() + "-C" + asientoDto.getColumna());
                asientoVendido.setNombrePersona(persona.getNombre());
                asientoVendido.setApellidoPersona(persona.getApellido());

                seatSoldRepository.save(asientoVendido);
            }
        }

        return respuestaCatedra;
    }

    public List<CatedraSaleDto> obtenerHistorialVentas() {
        // Simplemente delegamos la consulta al cliente de la Cátedra
        return catedraApiClient.listarVentasCatedra();
    }
}