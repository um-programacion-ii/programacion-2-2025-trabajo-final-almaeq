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
     * Actualmente usamos la estrategia "Trigger" (Sincronización completa).
     */
    public void procesarNotificacion(String mensaje) {
        System.out.println("🔄 Disparando proceso de sincronización...");
        System.out.println("   Motivo: " + mensaje);

        try {
            // Llamamos a la lógica que ya tenías para bajar todo de la cátedra
            eventService.syncEvents();
            System.out.println("✅ Sincronización finalizada.");
        } catch (Exception e) {
            System.err.println("❌ Error en la sincronización: " + e.getMessage());
        }
    }
}