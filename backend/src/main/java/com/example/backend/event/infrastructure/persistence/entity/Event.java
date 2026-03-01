package com.example.backend.event.infrastructure.persistence.entity;

import com.example.backend.sale.infrastructure.persistence.entity.Sale;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "eventos")
public class Event {
    @Id
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 1000) // Aumentamos tamaño por si la descripción es larga
    private String descripcion;

    @Column(length = 500) // Nuevo: Resumen corto
    private String resumen;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    private String organizador; // Guardaremos aquí los integrantes concatenados

    private String direccion;   // Nuevo

    @Column(length = 1000)
    private String imagenUrl;   // Nuevo: URL de la imagen

    private LocalDateTime ultimaActualizacion;

    private Double precio;

    // Capacidad del lugar (opcional, pero viene en el JSON)
    private Integer filas;
    private Integer columnas;

    @OneToMany(mappedBy = "evento")
    private Set<Sale> ventas = new HashSet<>();
}