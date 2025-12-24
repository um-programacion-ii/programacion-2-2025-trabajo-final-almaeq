package com.example.backend.sale.infrastructure.web.controller;

import com.example.backend.sale.application.service.ProcesoVentaService;
import com.example.backend.sale.infrastructure.web.dto.BlockRequestDto;
import com.example.backend.sale.infrastructure.web.dto.CatedraSaleDetailDto;
import com.example.backend.sale.infrastructure.web.dto.CatedraSaleDto;
import com.example.backend.sale.infrastructure.web.dto.SaleRequestDto; // Importar
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder; // Importar
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/venta")
@Tag(name = "Ventas", description = "Endpoints para gestionar el ciclo completo de venta: bloqueo, confirmación e historial.")
public class ProcesoVentaController {

    private final ProcesoVentaService procesoVentaService;

    public ProcesoVentaController(ProcesoVentaService procesoVentaService) {
        this.procesoVentaService = procesoVentaService;
    }

    @Operation(summary = "Bloquear Asientos", description = "Reserva temporalmente los asientos seleccionados comunicándose con el Proxy. Si tiene éxito, los asientos quedan reservados por un tiempo limitado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bloqueo exitoso",
                    content = @Content(schema = @Schema(example = "{\"resultado\": true, \"descripcion\": \"Asientos bloqueados\"}"))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (ej. ID de evento nulo)"),
            @ApiResponse(responseCode = "409", description = "Conflicto: Uno o más asientos ya están ocupados",
                    content = @Content(schema = @Schema(example = "{\"resultado\": false, \"descripcion\": \"Asiento F1-C2 ocupado\"}"))),
            @ApiResponse(responseCode = "503", description = "Error de comunicación con el servicio de Proxy")
    })
    @PostMapping("/bloquear")
    public ResponseEntity<Map<String, Object>> bloquearAsientos(@RequestBody BlockRequestDto request) {
        // Si falla, el servicio lanza excepción y salta al ControllerAdvice.
        return ResponseEntity.ok(procesoVentaService.bloquearAsientos(request));
    }

    @Operation(summary = "Confirmar Venta", description = "Finaliza la compra de entradas. Valida los datos, comunica la venta a la Cátedra y guarda el registro en la base de datos local del alumno.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Venta realizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en los datos de la venta o asientos no bloqueados previamente"),
            @ApiResponse(responseCode = "401", description = "Usuario no autenticado (Token JWT inválido o expirado)"),
            @ApiResponse(responseCode = "500", description = "Error interno al procesar la venta (se activa mecanismo de reintento/backup)")
    })
    @PostMapping("/confirmar")
    public ResponseEntity<Map<String, Object>> confirmarVenta(@RequestBody SaleRequestDto request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(procesoVentaService.confirmarVenta(request, username));
    }

    @Operation(summary = "Historial de Ventas", description = "Obtiene el listado histórico de todas las ventas realizadas consultando directamente a la API de la Cátedra.")
    @ApiResponse(responseCode = "200", description = "Lista de ventas recuperada correctamente")
    @GetMapping("/historial")
    public ResponseEntity<List<CatedraSaleDto>> getHistorialVentas() {
        return ResponseEntity.ok(procesoVentaService.obtenerHistorialVentas());
    }

    @Operation(summary = "Detalle de Venta", description = "Obtiene la información detallada de una venta específica buscándola por su ID en el sistema de la Cátedra.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalle de la venta encontrado"),
            @ApiResponse(responseCode = "404", description = "No se encontró ninguna venta con el ID proporcionado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CatedraSaleDetailDto> getVentaPorId(@PathVariable Long id) {
        CatedraSaleDetailDto detalle = procesoVentaService.obtenerVentaPorId(id);

        if (detalle == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detalle);
    }
}