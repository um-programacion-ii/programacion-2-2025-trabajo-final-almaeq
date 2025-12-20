package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.example.project.network.TokenManager
import org.example.project.screens.EventDetailScreen
// Asegúrate de importar tus pantallas correctamente
import org.example.project.screens.EventsListScreen
import org.example.project.screens.LoginScreen
import org.example.project.screens.SeatSelectionScreen

// Definimos los estados posibles de la App
enum class AppScreen {
    LOGIN,
    EVENTS_LIST,
    EVENT_DETAIL, // Preparado para el siguiente issue
    SEAT_SELECTION
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }
        var selectedEventId by remember { mutableStateOf<Long?>(null) }

        if (currentScreen == AppScreen.LOGIN && TokenManager.jwtToken != null) {
            currentScreen = AppScreen.EVENTS_LIST
        }

        when (currentScreen) {
            AppScreen.LOGIN -> {
                LoginScreen(onLoginSuccess = { currentScreen = AppScreen.EVENTS_LIST })
            }
            AppScreen.EVENTS_LIST -> {
                EventsListScreen(
                    onEventClick = { eventId ->
                        selectedEventId = eventId
                        currentScreen = AppScreen.EVENT_DETAIL
                    }
                )
            }
            AppScreen.EVENT_DETAIL -> {
                if (selectedEventId != null) {
                    EventDetailScreen(
                        eventId = selectedEventId!!,
                        onBack = { currentScreen = AppScreen.EVENTS_LIST },
                        onBuyClick = {
                            // Navegamos a la selección de asientos
                            currentScreen = AppScreen.SEAT_SELECTION
                        }
                    )
                }
            }
            AppScreen.SEAT_SELECTION -> {
                if (selectedEventId != null) {
                    SeatSelectionScreen(
                        eventId = selectedEventId!!,
                        onBack = { currentScreen = AppScreen.EVENT_DETAIL }, // Volver al detalle
                        onContinue = { seats ->
                            // Aquí irías a la carga de datos del cliente (Siguiente paso)
                            println("Asientos seleccionados: $seats")
                        }
                    )
                }
            }
        }
    }
}