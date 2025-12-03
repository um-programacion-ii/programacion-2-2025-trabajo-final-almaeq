package com.example.backend.sale.application.service;

import com.example.backend.event.infrastructure.persistence.repository.EventRepository; // Importar Repo
import com.example.backend.sale.infrastructure.client.CatedraApiClient;
import com.example.backend.sale.infrastructure.web.dto.BlockRequestDto;
import org.springframework.stereotype.Service;

@Service
public class ProcesoVentaService {

    private final CatedraApiClient catedraApiClient;
    private final EventRepository eventRepository;

    public ProcesoVentaService(CatedraApiClient catedraApiClient, EventRepository eventRepository) {
        this.catedraApiClient = catedraApiClient;
        this.eventRepository = eventRepository;
    }

    public boolean bloquearAsientos(BlockRequestDto request) {
        // 1. VERIFICACIÓN: ¿Existe el evento en nuestra base local?
        // Si no existe, rechazamos la petición inmediatamente.
        if (!eventRepository.existsById(request.getEventoId())) {
            System.err.println("Error: Intento de bloqueo para evento inexistente ID: " + request.getEventoId());
            return false;
        }

        // 2. Si existe, procedemos a llamar a la Cátedra
        return catedraApiClient.bloquearAsientos(request);
    }
}