package com.example.backend.event.application.service;

import org.springframework.stereotype.Service;

@Service
public class SincronizacionService {

    private final EventService eventService;

    public SincronizacionService(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Recibe la notificación y ejecuta la lógica de actualización.
     * Si viene un ID, actualiza solo ese evento. Si no, actualiza todo.
     */
    public void procesarNotificacion(Long eventoId, String mensaje) {
        System.out.println("🔄 Disparando proceso de sincronización...");
        System.out.println("   Motivo: " + mensaje);

        try {
            if (eventoId != null) {
                // ESTRATEGIA OPTIMIZADA: Solo bajamos el evento que cambió
                System.out.println("   Objetivo: Actualizar Evento ID " + eventoId);
                eventService.syncEventById(eventoId);
            } else {
                // ESTRATEGIA COMPLETA: Bajamos todo (fallback)
                System.out.println("   Objetivo: Sincronización completa");
                eventService.syncEvents();
            }
            System.out.println("✅ Sincronización finalizada.");
        } catch (Exception e) {
            System.err.println("❌ Error en la sincronización: " + e.getMessage());
        }
    }
}