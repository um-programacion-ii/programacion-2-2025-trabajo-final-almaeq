package com.example.backend.sale.infrastructure.web.controller;

import com.example.backend.sale.application.service.ProcesoVentaService;
import com.example.backend.sale.infrastructure.web.dto.BlockRequestDto;
import com.example.backend.sale.infrastructure.web.dto.SaleRequestDto; // Importar
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Importar
import org.springframework.security.core.context.SecurityContextHolder; // Importar
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/venta")
public class ProcesoVentaController {

    private final ProcesoVentaService procesoVentaService;

    public ProcesoVentaController(ProcesoVentaService procesoVentaService) {
        this.procesoVentaService = procesoVentaService;
    }

    @PostMapping("/bloquear")
    public ResponseEntity<Map<String, Object>> bloquearAsientos(@RequestBody BlockRequestDto request) {
        // Llamamos al servicio que ahora devuelve el Mapa completo
        Map<String, Object> respuestaCatedra = procesoVentaService.bloquearAsientos(request);

        // Verificamos si la operación fue exitosa según el campo "resultado"
        boolean exito = (boolean) respuestaCatedra.getOrDefault("resultado", false);

        if (exito) {
            // Devolvemos Payload 6 completo con status 200 OK
            return ResponseEntity.ok(respuestaCatedra);
        } else {
            // Devolvemos Payload 6 completo con status 400 Bad Request (o el que prefieras)
            // Es importante devolver el cuerpo "respuestaCatedra" para ver POR QUÉ falló (ej: "Ocupado")
            return ResponseEntity.badRequest().body(respuestaCatedra);
        }
    }
}