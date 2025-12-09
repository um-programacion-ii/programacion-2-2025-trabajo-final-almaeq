package com.example.backend.proxy.application.service;

import com.example.backend.sale.infrastructure.web.dto.BlockRequestDto; // ⬅⬅ NUEVO
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
     */
    public Map<String, Object> getSeatsStatus(Long eventId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(proxyUrl)
                    .path("/api/proxy/seats/{eventoId}")
                    .buildAndExpand(eventId)
                    .toUriString();

            System.out.println("Consultando al Proxy: " + url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error comunicándose con el Proxy: " + e.getMessage());
            return Map.of();
        }
    }

    // ============================
    // NUEVO: bloquear asientos vía Proxy
    // ============================
    public Map<String, Object> bloquearAsientos(BlockRequestDto request) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(proxyUrl)
                    .path("/api/proxy/seats/bloquear")
                    .toUriString();

            System.out.println("Bloqueando asientos vía Proxy: " + url);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, request, Map.class);

            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error bloqueando asientos vía Proxy: " + e.getMessage());
            return Map.of(
                    "resultado", false,
                    "descripcion", "Error de comunicación con Proxy"
            );
        }
    }
}
