package com.example.backend.event.infrastructure.web.controller;

import com.example.backend.event.application.service.EventService;
import com.example.backend.event.infrastructure.persistence.entity.Event;
import com.example.backend.event.infrastructure.web.dto.EventDetailDto;
import com.example.backend.event.infrastructure.web.dto.EventOverviewDto; // <--- IMPORTANTE: Importar el DTO resumido
import com.example.backend.event.infrastructure.web.dto.SeatDto;
import com.example.backend.proxy.application.service.ProxyClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors; // <--- Necesario para convertir la lista

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final ProxyClientService proxyClientService;

    public EventController(EventService eventService, ProxyClientService proxyClientService) {
        this.eventService = eventService;
        this.proxyClientService = proxyClientService;
    }

    @GetMapping
    public ResponseEntity<List<EventOverviewDto>> getAllEvents() {
        // 1. Obtenemos las entidades completas
        List<Event> events = eventService.getAllEvents();

        // 2. Las convertimos al DTO resumido usando Streams
        List<EventOverviewDto> dtos = events.stream()
                .map(event -> new EventOverviewDto(
                        event.getId(),
                        event.getTitulo(),
                        event.getDescripcion(),
                        event.getFechaHora(),
                        event.getOrganizador()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailDto> getEventDetail(@PathVariable Long id) {
        // 1. Buscar evento en DB local
        Event event = eventService.getEventById(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Consultar al Proxy (Asientos)
        Map<String, Object> proxyResponse = proxyClientService.getSeatsStatus(id);

        // 3. Procesar asientos (Mismo código de antes...)
        List<SeatDto> asientosDto = new ArrayList<>();
        if (proxyResponse != null && proxyResponse.containsKey("asientos")) {
            Object asientosObj = proxyResponse.get("asientos");
            if (asientosObj instanceof List) {
                List<Map<String, Object>> listaAsientos = (List<Map<String, Object>>) asientosObj;
                for (Map<String, Object> asientoMap : listaAsientos) {
                    SeatDto s = new SeatDto();
                    s.setFila(parseIntSafely(asientoMap.get("fila")));
                    s.setColumna(parseIntSafely(asientoMap.get("columna")));
                    s.setEstado((String) asientoMap.get("estado"));
                    asientosDto.add(s);
                }
            }
        }

        // 4. Retornar respuesta CON LOS DATOS NUEVOS
        EventDetailDto response = new EventDetailDto(
                event.getId(),
                event.getTitulo(),
                event.getResumen(),       // <--- Nuevo
                event.getDescripcion(),
                event.getImagenUrl(),     // <--- Nuevo
                event.getDireccion(),     // <--- Nuevo
                event.getFechaHora(),
                event.getOrganizador(),
                asientosDto
        );

        return ResponseEntity.ok(response);
    }

    // Método auxiliar de seguridad
    private Integer parseIntSafely(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}