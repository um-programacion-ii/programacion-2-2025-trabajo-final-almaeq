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
import java.util.HashMap;
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
        // 1. Buscar evento en DB local (MySQL) para saber dimensiones
        Event event = eventService.getEventById(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Consultar al Proxy (Redis) para saber cuáles están OCUPADOS
        Map<String, Object> proxyResponse = proxyClientService.getSeatsStatus(id);

        // Creamos un mapa rápido para buscar asientos ocupados: "fila-columna" -> "Estado"
        Map<String, String> ocupadosMap = new HashMap<>();

        if (proxyResponse != null && proxyResponse.containsKey("asientos")) {
            Object asientosObj = proxyResponse.get("asientos");
            if (asientosObj instanceof List) {
                List<Map<String, Object>> listaProxy = (List<Map<String, Object>>) asientosObj;
                for (Map<String, Object> s : listaProxy) {
                    int f = parseIntSafely(s.get("fila"));
                    int c = parseIntSafely(s.get("columna"));
                    String estado = (String) s.get("estado");
                    // Guardamos ej: "1-1" -> "Vendido"
                    ocupadosMap.put(f + "-" + c, estado);
                }
            }
        }

        // 3. GENERAR LA MATRIZ COMPLETA (Libres + Ocupados)
        List<SeatDto> todosLosAsientos = new ArrayList<>();

        // Usamos las filas/columnas del evento. Si son nulos, asumimos 0.
        int totalFilas = event.getFilas() != null ? event.getFilas() : 0;
        int totalCols = event.getColumnas() != null ? event.getColumnas() : 0;

        for (int f = 1; f <= totalFilas; f++) {
            for (int c = 1; c <= totalCols; c++) {
                String key = f + "-" + c;

                // Si está en el mapa de ocupados, usamos ese estado. Si no, es "Libre".
                String estado = ocupadosMap.getOrDefault(key, "Libre");

                todosLosAsientos.add(new SeatDto(f, c, estado));
            }
        }

        // 4. Retornar respuesta con la lista COMPLETA
        EventDetailDto response = new EventDetailDto(
                event.getId(),
                event.getTitulo(),
                event.getResumen(),
                event.getDescripcion(),
                event.getImagenUrl(),
                event.getDireccion(),
                event.getFechaHora(),
                event.getOrganizador(),
                event.getFilas(),
                event.getColumnas(),
                todosLosAsientos     // <--- Aquí va la lista generada
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