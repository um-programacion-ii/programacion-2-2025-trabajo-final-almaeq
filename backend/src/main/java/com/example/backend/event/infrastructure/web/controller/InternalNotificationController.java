package com.example.backend.event.infrastructure.web.controller;

import com.example.backend.event.application.service.SincronizacionService; // <--- Importamos el nuevo servicio
import com.example.backend.event.infrastructure.web.dto.EventNotificationDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/notificacion")
public class InternalNotificationController {

    // CORRECCIÓN: Usamos SincronizacionService en lugar de EventService
    private final SincronizacionService sincronizacionService;

    // Inyectamos el secreto desde application.properties
    @Value("${internal.api.secret}")
    private String internalSecret;

    // Inyección del servicio en el constructor
    public InternalNotificationController(SincronizacionService sincronizacionService) {
        this.sincronizacionService = sincronizacionService;
    }

    @PostMapping("/evento")
    public ResponseEntity<Void> recibirNotificacionEvento(
            @RequestHeader(value = "X-Internal-Secret", required = false) String requestSecret,
            @RequestBody EventNotificationDto notificationDto
    ) {
        // 1. SEGURIDAD: Verificar el Token Interno
        if (requestSecret == null || !requestSecret.equals(internalSecret)) {
            System.err.println("⛔ Intento de acceso no autorizado al endpoint interno.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 2. LOGICA: Delegar al servicio especializado
        System.out.println("🔔 Notificación interna recibida para Evento ID: " + notificationDto.getEventoId());

        // Le pasamos el mensaje/tipo de cambio al servicio para que decida qué hacer
        sincronizacionService.procesarNotificacion(notificationDto.getTipoDeCambio());

        return ResponseEntity.ok().build();
    }
}