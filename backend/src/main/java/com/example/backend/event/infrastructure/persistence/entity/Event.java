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
    // Este ID debe ser el mismo que el del servicio de la Cátedra.
    private Long id;

    @Column(nullable = false)
    private String titulo;

    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    private String organizador;

    private LocalDateTime ultimaActualizacion; // Para saber cuándo lo sincronizaste

    // --- Relaciones ---

    // Una venta es para un solo evento,
    // pero un evento puede tener muchas ventas.
    @OneToMany(mappedBy = "evento")
    private Set<Sale> ventas = new HashSet<>();
}
