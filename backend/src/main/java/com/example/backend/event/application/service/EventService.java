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
import java.util.List;

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

    // Método para sincronizar (Descargar de cátedra -> Guardar en Local)
    @Transactional
    public void syncEvents() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + catedraToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Endpoint "Payload 4" del PDF
            String url = catedraUrl + "/api/endpoints/v1/eventos";

            ResponseEntity<CatedraEventDto[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, CatedraEventDto[].class);

            if (response.getBody() != null) {
                for (CatedraEventDto dto : response.getBody()) {
                    saveOrUpdateEvent(dto);
                }
            }
        } catch (Exception e) {
            System.err.println("Error sincronizando eventos: " + e.getMessage());
            // No lanzamos error para que la app siga funcionando con lo que tenga local
        }
    }

    private void saveOrUpdateEvent(CatedraEventDto dto) {
        Event event = eventRepository.findById(dto.getId()).orElse(new Event());

        event.setId(dto.getId()); // ID original de cátedra
        event.setTitulo(dto.getTitulo());
        event.setDescripcion(dto.getDescripcion());
        event.setFechaHora(dto.getFecha());
        event.setOrganizador(dto.getNombre()); // Asumiendo que 'nombre' es el organizador
        event.setUltimaActualizacion(LocalDateTime.now());

        eventRepository.save(event);
    }

    // Método para listar (Leer de Local)
    public List<Event> getAllEvents() {
        // Primero intentamos sincronizar para tener datos frescos
        syncEvents();
        // Luego devolvemos lo que hay en la base local
        return eventRepository.findAllByOrderByFechaHoraAsc();
    }
}