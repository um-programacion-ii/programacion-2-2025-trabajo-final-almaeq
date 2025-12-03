package com.example.backend.sale.infrastructure.client;

import com.example.backend.sale.infrastructure.web.dto.BlockRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class CatedraApiClient {

    private final RestTemplate restTemplate;

    @Value("${catedra.api.url}")
    private String catedraUrl;

    @Value("${catedra.api.token}")
    private String catedraToken;

    public CatedraApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean bloquearAsientos(BlockRequestDto request) {
        try {
            String url = catedraUrl + "/api/endpoints/v1/bloquear-asientos";

            // Headers con el Token de la Cátedra
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + catedraToken);
            headers.set("Content-Type", "application/json");

            // El cuerpo es el mismo DTO que recibimos del móvil
            HttpEntity<BlockRequestDto> entity = new HttpEntity<>(request, headers);

            // Hacemos el POST
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            // Si devuelve 200 OK, el bloqueo fue exitoso
            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            System.err.println("Error al bloquear asientos en Cátedra: " + e.getMessage());
            return false;
        }
    }
}