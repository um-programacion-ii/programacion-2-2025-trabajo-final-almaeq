package com.example.backend.seatSold.infrastructure.persistence.entity;

import com.example.backend.sale.infrastructure.persistence.entity.Sale;
import jakarta.persistence.*;

@Entity
@Table(name = "asientos_vendidos")
public class SeatSold {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El identificador del asiento (ej: "F1-A12") que te da el proxy/cátedra
    @Column(nullable = false)
    private String ubicacion;

    @Column(nullable = false)
    private String nombrePersona;

    @Column(nullable = false)
    private String apellidoPersona;

    // --- Relaciones ---

    // Muchos asientos pertenecen a una sola venta.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    private Sale venta;
}

