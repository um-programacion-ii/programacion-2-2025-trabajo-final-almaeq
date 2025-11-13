package com.example.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Inyecta la clave secreta desde application.properties
    @Value("${app.jwt-secret}")
    private String jwtSecret;

    // Inyecta el tiempo de expiración desde application.properties
    @Value("${app.jwt-expiration-ms}")
    private long jwtExpirationMs;

    // Genera un token JWT para un usuario
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        // Clave secreta (debe ser lo suficientemente larga y segura)
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // NOTA: Aquí faltarían los métodos para *validar* el token y obtener el usuario desde el token, que se usarían en el filtro JWT.
}