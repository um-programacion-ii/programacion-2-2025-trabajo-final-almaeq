package org.example.project.network

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.errors.IOException
import org.example.project.AppScreen
import org.example.project.model.*

class EventRepository {

    private fun screenToStep(screen: AppScreen): Int {
        return when (screen) {
            AppScreen.EVENTS_LIST -> 1
            AppScreen.EVENT_DETAIL -> 2
            AppScreen.SEAT_SELECTION -> 3
            AppScreen.PASSENGER_DATA -> 4
            else -> 0
        }
    }

    suspend fun getEvents(): List<Event> {
        return try {
            val token = TokenManager.jwtToken ?: return emptyList()
            val response = ApiClient.client.get("events") {
                header("Authorization", "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) response.body() else emptyList()
        } catch (e: Exception) {
            println("Error obteniendo eventos: ${e.message}")
            emptyList()
        }
    }

    suspend fun getEventDetail(id: Long): Event? {
        return try {
            val token = TokenManager.jwtToken ?: return null
            val response = ApiClient.client.get("events/$id") {
                header("Authorization", "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        } catch (e: Exception) {
            println("Error detalle evento: ${e.message}")
            null
        }
    }

    // --- BLOQUEAR ASIENTOS (Con detección de 401) ---
    suspend fun blockSeats(eventId: Long, seats: List<Seat>): Resource<Boolean> {
        return try {
            val token = TokenManager.jwtToken ?: return Resource.SessionExpired // Si no hay token, fuera

            val simpleSeats = seats.map { SimpleSeat(it.fila, it.columna) }
            val requestBody = BlockRequest(eventId, simpleSeats)

            val response = ApiClient.client.post("venta/bloquear") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.OK) {
                Resource.Success(true)
            } else {
                try {
                    val errorMap = response.body<Map<String, Any>>()
                    val msg = errorMap["descripcion"]?.toString() ?: "Error al bloquear"
                    Resource.Error(msg)
                } catch (e: Exception) {
                    Resource.Error("No se pudieron bloquear los asientos")
                }
            }
        } catch (e: ClientRequestException) {
            // DETECCIÓN DE TOKEN VENCIDO
            if (e.response.status == HttpStatusCode.Unauthorized) {
                Resource.SessionExpired
            } else {
                // Otros errores (400, 409, etc.)
                try {
                    val errorMap = e.response.body<Map<String, Any>>()
                    val msg = errorMap["descripcion"]?.toString() ?: "Error en la solicitud"
                    Resource.Error(msg)
                } catch (ex: Exception) {
                    Resource.Error("Error del servidor: ${e.response.status.value}")
                }
            }
        } catch (e: IOException) {
            Resource.Error("No hay conexión. Revisa tu internet")
        } catch (e: Exception) {
            Resource.Error("Error inesperado: ${e.message}")
        }
    }

    // --- COMPRAR ASIENTOS (Con detección de 401) ---
    suspend fun buySeats(eventId: Long, seats: List<Seat>, names: List<String>): Resource<Boolean> {
        return try {
            val token = TokenManager.jwtToken ?: return Resource.SessionExpired

            val peopleList = names.map { fullName ->
                val parts = fullName.trim().split(" ", limit = 2)
                Person(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
            }
            val simpleSeats = seats.map { SimpleSeat(it.fila, it.columna) }
            val requestBody = SaleRequest(eventId, peopleList, simpleSeats)

            val response = ApiClient.client.post("venta/confirmar") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.OK) {
                Resource.Success(true)
            } else {
                try {
                    val errorMap = response.body<Map<String, Any>>()
                    val msg = errorMap["descripcion"]?.toString() ?: "Error en la venta"
                    Resource.Error(msg)
                } catch (e: Exception) {
                    Resource.Error("La venta fue rechazada")
                }
            }
        } catch (e: ClientRequestException) {
            // DETECCIÓN DE TOKEN VENCIDO
            if (e.response.status == HttpStatusCode.Unauthorized) {
                Resource.SessionExpired
            } else {
                try {
                    val errorMap = e.response.body<Map<String, Any>>()
                    val msg = errorMap["descripcion"]?.toString() ?: "Error en la venta"
                    Resource.Error(msg)
                } catch (ex: Exception) {
                    Resource.Error("Error en la venta (Código ${e.response.status.value})")
                }
            }
        } catch (e: IOException) {
            Resource.Error("No hay conexión. Revisa tu internet")
        } catch (e: Exception) {
            Resource.Error("Error inesperado: ${e.message}")
        }
    }

    suspend fun getSessionState(): PurchaseState? {
        return try {
            val token = TokenManager.jwtToken ?: return null
            val response = ApiClient.client.get("session/state") {
                header("Authorization", "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveSession(screen: AppScreen, eventId: Long?, seats: List<Seat>) {
        try {
            val token = TokenManager.jwtToken ?: return
            val step = screenToStep(screen)
            val seatStrings = seats.map { "F${it.fila}-A${it.columna}" }
            val state = PurchaseState(step, eventId, seatStrings)

            ApiClient.client.put("session/state") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(state)
            }
        } catch (e: Exception) {
            println("Error guardando sesión: ${e.message}")
        }
    }

    suspend fun clearSessionState() {
        try {
            val token = TokenManager.jwtToken ?: return
            ApiClient.client.post("session/logout") {
                header("Authorization", "Bearer $token")
            }
        } catch (e: Exception) {
            println("Error limpiando sesión")
        }
    }
}