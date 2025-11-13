package com.example.backend.user.infrastructure.persistence.repository;

import com.example.backend.user.infrastructure.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Método para encontrar un usuario por su nombre de usuario
    Optional<User> findByUsername(String username);

    // Método para verificar si un usuario existe por su nombre de usuario
    Boolean existsByUsername(String username);

    // Método para verificar si un usuario existe por su email
    Boolean existsByEmail(String email);
}
