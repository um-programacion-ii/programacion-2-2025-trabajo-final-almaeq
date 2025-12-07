package com.example.proxy.kafka.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

@Service
public class KafkaConsumerService {

    private final RestTemplate restTemplate;

    @Value("${backend.service.url}")
    private String backendUrl;

    @Value("${internal.api.secret}")
    private String internalSecret;

    public KafkaConsumerService() {
        this.restTemplate = new RestTemplate();
    }

    @KafkaListener(topics = "eventos-actualizacion", groupId = "${spring.kafka.consumer.group-id}")
    public void consumirMensaje(String mensajeKafka) {
        System.out.println("⚠️ Cambio detectado en Kafka Cátedra: " + mensajeKafka);
        notificarBackend(mensajeKafka);
    }

    private void notificarBackend(String mensajeKafka) {
        try {
            String url = backendUrl + "/api/internal/notificacion/evento";

            // 1. Headers con el SECRETO
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Secret", internalSecret); // <--- Aquí va la protección

            // 2. Body estructurado (JSON)
            // Como no sabemos el formato exacto de Kafka, enviamos un objeto genérico
            // que coincida con EventNotificationDto del Backend.
            Map<String, Object> body = new HashMap<>();
            body.put("eventoId", 0); // Enviamos 0 o tratamos de parsear el ID del mensajeKafka si es posible
            body.put("tipoDeCambio", mensajeKafka); // Mandamos el mensaje original como descripción

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForObject(url, request, Void.class);
            System.out.println("✅ Notificación protegida enviada al Backend.");

        } catch (Exception e) {
            System.err.println("❌ Error enviando notificación al Backend: " + e.getMessage());
        }
    }
}