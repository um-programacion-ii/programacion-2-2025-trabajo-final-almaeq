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

    @Value("${backend.service.url}")
    private String backendUrl;

    @Value("${internal.api.secret}")
    private String internalSecret;

    public BackendNotificacionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void notificarCambio(String mensaje) {
        try {
            String url = backendUrl + "/api/internal/notificacion/evento";

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Secret", internalSecret);

            // Body
            Map<String, Object> body = new HashMap<>();
            body.put("eventoId", 0);
            body.put("tipoDeCambio", mensaje);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForObject(url, request, Void.class);
            System.out.println("✅ (Service) Notificación enviada al Backend.");

        } catch (Exception e) {
            System.err.println("❌ Error notificando al Backend: " + e.getMessage());
        }
    }
}