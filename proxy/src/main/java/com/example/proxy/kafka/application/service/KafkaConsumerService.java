package com.example.proxy.kafka.application.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private final BackendNotificacionService notificacionService;

    public KafkaConsumerService(BackendNotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @KafkaListener(topics = "eventos-actualizacion", groupId = "${spring.kafka.consumer.group-id}")
    public void consumirMensaje(String mensajeKafka) {
        System.out.println("⚠️ Cambio detectado en Kafka Cátedra: " + mensajeKafka);

        // Delegamos la tarea al servicio específico como pide el enunciado
        notificacionService.notificarCambio(mensajeKafka);
    }
}