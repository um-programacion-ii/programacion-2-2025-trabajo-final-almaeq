package com.example.backend.event.infrastructure.web.controller;

import com.example.backend.event.application.service.SincronizacionService;
import com.example.backend.event.infrastructure.web.dto.EventNotificationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/notificacion")
@Tag(name = "Notificaciones Internas", description = "Endpoint privado utilizado exclusivamente por el Proxy para notificar cambios en eventos (vía Kafka).")
public class InternalNotificationController {

    private final SincronizacionService sincronizacionService;

    @Value("${internal.api.secret}")
    private String internalSecret;

    public InternalNotificationController(SincronizacionService sincronizacionService) {
        this.sincronizacionService = sincronizacionService;
    }

    @Operation(summary = "Recibir Notificación de Evento", description = "Recibe un aviso desde el Proxy indicando que un evento ha cambiado (creado, modificado o eliminado) en la Cátedra. Esto dispara la sincronización automática en el Backend.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificación recibida y procesada correctamente"),
            @ApiResponse(responseCode = "403", description = "Prohibido: El secreto proporcionado en el header es incorrecto o falta")
    })
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