package com.example.backend.event.infrastructure.web.controller;

import com.example.backend.event.application.service.SincronizacionService;
import com.example.backend.event.infrastructure.web.dto.EventNotificationDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/notificacion")
public class InternalNotificationController {

    private final SincronizacionService sincronizacionService;

    @Value("${internal.api.secret}")
    private String internalSecret;

    public InternalNotificationController(SincronizacionService sincronizacionService) {
        this.sincronizacionService = sincronizacionService;
    }

    @PostMapping("/evento")
    public ResponseEntity<Void> recibirNotificacionEvento(
            @RequestHeader(value = "X-Internal-Secret", required = false) String requestSecret,
            @RequestBody EventNotificationDto notificationDto
    ) {
        // 1. SEGURIDAD
        if (requestSecret == null || !requestSecret.equals(internalSecret)) {
            System.err.println("⛔ Intento de acceso no autorizado al endpoint interno.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 2. LOGICA
        System.out.println("🔔 Notificación interna recibida para Evento ID: " + notificationDto.getEventoId());

        // PASAMOS EL ID Y EL TIPO DE CAMBIO
        sincronizacionService.procesarNotificacion(
                notificationDto.getEventoId(),
                notificationDto.getTipoDeCambio()
        );

        return ResponseEntity.ok().build();
    }
}