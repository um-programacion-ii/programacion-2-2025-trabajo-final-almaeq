package org.example.project.model

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: Long,
    val titulo: String,
    val descripcion: String,
    val fechaHora: String // El DTO del backend lo envía como String
)