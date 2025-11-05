package com.example.backend.user.infrastructure.persistence.entity;

import com.example.backend.sale.infrastructure.persistence.entity.Sale;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // Recuerda guardarla hasheada (ej. BCrypt)

    @Column(unique = true, nullable = false)
    private String email;

    private String firstName;
    private String lastName;

    private boolean enabled = true; // Para Spring Security

    // --- Relaciones ---

    // Una venta le pertenece a un solo usuario,
    // pero un usuario puede tener muchas ventas.
    @OneToMany(mappedBy = "user")
    private Set<Sale> ventas = new HashSet<>();
    
}
