package com.example.backend.event.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.ZonedDateTime; // Usamos ZonedDateTime por la 'Z' del JSON
import java.util.List;

@Data
public class CatedraEventDto {
    private Long id;
    private String titulo;
    private String resumen;     // Nuevo campo
    private String descripcion;

    // El JSON trae "2025-11-10T11:00:00Z", usamos ZonedDateTime para leerlo bien
    private ZonedDateTime fecha;

    private String direccion;   // Nuevo campo
    private String imagen;      // Nuevo campo

    // Mapeamos los nombres exactos del JSON
    private Integer filaAsientos;
    private Integer columnAsientos; // Nota: en el JSON dice "columnAsientos"

    private Double precioEntrada;

    private EventoTipoDto eventoTipo; // Objeto anidado
    private List<IntegranteDto> integrantes; // Lista anidada

    @Data
    public static class EventoTipoDto {
        private String nombre;
        private String descripcion;
    }

    @Data
    public static class IntegranteDto {
        private String nombre;
        private String apellido;
        private String identificacion;
    }
}