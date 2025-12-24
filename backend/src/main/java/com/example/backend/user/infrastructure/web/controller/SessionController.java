package com.example.backend.user.infrastructure.web.controller;

import com.example.backend.user.infrastructure.web.dto.PurchaseStateDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/session")
@Tag(name = "Sesión de Usuario", description = "Gestión del estado temporal de la compra (Pasos, Asientos seleccionados) almacenado en Redis.")
public class SessionController {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SessionController(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // 1. Obtener el estado actual (GET)
    // El móvil llama a esto al iniciar sesión para saber si había algo pendiente.
    @Operation(summary = "Obtener Estado de Sesión", description = "Recupera el último estado guardado del usuario (paso actual, evento y asientos seleccionados). Se utiliza al iniciar la app para saber si hay una compra pendiente de finalizar.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado recuperado correctamente (puede estar vacío si es la primera vez)",
                    content = @Content(schema = @Schema(implementation = PurchaseStateDto.class))),
            @ApiResponse(responseCode = "500", description = "Error al deserializar los datos de Redis")
    })
    @GetMapping("/state")
    public ResponseEntity<PurchaseStateDto> getSessionState() {
        String username = getCurrentUsername();
        String redisKey = "session:" + username + ":state";

        String jsonState = redisTemplate.opsForValue().get(redisKey);

        if (jsonState == null) {
            // Si no hay estado, devolvemos un objeto vacío
            return ResponseEntity.ok(new PurchaseStateDto(0, null, null));
        }

        try {
            PurchaseStateDto state = objectMapper.readValue(jsonState, PurchaseStateDto.class);
            return ResponseEntity.ok(state);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 2. Guardar estado (PUT)
    // El móvil llama a esto cada vez que el usuario selecciona un asiento o cambia de pantalla.
    @Operation(summary = "Guardar Estado", description = "Guarda el progreso actual del usuario en Redis con una expiración de 30 minutos. Se debe llamar cada vez que el usuario avanza de pantalla o selecciona asientos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado guardado correctamente"),
            @ApiResponse(responseCode = "500", description = "Error al serializar o guardar en Redis")
    })
    @PutMapping("/state")
    public ResponseEntity<?> updateSessionState(@RequestBody PurchaseStateDto newState) {
        String username = getCurrentUsername();
        String redisKey = "session:" + username + ":state";

        try {
            String jsonState = objectMapper.writeValueAsString(newState);
            // Guardamos en Redis por 30 minutos
            redisTemplate.opsForValue().set(redisKey, jsonState, Duration.ofMinutes(30));
            return ResponseEntity.ok("Estado guardado");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al guardar estado");
        }
    }

    // 3. Cerrar Sesión (POST)
    // Borra los datos de Redis como pide el requerimiento.
    @Operation(summary = "Cerrar Sesión (Logout)", description = "Elimina explícitamente los datos de sesión y el estado de compra de Redis, impidiendo recuperar la compra posteriormente.")
    @ApiResponse(responseCode = "200", description = "Sesión cerrada y datos limpiados")
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        String username = getCurrentUsername();

        redisTemplate.delete("session:" + username);          // Borra la sesión simple
        redisTemplate.delete("session:" + username + ":state"); // Borra el estado de compra

        return ResponseEntity.ok("Sesión cerrada correctamente");
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}