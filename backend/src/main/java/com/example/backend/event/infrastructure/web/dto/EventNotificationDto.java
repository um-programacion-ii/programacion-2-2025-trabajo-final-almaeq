package com.example.backend.event.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventNotificationDto {
    private Long eventoId;
    private String tipoDeCambio; // Ej: "CAMBIO_HORARIO", "CANCELADO"
}
