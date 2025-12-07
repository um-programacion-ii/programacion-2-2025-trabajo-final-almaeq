package com.example.backend.event.infrastructure.web.controller;

import com.example.backend.event.application.service.EventService;
import com.example.backend.event.infrastructure.web.dto.EventNotificationDto; // Asegurate de importar tu DTO
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/notificacion")
public class InternalNotificationController {

    private final EventService eventService;

    // Inyectamos el secreto desde application.properties
    @Value("${internal.api.secret}")
    private String internalSecret;

    public InternalNotificationController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/evento")
    public ResponseEntity<Void> recibirNotificacionEvento(
            @RequestHeader(value = "X-Internal-Secret", required = false) String requestSecret,
            @RequestBody EventNotificationDto notificationDto // Ahora esperamos el DTO estructurado
    ) {
        // 1. SEGURIDAD: Verificar el Token Interno
        if (requestSecret == null || !requestSecret.equals(internalSecret)) {
            System.err.println("⛔ Intento de acceso no autorizado al endpoint interno.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 2. LOGICA: Usar la información del Body
        System.out.println("🔔 Notificación interna recibida para Evento ID: " + notificationDto.getEventoId());
        System.out.println("   Tipo de cambio: " + notificationDto.getTipoDeCambio());

        try {
            // Aquí podrías optimizar y sincronizar SOLO ese evento si quisieras:
            // eventService.syncEvent(notificationDto.getEventoId());

            // Por ahora mantenemos la sincronización completa que ya funciona:
            eventService.syncEvents();

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error en la sincronización: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}