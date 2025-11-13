package com.example.backend.security;

import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro que se ejecuta en cada petición para interceptar el token JWT,
 * validarlo y establecer la autenticación en el contexto de seguridad.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    // CORRECCIÓN: Inyectar tu CustomUserDetailsService
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Obtener el token de la cabecera
            String jwt = getJwtFromRequest(request);

            // 2. Validar el token
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {

                // 3. Obtener el username del token
                String username = tokenProvider.getUsernameFromToken(jwt);

                // 4. Cargar los detalles del usuario
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                // 5. Crear el objeto de autenticación
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Establecer la autenticación en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // En caso de error, no se establece la autenticación
            logger.error("No se pudo establecer la autenticación del usuario en el contexto de seguridad", ex);
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }

    /**
     * Método helper para extraer el token del "Authorization Header".
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Devuelve el token sin "Bearer "
        }
        return null;
    }
}