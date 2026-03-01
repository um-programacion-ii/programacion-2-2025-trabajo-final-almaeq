package com.example.proxy.redis.infrastructure.web.controller;

import com.example.proxy.catedra.infrastructure.client.CatedraProxyClient;
import com.example.proxy.redis.application.service.RedisAsientosService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisConnectionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/proxy")
@Tag(name = "Proxy de Asientos (Redis & Cátedra)", description = "Intermediario para la gestión de asientos. Consulta la caché de Redis para lectura rápida y comunica los bloqueos a la Cátedra.")
public class AsientosProxyController {

    private final RedisAsientosService redisAsientosService;
    private final CatedraProxyClient catedraProxyClient;
    private final ObjectMapper objectMapper;

    public AsientosProxyController(RedisAsientosService redisAsientosService,
                                   CatedraProxyClient catedraProxyClient,
                                   ObjectMapper objectMapper) {
        this.redisAsientosService = redisAsientosService;
        this.catedraProxyClient = catedraProxyClient;
        this.objectMapper = objectMapper;
    }

    // ==========================================
    // GET: obtener asientos desde Redis (para el backend)
    // ==========================================
    @Operation(summary = "Obtener Estado de Asientos (Redis)", description = "Consulta el estado actual de los asientos de un evento directamente desde la caché de Redis mantenida por el Proxy. Esto evita saturar la API de la Cátedra.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mapa de asientos recuperado exitosamente",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"asientos\": [{\"fila\": 1, \"columna\": 1, \"estado\": \"Ocupado\"}, {\"fila\": 1, \"columna\": 2, \"estado\": \"Libre\"}]}"))),
            @ApiResponse(responseCode = "204", description = "No hay información de asientos para este evento en Redis (aún no sincronizado)"),
            @ApiResponse(responseCode = "503", description = "Servicio No Disponible: No se pudo conectar con Redis para leer los datos",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"error\": \"El servicio de asientos (Redis) no está disponible temporalmente.\"}"))),
            @ApiResponse(responseCode = "500", description = "Error interno al procesar los datos JSON")
    })
    @GetMapping("/seats/{eventoId}")
    public ResponseEntity<?> obtenerAsientos(@PathVariable Long eventoId) {
        try {
            // 1. AHORA LA LLAMADA ESTÁ DENTRO DEL TRY
            String jsonAsientos = redisAsientosService.obtenerEstadoAsientosEvento(eventoId);

            if (jsonAsientos == null) {
                return ResponseEntity.noContent().build();
            }

            Map<String, Object> body = objectMapper.readValue(
                    jsonAsientos,
                    new TypeReference<Map<String, Object>>() {}
            );
            return ResponseEntity.ok(body);

        } catch (RedisConnectionFailureException | RedisConnectionException e) {
            // 2. ESTO ES LO QUE PIDE EL ISSUE:
            // Si falla la conexión, devolvemos 503 Service Unavailable
            System.err.println("CRITICAL: Redis Cátedra caído: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "El servicio de asientos (Redis) no está disponible temporalmente."));

        } catch (Exception e) {
            // 3. Error genérico (ej. fallo al parsear JSON) -> 500
            System.err.println("Error interno: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("mensaje", "Error interno en el Proxy"));
        }
    }

    // ==========================================
    // POST: bloquear asientos (backend -> proxy -> cátedra + Redis)
    // ==========================================
    @Operation(summary = "Solicitar Bloqueo de Asientos", description = "Envía una solicitud de bloqueo a la API de la Cátedra. Si la respuesta es exitosa, actualiza inmediatamente la caché de Redis local.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud procesada por la Cátedra (puede ser exitosa o fallida según disponibilidad)",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"resultado\": true, \"descripcion\": \"Asientos bloqueados correctamente\"}"))),
            @ApiResponse(responseCode = "500", description = "Error interno en el Proxy al comunicarse con la Cátedra o actualizar Redis")
    })
    @PostMapping("/seats/bloquear")
    public ResponseEntity<?> bloquearAsientos(@RequestBody Map<String, Object> request) {
        try {
            // 1. Llamar a Cátedra con el Payload 5
            Map<String, Object> respuestaCatedra = catedraProxyClient.bloquearAsientos(request);

            boolean exito = (boolean) respuestaCatedra.getOrDefault("resultado", false);

            Object eventoIdObj = respuestaCatedra.getOrDefault("eventoId", request.get("eventoId"));
            Long eventoId = eventoIdObj instanceof Number
                    ? ((Number) eventoIdObj).longValue()
                    : Long.valueOf(String.valueOf(eventoIdObj));

            // 2. Si salió bien, actualizar Redis
            if (exito) {
                redisAsientosService.actualizarBloqueosDesdeCatedra(eventoId, respuestaCatedra);
            }

            // 3. Devolver al backend el Payload 6 tal cual
            return ResponseEntity.ok(respuestaCatedra);

        } catch (Exception e) {
            System.err.println("Error en Proxy al bloquear asientos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "resultado", false,
                            "descripcion", "Error interno en Proxy"
                    )
            );
        }
    }
}
