package com.example.backend.exception;

import com.example.backend.sale.exception.AsientoNoDisponibleException;
import com.example.backend.sale.exception.VentaFallidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({AsientoNoDisponibleException.class, VentaFallidaException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex) {
        // Devuelve 409 Conflict como pide la consigna
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "resultado", false,
                "descripcion", ex.getMessage()
        ));
    }
}