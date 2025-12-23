package com.example.proxy.redis.infrastructure.web.controller;

import com.example.proxy.catedra.infrastructure.client.CatedraProxyClient;
import com.example.proxy.redis.application.service.RedisAsientosService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisConnectionException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/proxy")
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
