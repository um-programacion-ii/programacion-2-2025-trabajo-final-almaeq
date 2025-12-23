package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
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
        var seatsToBuy by remember { mutableStateOf<List<Seat>>(emptyList()) }

        val scope = rememberCoroutineScope()
        val repository = remember { EventRepository() }

        // Si ya tenemos token al iniciar (recarga), intentamos recuperar sesión o ir a lista
        LaunchedEffect(Unit) {
            if (TokenManager.jwtToken != null && currentScreen == AppScreen.LOGIN) {
                // Podrías replicar la lógica de onLoginSuccess aquí si quisieras autologin real
                currentScreen = AppScreen.EVENTS_LIST
            }
        }

        when (currentScreen) {
            AppScreen.LOGIN -> {
                LoginScreen(onLoginSuccess = {
                    // --- LÓGICA DE RECUPERACIÓN DE SESIÓN ---
                    scope.launch {
                        val state = repository.getSessionState()

                        if (state != null && state.eventId != null) {
                            println("Sesión recuperada: Paso ${state.currentStep}, Evento ${state.eventId}")

                            // 1. Restaurar ID del evento
                            selectedEventId = state.eventId

                            // 2. Restaurar Asientos (String -> Seat)
                            val restoredSeats = state.selectedSeats.mapNotNull { seatStr ->
                                try {
                                    // Formato esperado: "F1-A1"
                                    val parts = seatStr.split("-")
                                    val fila = parts[0].substring(1).toInt()
                                    val col = parts[1].substring(1).toInt()
                                    Seat(fila, col, "libre")
                                } catch (e: Exception) {
                                    println("Error parseando asiento: $seatStr")
                                    null
                                }
                            }
                            seatsToBuy = restoredSeats

                            // 3. Navegar
                            when (state.currentStep) {
                                4 -> currentScreen = AppScreen.PASSENGER_DATA // "Cargando nombres"
                                3 -> currentScreen = AppScreen.EVENT_DETAIL   // "Seleccionando asientos" -> Detalle (Requerimiento)
                                else -> currentScreen = AppScreen.EVENTS_LIST
                            }
                        } else {
                            // Sin sesión previa
                            currentScreen = AppScreen.EVENTS_LIST
                        }
                    }
                })
            }

            AppScreen.EVENTS_LIST -> {
                EventsListScreen(
                    onEventClick = { eventId ->
                        selectedEventId = eventId
                        currentScreen = AppScreen.EVENT_DETAIL
                        // Guardar paso 2
                        scope.launch {
                            repository.saveSession(AppScreen.EVENT_DETAIL, eventId, emptyList())
                        }
                    }
                )
            }

            AppScreen.EVENT_DETAIL -> {
                if (selectedEventId != null) {
                    EventDetailScreen(
                        eventId = selectedEventId!!,
                        onBack = {
                            currentScreen = AppScreen.EVENTS_LIST
                            scope.launch { repository.saveSession(AppScreen.EVENTS_LIST, null, emptyList()) }
                        },
                        onBuyClick = {
                            currentScreen = AppScreen.SEAT_SELECTION
                            // Guardar paso 3
                            scope.launch {
                                repository.saveSession(AppScreen.SEAT_SELECTION, selectedEventId, emptyList())
                            }
                        }
                    )
                }
            }

            AppScreen.SEAT_SELECTION -> {
                if (selectedEventId != null) {
                    var isBlocking by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        SeatSelectionScreen(
                            eventId = selectedEventId!!,
                            onBack = {
                                currentScreen = AppScreen.EVENT_DETAIL
                                scope.launch { repository.saveSession(AppScreen.EVENT_DETAIL, selectedEventId, emptyList()) }
                            },
                            onContinue = { selectedSeats ->
                                scope.launch {
                                    isBlocking = true
                                    val success = repository.blockSeats(selectedEventId!!, selectedSeats)
                                    isBlocking = false

                                    if (success) {
                                        seatsToBuy = selectedSeats
                                        currentScreen = AppScreen.PASSENGER_DATA
                                        // Guardar paso 4 (con asientos)
                                        repository.saveSession(AppScreen.PASSENGER_DATA, selectedEventId, selectedSeats)
                                    } else {
                                        println("Error: No se pudieron bloquear los asientos.")
                                    }
                                }
                            }
                        )
                        if (isBlocking) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }

            AppScreen.PASSENGER_DATA -> {
                if (selectedEventId != null && seatsToBuy.isNotEmpty()) {
                    var isBuying by remember { mutableStateOf(false) }
                    var showSuccessDialog by remember { mutableStateOf(false) }
                    var showErrorDialog by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        PassengerDataScreen(
                            selectedSeats = seatsToBuy,
                            onBack = {
                                currentScreen = AppScreen.SEAT_SELECTION
                                // Volver al paso 3
                                scope.launch { repository.saveSession(AppScreen.SEAT_SELECTION, selectedEventId, emptyList()) }
                            },
                            onConfirmPurchase = { names ->
                                scope.launch {
                                    isBuying = true
                                    val success = repository.buySeats(selectedEventId!!, seatsToBuy, names)
                                    isBuying = false

                                    if (success) {
                                        showSuccessDialog = true
                                        // Borrar sesión al comprar
                                        repository.clearSessionState()
                                    } else {
                                        showErrorDialog = true
                                    }
                                }
                            }
                        )

                        if (isBuying) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        if (showSuccessDialog) {
                            AlertDialog(
                                onDismissRequest = {},
                                title = { Text("¡Compra Exitosa!") },
                                text = { Text("Tus entradas han sido reservadas correctamente.") },
                                confirmButton = {
                                    Button(onClick = {
                                        showSuccessDialog = false
                                        seatsToBuy = emptyList()
                                        selectedEventId = null
                                        currentScreen = AppScreen.EVENTS_LIST
                                    }) {
                                        Text("Volver al Inicio")
                                    }
                                }
                            )
                        }

                        if (showErrorDialog) {
                            AlertDialog(
                                onDismissRequest = { showErrorDialog = false },
                                title = { Text("Error") },
                                text = { Text("No se pudo completar la compra.") },
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