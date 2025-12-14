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
    // Esta función usa tu nuevo ApiClient
    suspend fun login(user: String, pass: String): Boolean {
        return try {
            val response = ApiClient.client.post("/authenticate") {
                setBody(LoginRequest(user, pass))
            }
            val data = response.body<LoginResponse>()

            // Si llegamos aquí, ¡funciona! Guardamos el token
            TokenManager.jwtToken = data.token
            println("Login Exitoso! Token guardado: ${data.token.take(10)}...")
            true
        } catch (e: Exception) {
            println("Error en Login: ${e.message}")
            false
        }
    }
}