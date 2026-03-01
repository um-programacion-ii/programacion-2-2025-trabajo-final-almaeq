package org.example.project.model

import kotlinx.serialization.Serializable

@Serializable
data class SaleRequest(
    val eventoId: Long,
    val personas: List<Person>,
    val asientos: List<SimpleSeat>
)

@Serializable
data class Person(
    val nombre: String,
    val apellido: String
)