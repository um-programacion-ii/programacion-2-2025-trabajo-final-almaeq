package org.example.project.network

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String)

class AuthRepository {
    suspend fun login(user: String, pass: String): Boolean {
        return try {
            val response = ApiClient.client.post("authenticate") {
                setBody(LoginRequest(user, pass))
            }

            // DIAGNÓSTICO: Imprimir respuesta del servidor
            println("Status Code: ${response.status}")

            if (response.status.value in 200..299) {
                val data = response.body<LoginResponse>()
                TokenManager.jwtToken = data.token
                true
            } else {
                println("Error del servidor: ${response.status}")
                false
            }
        } catch (e: Exception) {
            // MIRA ESTE MENSAJE EN EL LOGCAT
            println("EXCEPCIÓN REAL: ${e}")
            e.printStackTrace()
            false
        }
    }
}