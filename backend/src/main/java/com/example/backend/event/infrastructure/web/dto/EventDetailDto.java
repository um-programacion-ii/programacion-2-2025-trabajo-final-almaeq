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
    private String descripcion;
    private LocalDateTime fechaHora;
    private String organizador;
    private List<SeatDto> asientos; // Lista de asientos con su estado
}
