package com.example.backend.event.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDetailDto {
    private Long id;
    private String titulo;

    // CAMPOS NUEVOS QUE TRAEMOS DEL PAYLOAD 5
    private String resumen;
    private String descripcion;
    private String imagenUrl;
    private String direccion;

    private LocalDateTime fechaHora;
    private String organizador;

    private List<SeatDto> asientos;
}