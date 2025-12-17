package org.example.project.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import org.example.project.model.Event

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
}