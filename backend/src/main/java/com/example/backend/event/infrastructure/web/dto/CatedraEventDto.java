package com.example.backend.event.infrastructure.web.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CatedraEventDto {
    private Long id;
    private String titulo;
    private String descripcion;
    private LocalDateTime fecha; // La cátedra usa "fecha", tu entidad "fechaHora"
    private String nombre;       // A veces aquí viene el organizador
}