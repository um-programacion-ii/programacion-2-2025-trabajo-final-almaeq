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

    public BackendNotificacionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void notificarBackend(String mensaje) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("mensaje", mensaje);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        // IMPORTANTE: No usar try-catch aquí.
        // Si el backend falla (500 o timeout), RestTemplate lanza excepción.
        // Esa excepción sube a KafkaConsumerService y activa el @RetryableTopic.
        restTemplate.postForEntity(backendUrl + "/api/internal/notificacion", request, String.class);

        System.out.println("Notificación enviada al backend correctamente: " + mensaje);
    }
}