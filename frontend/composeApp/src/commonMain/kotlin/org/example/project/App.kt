package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.example.project.network.TokenManager
// Asegúrate de importar tus pantallas correctamente
import org.example.project.screens.EventsListScreen
import org.example.project.screens.LoginScreen

// Definimos los estados posibles de la App
enum class AppScreen {
    LOGIN,
    EVENTS_LIST,
    EVENT_DETAIL // Preparado para el siguiente issue
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        // Estado de navegación
        var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }
        // Estado para guardar el ID del evento seleccionado (para el futuro)
        var selectedEventId by remember { mutableStateOf<Long?>(null) }

        // Si ya hay token en memoria, saltamos directo a la lista
        if (currentScreen == AppScreen.LOGIN && TokenManager.jwtToken != null) {
            currentScreen = AppScreen.EVENTS_LIST
        }

        when (currentScreen) {
            AppScreen.LOGIN -> {
                LoginScreen(
                    onLoginSuccess = {
                        currentScreen = AppScreen.EVENTS_LIST
                    }
                )
            }
            AppScreen.EVENTS_LIST -> {
                EventsListScreen(
                    onEventClick = { eventId ->
                        println("Navegando al detalle del evento: $eventId")
                        selectedEventId = eventId
                        // Aquí cambiarás a DETAIL en el próximo issue
                        // currentScreen = AppScreen.EVENT_DETAIL
                    }
                )
            }
            AppScreen.EVENT_DETAIL -> {
                // Próximamente: EventDetailScreen(selectedEventId)
            }
        }
    }
}