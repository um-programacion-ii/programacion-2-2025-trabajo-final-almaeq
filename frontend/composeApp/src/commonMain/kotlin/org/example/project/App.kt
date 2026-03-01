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
import org.example.project.network.Resource
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

        // Mensaje global de error
        var globalErrorMessage by remember { mutableStateOf<String?>(null) }

        val scope = rememberCoroutineScope()
        val repository = remember { EventRepository() }

        // Auto-login check (opcional)
        LaunchedEffect(Unit) {
            if (TokenManager.jwtToken != null && currentScreen == AppScreen.LOGIN) {
                currentScreen = AppScreen.EVENTS_LIST
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                AppScreen.LOGIN -> {
                    LoginScreen(onLoginSuccess = {
                        scope.launch {
                            val state = repository.getSessionState()
                            if (state != null && state.eventId != null) {
                                selectedEventId = state.eventId
                                val restoredSeats = state.selectedSeats.mapNotNull { seatStr ->
                                    try {
                                        val parts = seatStr.split("-")
                                        val fila = parts[0].substring(1).toInt()
                                        val col = parts[1].substring(1).toInt()
                                        Seat(fila, col, "libre")
                                    } catch (e: Exception) { null }
                                }
                                seatsToBuy = restoredSeats
                                when (state.currentStep) {
                                    4 -> currentScreen = AppScreen.PASSENGER_DATA
                                    3 -> currentScreen = AppScreen.EVENT_DETAIL
                                    else -> currentScreen = AppScreen.EVENTS_LIST
                                }
                            } else {
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
                            scope.launch { repository.saveSession(AppScreen.EVENT_DETAIL, eventId, emptyList()) }
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
                                scope.launch { repository.saveSession(AppScreen.SEAT_SELECTION, selectedEventId, emptyList()) }
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
                                        val result = repository.blockSeats(selectedEventId!!, selectedSeats)
                                        isBlocking = false

                                        when (result) {
                                            is Resource.Success -> {
                                                seatsToBuy = selectedSeats
                                                currentScreen = AppScreen.PASSENGER_DATA
                                                repository.saveSession(AppScreen.PASSENGER_DATA, selectedEventId, selectedSeats)
                                            }
                                            is Resource.Error -> {
                                                globalErrorMessage = result.message
                                            }
                                            is Resource.SessionExpired -> {
                                                // REDIRIGIR AL LOGIN
                                                TokenManager.jwtToken = null
                                                currentScreen = AppScreen.LOGIN
                                                globalErrorMessage = "Tu sesión ha expirado. Ingresa nuevamente."
                                            }
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

                        Box(modifier = Modifier.fillMaxSize()) {
                            PassengerDataScreen(
                                selectedSeats = seatsToBuy,
                                onBack = {
                                    currentScreen = AppScreen.SEAT_SELECTION
                                    scope.launch { repository.saveSession(AppScreen.SEAT_SELECTION, selectedEventId, emptyList()) }
                                },
                                onConfirmPurchase = { names ->
                                    scope.launch {
                                        isBuying = true
                                        val result = repository.buySeats(selectedEventId!!, seatsToBuy, names)
                                        isBuying = false

                                        when (result) {
                                            is Resource.Success -> {
                                                showSuccessDialog = true
                                                repository.clearSessionState()
                                            }
                                            is Resource.Error -> {
                                                globalErrorMessage = result.message
                                            }
                                            is Resource.SessionExpired -> {
                                                // REDIRIGIR AL LOGIN
                                                TokenManager.jwtToken = null
                                                currentScreen = AppScreen.LOGIN
                                                globalErrorMessage = "Tu sesión ha expirado. Ingresa nuevamente."
                                            }
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
                        }
                    } else {
                        currentScreen = AppScreen.EVENTS_LIST
                    }
                }
            }

            // DIALOGO GLOBAL DE ERROR
            if (globalErrorMessage != null) {
                AlertDialog(
                    onDismissRequest = { globalErrorMessage = null },
                    title = { Text("Atención") },
                    text = { Text(globalErrorMessage!!) },
                    confirmButton = {
                        Button(onClick = { globalErrorMessage = null }) {
                            Text("Entendido")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    textContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}