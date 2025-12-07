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
        // 1. Obtener datos básicos del evento (MySQL local)
        Event event = eventService.getEventById(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Obtener estado de asientos (Redis Cátedra via Proxy)
        Map<String, Object> proxyResponse = proxyClientService.getSeatsStatus(id);

        // 3. Convertir la respuesta del Proxy a tu lista de SeatDto
        List<SeatDto> asientosDto = new ArrayList<>();

        if (proxyResponse != null && proxyResponse.containsKey("asientos")) {
            // El JSON viene como List<Map>, hay que convertirlo
            List<Map<String, Object>> listaAsientos = (List<Map<String, Object>>) proxyResponse.get("asientos");

            for (Map<String, Object> asientoMap : listaAsientos) {
                SeatDto s = new SeatDto();
                // Ojo: los números pueden venir como Integer, convertimos a String seguro
                s.setFila(String.valueOf(asientoMap.get("fila")));
                s.setColumna(String.valueOf(asientoMap.get("columna")));
                s.setEstado((String) asientoMap.get("estado"));
                asientosDto.add(s);
            }
        }

        // 4. Armar el objeto final que verá el celular
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
}