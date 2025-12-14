package com.example.backend.sale.infrastructure.web.dto;

import lombok.Data;
import java.time.ZonedDateTime;

@Data
public class CatedraSaleDto {
    private Long eventoId;
    private Long ventaId;
    private ZonedDateTime fechaVenta; // Usamos ZonedDateTime por la "Z" del JSON
    private Boolean resultado;        // true o false
    private String descripcion;
    private Double precioVenta;
    private Integer cantidadAsientos;
}