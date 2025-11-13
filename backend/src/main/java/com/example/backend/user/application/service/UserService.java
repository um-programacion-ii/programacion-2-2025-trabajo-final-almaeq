package com.example.backend.user.application.service;

import com.example.backend.user.infrastructure.persistence.entity.User;
import com.example.backend.user.infrastructure.persistence.repository.UserRepository;
import com.example.backend.user.infrastructure.web.dto.RegisterRequestDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Inyección de dependencias
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Lógica para registrar un nuevo usuario.
     */
    public User registerUser(RegisterRequestDto registerRequest) {
        // Verificar si el usuario ya existe
        if (userRepository.existsByUsername(registerRequest.getUsername())) { // Usar getter
            throw new RuntimeException("Error: ¡El nombre de usuario ya está en uso!");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) { // Usar getter
            throw new RuntimeException("Error: ¡El email ya está en uso!");
        }

        // Crear el nuevo usuario
        User user = new User();
        // CORRECCIÓN: Estabas usando .equals() en lugar de .setUsername()
        user.setUsername(registerRequest.getUsername()); // Usar setUsername y getter
        user.setEmail(registerRequest.getEmail()); // Usar getter

        // Hashear la contraseña antes de guardarla
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword())); // Usar getter

        user.setFirstName(registerRequest.getFirstName()); // Usar getter
        user.setLastName(registerRequest.getLastName()); // Usar getter
        user.setEnabled(true); // Habilitar el usuario por defecto

        // Guardar el usuario en la base de datos
        return userRepository.save(user);
    }
}
