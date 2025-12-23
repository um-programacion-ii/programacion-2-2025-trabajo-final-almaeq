package com.example.backend.sale.infrastructure.web.controller;

import com.example.backend.sale.application.service.ProcesoVentaService;
import com.example.backend.sale.infrastructure.web.dto.BlockRequestDto;
import com.example.backend.sale.infrastructure.web.dto.CatedraSaleDetailDto;
import com.example.backend.sale.infrastructure.web.dto.CatedraSaleDto;
import com.example.backend.sale.infrastructure.web.dto.SaleRequestDto; // Importar
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Importar
import org.springframework.security.core.context.SecurityContextHolder; // Importar
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        // Si falla, el servicio lanza excepción y salta al ControllerAdvice.
        // Si llega aquí, es que fue éxito.
        return ResponseEntity.ok(procesoVentaService.bloquearAsientos(request));
    }

    @PostMapping("/confirmar")
    public ResponseEntity<Map<String, Object>> confirmarVenta(@RequestBody SaleRequestDto request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(procesoVentaService.confirmarVenta(request, username));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<CatedraSaleDto>> getHistorialVentas() {
        return ResponseEntity.ok(procesoVentaService.obtenerHistorialVentas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatedraSaleDetailDto> getVentaPorId(@PathVariable Long id) {
        CatedraSaleDetailDto detalle = procesoVentaService.obtenerVentaPorId(id);

        if (detalle == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detalle);
    }
}