package org.example.project.model

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: Long,
    val titulo: String,
    val descripcion: String,
    val fechaHora: String,
    // Campos nuevos para el detalle (pueden venir nulos en el listado general)
    val resumen: String? = null,
    val imagenUrl: String? = null,
    val direccion: String? = null,
    val organizador: String? = null,
    val filas: Int = 0,
    val columnas: Int = 0,
    val asientos: List<Seat> = emptyList()
)