package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.example.project.model.Seat
import org.example.project.network.EventRepository
import org.example.project.network.TokenManager
import org.example.project.screens.*

enum class AppScreen {
    LOGIN,
    EVENTS_LIST,
    EVENT_DETAIL,
    SEAT_SELECTION,
    PASSENGER_DATA
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }
        var selectedEventId by remember { mutableStateOf<Long?>(null) }

        // Variable para guardar los asientos elegidos temporalmente
        var seatsToBuy by remember { mutableStateOf<List<Seat>>(emptyList()) }

        // Si ya tenemos token, saltamos el login (útil al recargar)
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
                            currentScreen = AppScreen.SEAT_SELECTION
                        }
                    )
                }
            }

            AppScreen.SEAT_SELECTION -> {
                if (selectedEventId != null) {
                    // Instanciamos lo necesario para bloquear
                    val repository = remember { EventRepository() }
                    val scope = rememberCoroutineScope()
                    var isBlocking by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        SeatSelectionScreen(
                            eventId = selectedEventId!!,
                            onBack = { currentScreen = AppScreen.EVENT_DETAIL },
                            onContinue = { selectedSeats ->
                                // --- LÓGICA DE BLOQUEO (Opción B) ---
                                scope.launch {
                                    isBlocking = true
                                    // 1. Intentamos bloquear en el servidor
                                    val success =
                                        repository.blockSeats(selectedEventId!!, selectedSeats)
                                    isBlocking = false

                                    if (success) {
                                        // 2. Si funcionó, guardamos y avanzamos
                                        seatsToBuy = selectedSeats
                                        currentScreen = AppScreen.PASSENGER_DATA
                                    } else {
                                        // 3. Si falló, imprimimos error (aquí podrías poner un Toast)
                                        println("Error: No se pudieron bloquear los asientos (quizás ya están ocupados).")
                                    }
                                }
                            }
                        )

                        // Indicador de carga mientras se bloquea
                        if (isBlocking) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }

            AppScreen.PASSENGER_DATA -> {
                if (selectedEventId != null && seatsToBuy.isNotEmpty()) {
                    // Estados para la carga y alertas
                    val repository = remember { EventRepository() }
                    val scope = rememberCoroutineScope()
                    var isBuying by remember { mutableStateOf(false) }
                    var showSuccessDialog by remember { mutableStateOf(false) }
                    var showErrorDialog by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        PassengerDataScreen(
                            selectedSeats = seatsToBuy,
                            onBack = { currentScreen = AppScreen.SEAT_SELECTION },
                            onConfirmPurchase = { names ->
                                scope.launch {
                                    isBuying = true
                                    // LLAMADA AL REPOSITORIO
                                    val success =
                                        repository.buySeats(selectedEventId!!, seatsToBuy, names)
                                    isBuying = false

                                    if (success) {
                                        showSuccessDialog = true
                                    } else {
                                        showErrorDialog = true
                                    }
                                }
                            }
                        )

                        // Loading Indicator
                        if (isBuying) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        // Diálogo de Éxito
                        if (showSuccessDialog) {
                            AlertDialog(
                                onDismissRequest = {}, // No permitir cerrar tocando afuera
                                title = { Text("¡Compra Exitosa!") },
                                text = { Text("Tus entradas han sido reservadas correctamente.") },
                                confirmButton = {
                                    Button(onClick = {
                                        showSuccessDialog = false
                                        // Volvemos al listado inicial limpiando todo
                                        currentScreen = AppScreen.EVENTS_LIST
                                    }) {
                                        Text("Volver al Inicio")
                                    }
                                }
                            )
                        }

                        // Diálogo de Error
                        if (showErrorDialog) {
                            AlertDialog(
                                onDismissRequest = { showErrorDialog = false },
                                title = { Text("Error") },
                                text = { Text("No se pudo completar la compra. Es posible que los asientos ya no estén disponibles o el tiempo haya expirado.") },
                                confirmButton = {
                                    Button(onClick = { showErrorDialog = false }) {
                                        Text("Entendido")
                                    }
                                }
                            )
                        }
                    }
                } else {
                    currentScreen = AppScreen.EVENTS_LIST
                }
            }
        }

    }
}