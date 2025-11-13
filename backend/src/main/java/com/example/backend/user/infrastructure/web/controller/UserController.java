package com.example.backend.user.infrastructure.web.controller;

import com.example.backend.user.application.service.UserService;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.user.infrastructure.web.dto.LoginRequestDto;
import com.example.backend.user.infrastructure.web.dto.LoginResponseDto;
import com.example.backend.user.infrastructure.web.dto.RegisterRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    public UserController(AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider,
                          UserService userService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    /**
     * Endpoint para la autenticación de usuarios (login).
     * El cliente móvil llamará a este endpoint.
     */
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequestDto loginRequest) {

        // Autenticar al usuario con Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // Establecer la autenticación en el contexto de seguridad
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generar el token JWT
        String jwt = tokenProvider.generateToken(authentication);

        // 1. Crear el DTO de respuesta vacío
        LoginResponseDto loginResponse = new LoginResponseDto();
        // 2. Usar el setter para asignar el token
        loginResponse.setToken(jwt);

        // Devolver el token en la respuesta
        return ResponseEntity.ok(loginResponse);
    }
    /**
     * Endpoint para el registro de nuevos usuarios.
     * Como dice el enunciado, esto podría ser usado desde la web de JHipster,
     * pero si no usas JHipster, necesitas un endpoint para crear usuarios.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequestDto registerRequest) {
        try {
            userService.registerUser(registerRequest);
            return ResponseEntity.ok("¡Usuario registrado exitosamente!");
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
}