package com.example.backend.user.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseStateDto {
    private Integer currentStep;        // Ej: 1 (Ver evento), 2 (Seleccionar asientos)
    private Long eventId;               // El ID del evento que está mirando
    private List<String> selectedSeats; // Lista de asientos, ej: ["F1-A1", "F1-A2"]
}