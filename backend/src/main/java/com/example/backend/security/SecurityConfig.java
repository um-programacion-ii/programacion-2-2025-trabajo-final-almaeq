package com.example.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // AÑADIR LA INYECCIÓN DEL FILTRO
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // AÑADIR EL FILTRO AL CONSTRUCTOR
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    /**
     * Define el bean del PasswordEncoder que usaremos para hashear contraseñas.
     */
    // ESTA ANOTACIÓN ES FUNDAMENTAL
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Define el bean del AuthenticationManager, necesario para el login.
     */
    // ESTA ANOTACIÓN ES FUNDAMENTAL
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configuración principal de Spring Security.
     */
    // ESTA ANOTACIÓN ES LA MÁS IMPORTANTE
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Deshabilitar CSRF (común en APIs REST sin estado)
                .csrf(csrf -> csrf.disable())

                // Configurar la política de sesión como STATELESS (sin estado)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configurar las reglas de autorización
                .authorizeHttpRequests(authz -> authz
                        // Permitir el acceso público a los endpoints de registro y login
                        .requestMatchers("/api/register", "/api/authenticate").permitAll()
                        // Cualquier otra solicitud debe estar autenticada
                        .anyRequest().authenticated()
                );

        // AÑADIR EL FILTRO JWT ANTES DEL FILTRO BÁSICO
        // Esto le dice a Spring Security que use nuestro filtro primero.
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}