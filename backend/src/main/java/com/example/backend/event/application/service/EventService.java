 package com.example.backend.event.application.service;

import com.example.backend.event.infrastructure.persistence.entity.Event;
import com.example.backend.event.infrastructure.persistence.repository.EventRepository;
import com.example.backend.event.infrastructure.web.dto.CatedraEventDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final RestTemplate restTemplate;

    @Value("${catedra.api.url}")
    private String catedraUrl;

    @Value("${catedra.api.token}")
    private String catedraToken;

    public EventService(EventRepository eventRepository, RestTemplate restTemplate) {
        this.eventRepository = eventRepository;
        this.restTemplate = restTemplate;
    }

    // --- NUEVO MÉTODO: Sincronizar UN solo evento (Payload 5) ---
    @Transactional
    public void syncEventById(Long id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + catedraToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Endpoint "Payload 5" - Singular: /evento/{id}
            String url = catedraUrl + "/api/endpoints/v1/evento/" + id;

            // Esperamos un solo objeto CatedraEventDto
            ResponseEntity<CatedraEventDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, CatedraEventDto.class);

            if (response.getBody() != null) {
                saveOrUpdateEvent(response.getBody());
                System.out.println("✅ Evento " + id + " sincronizado correctamente.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error sincronizando evento " + id + ": " + e.getMessage());
        }
    }

    // Método para sincronizar TODOS (Payload 4)
    @Transactional
    public void syncEvents() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + catedraToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Endpoint "Payload 4" - Plural: /eventos
            String url = catedraUrl + "/api/endpoints/v1/eventos";

            ResponseEntity<CatedraEventDto[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, CatedraEventDto[].class);

            if (response.getBody() != null) {
                for (CatedraEventDto dto : response.getBody()) {
                    saveOrUpdateEvent(dto);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error sincronizando lista de eventos: " + e.getMessage());
        }
    }

    private void saveOrUpdateEvent(CatedraEventDto dto) {
        Event event = eventRepository.findById(dto.getId()).orElse(new Event());

        event.setId(dto.getId());
        event.setTitulo(dto.getTitulo());
        event.setDescripcion(dto.getDescripcion());
        event.setResumen(dto.getResumen());
        event.setDireccion(dto.getDireccion());
        event.setImagenUrl(dto.getImagen());
        event.setPrecio(dto.getPrecioEntrada());

        event.setFilas(dto.getFilaAsientos());
        event.setColumnas(dto.getColumnAsientos());

        if (dto.getFecha() != null) {
            event.setFechaHora(dto.getFecha()
                    .withZoneSameInstant(ZoneId.of("America/Argentina/Mendoza"))
                    .toLocalDateTime());
        }

        if (dto.getIntegrantes() != null && !dto.getIntegrantes().isEmpty()) {
            String organizadores = dto.getIntegrantes().stream()
                    .map(i -> i.getNombre() + " " + i.getApellido())
                    .collect(Collectors.joining(", "));
            event.setOrganizador(organizadores);
        } else {
            event.setOrganizador(dto.getEventoTipo() != null ? dto.getEventoTipo().getNombre() : "Evento Cátedra");
        }

        event.setUltimaActualizacion(LocalDateTime.now());
        eventRepository.save(event);
    }

    public List<Event> getAllEvents() {
        syncEvents();
        return eventRepository.findAllByOrderByFechaHoraAsc();
    }

    public Event getEventById(Long id) {
        return eventRepository.findById(id).orElse(null);
    }
}