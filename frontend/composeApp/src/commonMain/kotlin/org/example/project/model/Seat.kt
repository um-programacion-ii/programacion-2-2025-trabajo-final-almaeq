package org.example.project.model

import kotlinx.serialization.Serializable

@Serializable
data class Seat(
    val fila: Int,
    val columna: Int,
    val estado: String // "Libre", "Ocupado", "Bloqueado"
)