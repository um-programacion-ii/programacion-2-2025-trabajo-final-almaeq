package com.example.backend.sale.infrastructure.web.controller;

import com.example.backend.sale.application.service.ProcesoVentaService;
import com.example.backend.sale.infrastructure.web.dto.BlockRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/venta")
public class ProcesoVentaController {

    private final ProcesoVentaService procesoVentaService;

    public ProcesoVentaController(ProcesoVentaService procesoVentaService) {
        this.procesoVentaService = procesoVentaService;
    }

    @PostMapping("/bloquear")
    public ResponseEntity<?> bloquearAsientos(@RequestBody BlockRequestDto request) {
        boolean exito = procesoVentaService.bloquearAsientos(request);

        if (exito) {
            return ResponseEntity.ok(Map.of("mensaje", "Asientos bloqueados correctamente"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudieron bloquear los asientos. Es posible que ya estén ocupados."));
        }
    }
}