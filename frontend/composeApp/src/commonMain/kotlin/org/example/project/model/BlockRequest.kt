package org.example.project.model

import kotlinx.serialization.Serializable

@Serializable
data class BlockRequest(
    val eventoId: Long,
    val asientos: List<SimpleSeat>
)

@Serializable
data class SimpleSeat(
    val fila: Int,
    val columna: Int
)