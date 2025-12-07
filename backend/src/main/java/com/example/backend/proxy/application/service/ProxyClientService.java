package com.example.backend.proxy.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class ProxyClientService {

    private final RestTemplate restTemplate;


    @Value("${PROXY_SERVICE_URL:http://localhost:8081}")
    private String proxyUrl;

    public ProxyClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Consulta al Proxy el estado de los asientos de un evento.
     * Retorna un Mapa donde: Clave = Asiento (ej: "A1"), Valor = Estado (ej: "Libre", "Ocupado")
     */
    public Map<String, Object> getSeatsStatus(Long eventId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(proxyUrl)
                    .path("/api/proxy/seats/{eventId}") // Asegúrate que esta URL coincida con la del Proxy
                    .buildAndExpand(eventId)
                    .toUriString();

            System.out.println("Consultando al Proxy: " + url);

            // CAMBIO: Map.class devolverá Map<String, Object> automáticamente
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error comunicándose con el Proxy: " + e.getMessage());
            return Map.of();
        }
    }
}