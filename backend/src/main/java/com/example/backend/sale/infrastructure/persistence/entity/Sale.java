package com.example.backend.sale.infrastructure.persistence.entity;

import com.example.backend.event.infrastructure.persistence.entity.Event;
import com.example.backend.seatSold.infrastructure.persistence.entity.SeatSold;
import com.example.backend.user.infrastructure.persistence.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ventas")
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaVenta;

    @Column(nullable = false)
    private String estado;

    // --- Relaciones ---

    // Muchas ventas pueden pertenecer a un mismo usuario.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Muchas ventas pueden ser del mismo evento.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Event evento;

    // Una venta se compone de uno o más asientos (hasta 4).
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SeatSold> asientos = new HashSet<>();
}
