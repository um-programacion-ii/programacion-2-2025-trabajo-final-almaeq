package com.example.proxy.catedra.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class CatedraProxyClient {

    private final RestTemplate restTemplate;

    @Value("${catedra.api.url}")
    private String catedraUrl;

    @Value("${catedra.api.token}")
    private String catedraToken;

    public CatedraProxyClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> bloquearAsientos(Map<String, Object> payload) {
        try {
            String url = catedraUrl + "/api/endpoints/v1/bloquear-asientos";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + catedraToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error bloqueando asientos en Cátedra desde Proxy: " + e.getMessage());
            return Map.of(
                    "resultado", false,
                    "descripcion", "Error de comunicación con Cátedra desde Proxy"
            );
        }
    }
}
