package com.example.backend.event.infrastructure.web.controller;

import com.example.backend.event.application.service.EventService;
import com.example.backend.event.infrastructure.persistence.entity.Event;
import com.example.backend.event.infrastructure.web.dto.EventDetailDto;
import com.example.backend.event.infrastructure.web.dto.EventOverviewDto;
import com.example.backend.event.infrastructure.web.dto.SeatDto;
import com.example.backend.proxy.application.service.ProxyClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        // Intentamos sincronizar la lista general (opcional, pero recomendado)
        try {
            eventService.syncEvents();
        } catch (Exception e) {
            System.out.println("Aviso: No se pudo sincronizar el listado (posiblemente error de token o red).");
        }

        List<Event> events = eventService.getAllEvents();
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
        // 1. IMPORTANTE: Forzar sincronización para traer filas/columnas REALES
        try {
            eventService.syncEventById(id);
        } catch (Exception e) {
            System.err.println("Error al sincronizar evento " + id + ": " + e.getMessage());
        }

        // 2. Buscar evento en DB local (ahora tendrá los datos correctos)
        Event event = eventService.getEventById(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        // 3. Consultar Redis (Estados ocupados)
        Map<String, Object> proxyResponse = proxyClientService.getSeatsStatus(id);

        Map<String, String> ocupadosMap = new HashMap<>();
        if (proxyResponse != null && proxyResponse.containsKey("asientos")) {
            Object asientosObj = proxyResponse.get("asientos");
            if (asientosObj instanceof List) {
                List<Map<String, Object>> listaProxy = (List<Map<String, Object>>) asientosObj;
                for (Map<String, Object> s : listaProxy) {
                    int f = parseIntSafely(s.get("fila"));
                    int c = parseIntSafely(s.get("columna"));
                    String estado = (String) s.get("estado");
                    ocupadosMap.put(f + "-" + c, estado);
                }
            }
        }

        // 4. Generar Matriz Completa
        List<SeatDto> todosLosAsientos = new ArrayList<>();

        // Usar un fallback de 10 si viene 0 para evitar errores visuales,
        // pero con la sync de arriba debería venir bien (ej: 20 y 8).
        int totalFilas = (event.getFilas() != null && event.getFilas() > 0) ? event.getFilas() : 10;
        int totalCols = (event.getColumnas() != null && event.getColumnas() > 0) ? event.getColumnas() : 10;

        for (int f = 1; f <= totalFilas; f++) {
            for (int c = 1; c <= totalCols; c++) {
                String key = f + "-" + c;
                String estado = ocupadosMap.getOrDefault(key, "Libre");
                todosLosAsientos.add(new SeatDto(f, c, estado));
            }
        }

        // 5. Retornar DTO incluyendo filas y columnas
        EventDetailDto response = new EventDetailDto(
                event.getId(),
                event.getTitulo(),
                event.getResumen(),
                event.getDescripcion(),
                event.getImagenUrl(),
                event.getDireccion(),
                event.getFechaHora(),
                event.getOrganizador(),
                totalFilas, // ¡Clave para el celular!
                totalCols,  // ¡Clave para el celular!
                todosLosAsientos
        );

        return ResponseEntity.ok(response);
    }

    private Integer parseIntSafely(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt((String) value); } catch (Exception e) { return 0; }
        }
        return 0;
    }
}