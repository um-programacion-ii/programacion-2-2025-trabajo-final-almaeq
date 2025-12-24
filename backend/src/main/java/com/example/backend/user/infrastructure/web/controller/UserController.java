package com.example.backend.user.infrastructure.web.controller;

import com.example.backend.security.JwtTokenProvider;
import com.example.backend.user.application.service.UserService;
import com.example.backend.user.infrastructure.web.dto.LoginRequestDto;
import com.example.backend.user.infrastructure.web.dto.LoginResponseDto;
import com.example.backend.user.infrastructure.web.dto.RegisterRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Usuarios", description = "Autenticación y Registro")
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

    @Operation(summary = "Iniciar Sesión", description = "Devuelve un JWT si las credenciales son correctas")
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequestDto loginRequest, HttpServletRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generar Token JWT
        String jwt = tokenProvider.generateToken(authentication);

        // CREAR SESIÓN SPRING SESSION
        HttpSession session = request.getSession(true);
        session.setAttribute("usuario", authentication.getName());

        LoginResponseDto loginResponse = new LoginResponseDto();
        loginResponse.setToken(jwt);

        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "Registrar Usuario", description = "Crea un nuevo usuario con rol USER por defecto")
    @ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario ya existe")
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