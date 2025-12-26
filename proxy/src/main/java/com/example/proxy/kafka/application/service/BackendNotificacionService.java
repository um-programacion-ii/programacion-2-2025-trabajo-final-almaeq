package com.example.proxy.kafka.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class BackendNotificacionService {

    private final RestTemplate restTemplate;

    @Value("${backend.url:http://backend:8080}")
    private String backendUrl;

    @Value("${internal.api.secret}")
    private String internalSecret;

    public BackendNotificacionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void notificarBackend(String mensajeKafka) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Secret", internalSecret);

        // --- CORRECCIÓN CLAVE AQUÍ ---
        // El mensaje de Kafka es solo texto ("Cambios en los datos...").
        // El Backend espera un JSON (EventNotificationDto).
        // Vamos a construir ese JSON manualmente usando un Map.

        Map<String, Object> body = new HashMap<>();
        body.put("eventoId", null); // Null para forzar actualización completa
        body.put("tipoDeCambio", mensajeKafka); // Ponemos el mensaje aquí

        // Enviamos el Map, y RestTemplate lo convertirá a JSON: {"eventoId":null, "tipoDeCambio":"..."}
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String url = backendUrl + "/api/internal/notificacion/evento";

        // Enviamos la petición
        restTemplate.postForEntity(url, request, Void.class);

        System.out.println("✅ Notificación transformada y enviada al backend: " + url);
    }
}