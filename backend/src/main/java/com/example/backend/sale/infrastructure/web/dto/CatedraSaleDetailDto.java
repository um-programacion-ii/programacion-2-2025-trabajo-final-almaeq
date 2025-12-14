package com.example.backend.sale.infrastructure.web.dto;

import lombok.Data;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
public class CatedraSaleDetailDto {
    private Long eventoId;
    private Long ventaId;
    private ZonedDateTime fechaVenta;
    private Boolean resultado;
    private String descripcion;
    private Double precioVenta;

    // Este es el campo exclusivo del Payload 9
    private List<Map<String, Object>> asientos;
}