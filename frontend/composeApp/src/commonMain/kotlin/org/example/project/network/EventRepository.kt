package org.example.project.network

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.HttpStatusCode
import org.example.project.model.BlockRequest
import org.example.project.model.Event
import org.example.project.model.Seat
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.example.project.AppScreen
import org.example.project.model.Person
import org.example.project.model.PurchaseState
import org.example.project.model.SaleRequest
import org.example.project.model.SimpleSeat

class EventRepository {

    // --- HELPER: Convierte la Pantalla (Enum) a Número (Int) para el Backend ---
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

    suspend fun buySeats(eventId: Long, seats: List<Seat>, names: List<String>): Boolean {
        return try {
            val token = TokenManager.jwtToken ?: return false

            // 1. Transformar la lista de nombres (Strings) a objetos Person
            // Asumimos que el primer espacio separa Nombre de Apellido
            val peopleList = names.map { fullName ->
                val parts = fullName.trim().split(" ", limit = 2)
                val nombre = parts.getOrElse(0) { "" }
                val apellido = parts.getOrElse(1) { "" } // Si no puso apellido, va vacío
                Person(nombre, apellido)
            }

            // 2. Transformar asientos UI a SimpleSeat
            val simpleSeats = seats.map { SimpleSeat(it.fila, it.columna) }

            // 3. Crear el Request Body
            val requestBody = SaleRequest(eventId, peopleList, simpleSeats)

            println("Enviando compra: $requestBody")

            // 4. Llamar al Backend
            // IMPORTANTE: Verifica si tu ApiClient ya tiene "/api/" en la base url.
            // Si "venta/bloquear" te funcionó antes, usa "venta/confirmar".
            // Si tuviste que poner "api/venta/bloquear", usa "api/venta/confirmar" aquí.
            val response = ApiClient.client.post("venta/confirmar") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status == HttpStatusCode.OK) {
                println("¡COMPRA EXITOSA!")
                true
            } else {
                println("ERROR EN COMPRA: ${response.status}")
                false
            }
        } catch (e: Exception) {
            println("EXCEPCIÓN AL COMPRAR: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // 1. OBTENER SESIÓN (GET) - Esto estaba bien, pero retornará Strings en los asientos
    suspend fun getSessionState(): PurchaseState? {
        return try {
            val token = TokenManager.jwtToken ?: return null
            val response = ApiClient.client.get("session/state") {
                header("Authorization", "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            println("No hay sesión activa o error: ${e.message}")
            null
        }
    }

    // 2. GUARDAR SESIÓN (PUT) - CORREGIDO
    suspend fun saveSession(screen: AppScreen, eventId: Long?, seats: List<Seat>) {
        try {
            val token = TokenManager.jwtToken ?: return

            val step = screenToStep(screen)

            // CORRECCIÓN: Convertir objetos Seat a Strings formato "F{fila}-A{columna}"
            // Esto coincide con el comentario del backend: // Ej: ["F1-A1", "F1-A2"]
            val seatStrings = seats.map { "F${it.fila}-A${it.columna}" }

            val state = PurchaseState(step, eventId, seatStrings)

            // CORRECCIÓN URL y MÉTODO: El backend usa @PutMapping("/api/session/state")
            ApiClient.client.put("session/state") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(state)
            }
        } catch (e: Exception) {
            println("Error guardando sesión: ${e.message}")
        }
    }

    // 3. BORRAR SESIÓN (LOGOUT) - CORREGIDO
    suspend fun clearSessionState() {
        try {
            val token = TokenManager.jwtToken ?: return
            // CORRECCIÓN URL y MÉTODO: El backend usa @PostMapping("/api/session/logout")
            ApiClient.client.post("session/logout") {
                header("Authorization", "Bearer $token")
            }
        } catch (e: Exception) {
            println("Error borrando sesión: ${e.message}")
        }
    }
}