package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.example.project.login.LoginScreen
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.example.project.network.TokenManager

@Composable
@Preview
fun App() {
    MaterialTheme {
        // Estado simple para navegación: si hay token, mostramos contenido, sino Login
        var isLoggedIn by remember { mutableStateOf(TokenManager.jwtToken != null) }

        if (!isLoggedIn) {
            LoginScreen(
                onLoginSuccess = {
                    isLoggedIn = true
                }
            )
        } else {
            // Aquí iría tu pantalla principal (ej. lista de eventos)
            // Por ahora mantenemos el saludo original como placeholder
            MainContent()
        }
    }
}

@Composable
fun MainContent() {
    // Este es el contenido que se ve DESPUÉS de loguearse
    GreetingView()
}

@Composable
fun GreetingView() {
    // Reutilizando lógica existente para mostrar que funciona
    val greeting = remember { Greeting().greet() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Bienvenido! Token guardado en memoria. Plataforma: $greeting")
    }
}