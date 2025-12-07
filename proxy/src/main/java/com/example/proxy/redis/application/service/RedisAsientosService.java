package com.example.proxy.redis.application.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisAsientosService {

    private final StringRedisTemplate redisTemplate;

    public RedisAsientosService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String obtenerEstadoAsientosEvento(Long eventoId) {
        // CORRECCIÓN: La cátedra especificó que la clave es "evento_" + ID
        String key = "evento_" + eventoId;

        // Recuperamos el JSON tal cual está en Redis (que contiene "eventoId" y "asientos")
        String json = redisTemplate.opsForValue().get(key);

        return json;
    }
}