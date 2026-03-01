package com.example.proxy.kafka.application.service;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private final BackendNotificacionService backendNotificacionService;

    public KafkaConsumerService(BackendNotificacionService backendNotificacionService) {
        this.backendNotificacionService = backendNotificacionService;
    }

    // 1. attempts = "4": Intentará 1 vez normal + 3 reintentos.
    // 2. backoff: Espera 1s, luego 2s, luego 4s (exponencial) entre intentos.
    // 3. Si falla todo, envía el mensaje al tópico "eventos-dlt" (Dead Letter Topic).
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "true" // Crea el tópico de error automático si no existe
    )
    @KafkaListener(topics = "eventos-actualizacion", groupId = "proxy-group-almaeq")
    public void consumirEventos(String mensaje, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        System.out.println("Recibido en Kafka (" + topic + "): " + mensaje);

        // Llamamos al servicio. Si falla (lanza excepción), @RetryableTopic toma el control.

        backendNotificacionService.notificarBackend(mensaje);
    }

    // MÉTODO QUE SE EJECUTA SI FALLAN TODOS LOS REINTENTOS (DLQ Handler)
    @DltHandler
    public void listenDlt(String mensaje, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        System.err.println("!!! ERROR CRÍTICO: Mensaje enviado a DLQ (Dead Letter Queue): " + topic);
        System.err.println("Contenido del mensaje perdido: " + mensaje);

    }
}