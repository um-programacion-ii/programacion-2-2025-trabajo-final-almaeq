package org.example.project.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.HttpStatusCode
import org.example.project.model.BlockRequest
import org.example.project.model.Event
import org.example.project.model.Seat
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.example.project.model.SimpleSeat

class EventRepository {
    suspend fun getEvents(): List<Event> {
        return try {
            // 1. Verificamos si tenemos token
            val token = TokenManager.jwtToken
            if (token == null) {
                println("ERROR: El token es NULO. El login no guardó el token.")
                return emptyList()
            }

            println("Enviando petición con Token: Bearer ${token.take(10)}...")

            val response = ApiClient.client.get("events") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                // 2. Intentamos leer el cuerpo. Si falla aquí, es formato de JSON/Datos
                val listaEventos = response.body<List<Event>>()
                println("ÉXITO: Se recibieron ${listaEventos.size} eventos.")
                listaEventos
            } else {
                println("ERROR API: Código ${response.status}")
                // Si es 401/403, el token está mal o expiró.
                emptyList()
            }
        } catch (e: Exception) {
            // 3. AQUÍ ESTÁ LA CLAVE. Mira este mensaje en el Logcat.
            println("EXCEPCIÓN CRÍTICA: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getEventDetail(id: Long): Event? {
        return try {
            val token = TokenManager.jwtToken ?: return null

            // Llama a /api/events/{id}
            val response = ApiClient.client.get("events/$id") {
                header("Authorization", "Bearer $token")
            }

            if (response.status == HttpStatusCode.OK) {
                response.body<Event>()
            } else {
                println("Error Detalle: ${response.status}")
                null
            }
        } catch (e: Exception) {
            println("Excepción Detalle: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun blockSeats(eventId: Long, seats: List<Seat>): Boolean {
        return try {
            val token = TokenManager.jwtToken ?: return false

            // Convertimos tus asientos UI (Seat) a los que pide el backend (SimpleSeat)
            val simpleSeats = seats.map { SimpleSeat(it.fila, it.columna) }
            val requestBody = BlockRequest(eventId, simpleSeats)

            val response = ApiClient.client.post("venta/bloquear") { // Asegúrate que la ruta coincida con tu backend
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.OK) {
                println("BLOQUEO EXITOSO")
                true
            } else {
                println("FALLO EL BLOQUEO: ${response.status}")
                false
            }
        } catch (e: Exception) {
            println("ERROR DE RED AL BLOQUEAR: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}