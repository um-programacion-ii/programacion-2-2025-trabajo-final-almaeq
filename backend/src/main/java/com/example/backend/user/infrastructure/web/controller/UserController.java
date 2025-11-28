package com.example.backend.user.infrastructure.web.controller;

import com.example.backend.security.JwtTokenProvider;
import com.example.backend.user.application.service.UserService;
import com.example.backend.user.infrastructure.web.dto.LoginRequestDto;
import com.example.backend.user.infrastructure.web.dto.LoginResponseDto;
import com.example.backend.user.infrastructure.web.dto.RegisterRequestDto;
import org.springframework.data.redis.core.StringRedisTemplate; // IMPORTANTE
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration; // IMPORTANTE

@RestController
@RequestMapping("/api")
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate; // Inyectamos Redis

    public UserController(AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider,
                          UserService userService,
                          StringRedisTemplate redisTemplate) { // Agregamos al constructor
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/authenticate") // Este es tu endpoint de login
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequestDto loginRequest) {

        // 1. Autenticar (Verifica usuario y contraseña en DB)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. Generar Token JWT
        String jwt = tokenProvider.generateToken(authentication);

        // 3. CREAR SESIÓN EN REDIS (Requisito del Issue #8)
        // Guardamos una clave simple "session:{username}" con valor "active"
        // Expira en 30 minutos (Duration.ofMinutes(30))
        String username = authentication.getName();
        String redisKey = "session:" + username;
        redisTemplate.opsForValue().set(redisKey, "active", Duration.ofMinutes(30));

        // 4. Devolver respuesta
        LoginResponseDto loginResponse = new LoginResponseDto();
        loginResponse.setToken(jwt);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequestDto registerRequest) {
        try {
            userService.registerUser(registerRequest);
            return ResponseEntity.ok("¡Usuario registrado exitosamente!");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}