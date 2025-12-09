package com.example.backend.event.infrastructure.web.controller;

import com.example.backend.event.application.service.EventService;
import com.example.backend.event.infrastructure.persistence.entity.Event;
import com.example.backend.event.infrastructure.web.dto.EventDetailDto;
import com.example.backend.event.infrastructure.web.dto.SeatDto;
import com.example.backend.proxy.application.service.ProxyClientService; // Asegúrate de que este import exista
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }


    @GetMapping("/{id}")
    public ResponseEntity<EventDetailDto> getEventDetail(@PathVariable Long id) {
        // 1. Buscar evento en DB local
        Event event = eventService.getEventById(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Consultar al Proxy
        Map<String, Object> proxyResponse = proxyClientService.getSeatsStatus(id);

        // 3. Procesar asientos
        List<SeatDto> asientosDto = new ArrayList<>();

        // Verificamos que la respuesta no sea nula y contenga la lista "asientos"
        if (proxyResponse != null && proxyResponse.containsKey("asientos")) {
            Object asientosObj = proxyResponse.get("asientos");

            if (asientosObj instanceof List) {
                List<Map<String, Object>> listaAsientos = (List<Map<String, Object>>) asientosObj;

                for (Map<String, Object> asientoMap : listaAsientos) {
                    SeatDto s = new SeatDto();

                    // Usamos el método seguro para leer tanto "1" como 1
                    s.setFila(parseIntSafely(asientoMap.get("fila")));
                    s.setColumna(parseIntSafely(asientoMap.get("columna")));
                    s.setEstado((String) asientoMap.get("estado"));

                    asientosDto.add(s);
                }
            }
        }

        // 4. Retornar respuesta
        EventDetailDto response = new EventDetailDto(
                event.getId(),
                event.getTitulo(),
                event.getDescripcion(),
                event.getFechaHora(),
                event.getOrganizador(),
                asientosDto
        );

        return ResponseEntity.ok(response);
    }

    // Método auxiliar de seguridad (cópialo dentro de la clase EventController)
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