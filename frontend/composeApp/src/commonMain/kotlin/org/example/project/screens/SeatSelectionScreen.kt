package org.example.project.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.model.Event
import org.example.project.model.Seat
import org.example.project.network.EventRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSelectionScreen(
    eventId: Long,
    onBack: () -> Unit,
    onContinue: (List<Seat>) -> Unit
) {
    val repository = remember { EventRepository() }
    val scope = rememberCoroutineScope()
    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val selectedSeats = remember { mutableStateListOf<Seat>() }

    LaunchedEffect(eventId) {
        scope.launch {
            try {
                event = repository.getEventDetail(eventId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elige tus asientos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(contentPadding = PaddingValues(16.dp)) {
                Button(
                    onClick = { onContinue(selectedSeats.toList()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedSeats.isNotEmpty()
                ) {
                    Text("Confirmar (${selectedSeats.size} asientos)")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (event != null) {
                val currentEvent = event!!

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = currentEvent.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Selecciona hasta 4 asientos.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))

                    // LEYENDA DE COLORES
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem(Color.Green, "Libre")
                        LegendItem(Color.Blue, "Tu Selección")
                        LegendItem(Color(0xFFFFB74D), "Bloqueado")
                        LegendItem(Color(0xFFE57373), "Vendido")
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Spacer(modifier = Modifier.height(8.dp))

                    // GRILLA DE ASIENTOS
                    val seatsList = currentEvent.asientos

                    if (seatsList.isEmpty()) {
                        // Mensaje si no hay datos de asientos
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No hay información de asientos disponible para este evento.")
                        }
                    } else {
                        // 1. Calculamos las columnas reales (evitamos división por 0)
                        val columnasReales = if (currentEvent.columnas > 0) currentEvent.columnas else 1

                        // 2. Ordenamos los asientos para que se pinten en orden
                        val sortedSeats = remember(seatsList) {
                            seatsList.sortedWith(compareBy({ it.fila }, { it.columna }))
                        }

                        // 3. Renderizamos la grilla correcta
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnasReales),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(sortedSeats) { seat ->
                                SeatItem(
                                    seat = seat,
                                    isSelected = selectedSeats.contains(seat),
                                    onSeatClick = { clickedSeat ->
                                        if (selectedSeats.contains(clickedSeat)) {
                                            selectedSeats.remove(clickedSeat)
                                        } else if (selectedSeats.size < 4) {
                                            selectedSeats.add(clickedSeat)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTES DE UI ---

@Composable
fun SeatItem(seat: Seat, isSelected: Boolean, onSeatClick: (Seat) -> Unit) {
    val backgroundColor = when {
        seat.estado == "Vendido" || seat.estado == "Ocupado" -> Color(0xFFE57373) // Rojo
        seat.estado == "Bloqueado" -> Color(0xFFFFB74D) // Naranja
        isSelected -> Color.Blue // Azul
        else -> Color.Green // Verde (Libre)
    }

    val isEnabled = seat.estado != "Vendido" && seat.estado != "Ocupado" && seat.estado != "Bloqueado"

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Cuadrado perfecto
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable(enabled = isEnabled) { onSeatClick(seat) }
            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Mostramos solo el número de columna para que entre bien
        Text(
            text = "${seat.columna}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected || !isEnabled) Color.White else Color.Black
        )
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(14.dp).background(color, RoundedCornerShape(4.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}