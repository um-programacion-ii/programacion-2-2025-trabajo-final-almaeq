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
    public Map<String, String> getSeatsStatus(Long eventId) {
        try {
            // Construimos la URL: http://proxy:8081/api/proxy/seats/{eventId}
            String url = UriComponentsBuilder.fromHttpUrl(proxyUrl)
                    .path("/api/proxy/seats/{eventId}")
                    .buildAndExpand(eventId)
                    .toUriString();

            System.out.println("Consultando al Proxy: " + url);

            // Hacemos la petición GET
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            return (Map<String, String>) response.getBody();
        } catch (Exception e) {
            System.err.println("Error comunicándose con el Proxy: " + e.getMessage());
            // En caso de error, devolvemos un mapa vacío para no romper la app
            return Map.of();
        }
    }
}