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

    @PostMapping("/confirmar")
    public ResponseEntity<Map<String, Object>> confirmarVenta(@RequestBody SaleRequestDto request) {

        // 1. Obtener el usuario real del Token JWT
        // Spring Security ya validó el token en el filtro y guardó al usuario aquí.
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            // 2. Pasar ese username real al servicio
            Map<String, Object> respuesta = procesoVentaService.confirmarVenta(request, username);

            boolean exito = (boolean) respuesta.getOrDefault("resultado", false);

            if (exito) {
                return ResponseEntity.ok(respuesta);
            } else {
                return ResponseEntity.badRequest().body(respuesta);
            }
        } catch (Exception e) {
            // Si el usuario del token no está en la BD, saltará aquí (Usuario no encontrado)
            return ResponseEntity.internalServerError().body(Map.of(
                    "resultado", false,
                    "descripcion", "Error interno: " + e.getMessage()
            ));
        }
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