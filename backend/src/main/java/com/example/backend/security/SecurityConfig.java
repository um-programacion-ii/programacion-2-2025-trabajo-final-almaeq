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
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authz -> authz
                        // Permitir acceso público a endpoints de monitoreo (Healthchecks)
                        .requestMatchers("/actuator/**").permitAll() // <--- ¡AGREGA ESTA LÍNEA!

                        // Permitir login y registro
                        .requestMatchers("/api/register", "/api/authenticate").permitAll()

                        // El resto requiere autenticación
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}