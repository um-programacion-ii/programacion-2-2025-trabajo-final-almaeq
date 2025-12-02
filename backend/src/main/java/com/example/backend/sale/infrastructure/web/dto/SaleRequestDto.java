package com.example.backend.sale.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaleRequestDto {
    private Long eventoId;
    private List<PersonDto> personas;
    private List<String> asientos; // Ej: ["F1-A1", "F1-A2"]
}
