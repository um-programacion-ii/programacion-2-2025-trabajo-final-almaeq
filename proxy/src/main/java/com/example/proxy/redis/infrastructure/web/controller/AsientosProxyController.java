package com.example.proxy.redis.infrastructure.web.controller;

import com.example.proxy.redis.application.service.RedisAsientosService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proxy")
public class AsientosProxyController {

    private final RedisAsientosService redisAsientosService;

    public AsientosProxyController(RedisAsientosService redisAsientosService) {
        this.redisAsientosService = redisAsientosService;
    }

    // CORRECCIÓN: Cambiado el path para coincidir con el Backend (ProxyClientService)
    @GetMapping("/seats/{eventoId}")
    public ResponseEntity<?> obtenerAsientos(@PathVariable Long eventoId) {
        // Lógica existente...
        String jsonAsientos = redisAsientosService.obtenerEstadoAsientosEvento(eventoId);

        if (jsonAsientos == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(jsonAsientos);
    }
}