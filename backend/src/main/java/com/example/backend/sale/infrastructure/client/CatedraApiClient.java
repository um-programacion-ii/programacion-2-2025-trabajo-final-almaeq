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

    // CAMBIAR: De boolean a Map<String, Object>
    public Map<String, Object> bloquearAsientos(BlockRequestDto request) {
        try {
            String url = catedraUrl + "/api/endpoints/v1/bloquear-asientos";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + catedraToken);
            headers.set("Content-Type", "application/json");

            HttpEntity<BlockRequestDto> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            // Devolvemos el cuerpo entero de la respuesta (Payload 6 completo)
            return response.getBody();

        } catch (Exception e) {
            System.err.println("Error al bloquear asientos en Cátedra: " + e.getMessage());
            // En caso de error de conexión, devolvemos un mapa indicando fallo
            return Map.of("resultado", false, "descripcion", "Error de comunicación: " + e.getMessage());
        }
    }

    /**
     * Envía la solicitud de venta a la Cátedra.
     * Recibe un Map con la estructura exacta que pide la cátedra (Payload 7).
     */
    public Map<String, Object> realizarVenta(Map<String, Object> requestBody) {
        try {
            String url = catedraUrl + "/api/endpoints/v1/realizar-venta";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + catedraToken);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // POST a la cátedra
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error realizando venta en Cátedra: " + e.getMessage());
            // Retornamos un mapa indicando fallo si hay excepción
            return Map.of("resultado", false, "descripcion", "Error de comunicación con Cátedra");
        }
    }
}