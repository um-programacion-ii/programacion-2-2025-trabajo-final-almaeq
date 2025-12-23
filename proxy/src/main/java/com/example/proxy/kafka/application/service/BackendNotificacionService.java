package com.example.proxy.kafka.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class BackendNotificacionService {

    private final RestTemplate restTemplate;

    @Value("${backend.url}")
    private String backendUrl;

    public BackendNotificacionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // CAMBIO: El método ahora lanza Exception si falla, NO la captura.
    public void notificarBackend(String mensaje) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("mensaje", mensaje);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        // Si el backend está caído (ConnectionRefused) o da 500,
        // RestTemplate lanzará una excepción automáticamente.
        // Esto disparará el mecanismo de reintento en el KafkaConsumer.
        restTemplate.postForEntity(backendUrl + "/api/internal/notificacion", request, String.class);

        System.out.println("Notificación enviada al backend correctamente: " + mensaje);
    }
}