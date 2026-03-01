package com.example.backend.event.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatDto {
    private Integer fila;
    private Integer columna;
    private String estado;// Ej: "Libre", "Ocupado", "Bloqueado"
}
