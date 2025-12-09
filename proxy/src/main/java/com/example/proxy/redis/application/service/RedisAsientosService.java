package com.example.proxy.redis.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class RedisAsientosService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAsientosService(StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper) {      // ⬅⬅ NUEVO
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String obtenerEstadoAsientosEvento(Long eventoId) {
        String key = "evento_" + eventoId;

        // Recuperamos el JSON tal cual está en Redis (que contiene "eventoId" y "asientos")
        return redisTemplate.opsForValue().get(key);
    }

    // ============================
    // NUEVO: actualizar bloqueos desde la respuesta de Cátedra (Payload 6)
    // ============================
    public void actualizarBloqueosDesdeCatedra(Long eventoId, Map<String, Object> payloadCatedra) {
        String key = "evento_" + eventoId;

        try {
            // Estado actual en Redis
            String jsonActual = redisTemplate.opsForValue().get(key);
            Map<String, Object> estadoActual;

            if (jsonActual != null && !jsonActual.isBlank()) {
                estadoActual = objectMapper.readValue(
                        jsonActual,
                        new TypeReference<Map<String, Object>>() {}
                );
            } else {
                estadoActual = new HashMap<>();
                estadoActual.put("eventoId", eventoId);
                estadoActual.put("asientos", new ArrayList<Map<String, Object>>());
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> asientosActuales =
                    (List<Map<String, Object>>) estadoActual.get("asientos");

            if (asientosActuales == null) {
                asientosActuales = new ArrayList<>();
                estadoActual.put("asientos", asientosActuales);
            }

            // Asientos que vienen en el Payload 6 de Cátedra
            Object asientosNuevosObj = payloadCatedra.get("asientos");
            if (asientosNuevosObj instanceof List<?> listaNuevos) {
                for (Object o : listaNuevos) {
                    if (!(o instanceof Map)) continue;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> asientoCatedra = (Map<String, Object>) o;

                    Number filaNum = (Number) asientoCatedra.get("fila");
                    Number colNum = (Number) asientoCatedra.get("columna");
                    if (filaNum == null || colNum == null) continue;

                    int fila = filaNum.intValue();
                    int columna = colNum.intValue();

                    // Buscar si ya existe en el estado actual
                    Map<String, Object> asientoRedis = null;
                    for (Map<String, Object> a : asientosActuales) {
                        int f = ((Number) a.getOrDefault("fila", -1)).intValue();
                        int c = ((Number) a.getOrDefault("columna", -1)).intValue();
                        if (f == fila && c == columna) {
                            asientoRedis = a;
                            break;
                        }
                    }

                    if (asientoRedis == null) {
                        asientoRedis = new HashMap<>();
                        asientoRedis.put("fila", fila);
                        asientoRedis.put("columna", columna);
                        asientosActuales.add(asientoRedis);
                    }

                    // Estado de Redis: "Bloqueado" (no "Bloqueo exitoso")
                    asientoRedis.put("estado", "Bloqueado");

                    // Expiración a 5 minutos
                    Instant expira = Instant.now().plus(Duration.ofMinutes(5));
                    asientoRedis.put("expira", expira.toString());
                }
            }

            String jsonNuevo = objectMapper.writeValueAsString(estadoActual);
            redisTemplate.opsForValue().set(key, jsonNuevo);

        } catch (Exception e) {
            System.err.println("Error actualizando Redis con bloqueos: " + e.getMessage());
        }
    }
}
