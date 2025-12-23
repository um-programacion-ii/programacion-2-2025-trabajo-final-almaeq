package org.example.project.model

import kotlinx.serialization.Serializable

@Serializable
data class PurchaseState(
    val currentStep: Int, // 1: Listado, 2: Detalle, 3: Asientos, 4: Pasajeros
    val eventId: Long? = null,
    // CAMBIO IMPORTANTE: El backend espera Strings tipo "F1-A1", no objetos JSON.
    val selectedSeats: List<String> = emptyList()
)