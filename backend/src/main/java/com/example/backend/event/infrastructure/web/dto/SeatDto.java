package com.example.backend.event.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatDto {
    private String fila;
    private String columna;
    private String estado; // Ej: "Libre", "Ocupado", "Bloqueado"
}
