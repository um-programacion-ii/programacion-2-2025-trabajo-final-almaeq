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
    onContinue: (List<Seat>) -> Unit // Pasamos los asientos seleccionados al siguiente paso
) {
    val repository = remember { EventRepository() }
    val scope = rememberCoroutineScope()
    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Estado de selección
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
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                    Text(
                        text = currentEvent.titulo,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Máximo 4 asientos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))

                    // LEYENDA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem(Color.Gray, "Ocupado")
                        LegendItem(Color.Green, "Libre")
                        LegendItem(Color.Blue, "Tu Selección")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // GRILLA DE ASIENTOS
                    val maxCol = currentEvent.asientos.maxOfOrNull { it.columna } ?: 1

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(maxCol),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(currentEvent.asientos.sortedWith(compareBy({ it.fila }, { it.columna }))) { seat ->
                            SeatItem(
                                seat = seat,
                                isSelected = selectedSeats.contains(seat),
                                onSeatClick = { clickedSeat ->
                                    if (selectedSeats.contains(clickedSeat)) {
                                        selectedSeats.remove(clickedSeat)
                                    } else {
                                        if (selectedSeats.size < 4) {
                                            selectedSeats.add(clickedSeat)
                                        }
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

// Reutilizamos los componentes pequeños (SeatItem y LegendItem) del paso anterior
@Composable
fun SeatItem(seat: Seat, isSelected: Boolean, onSeatClick: (Seat) -> Unit) {
    val backgroundColor = when {
        seat.estado == "Ocupado" || seat.estado == "Bloqueado" -> Color.Gray
        isSelected -> Color.Blue
        else -> Color.Green
    }
    val isEnabled = seat.estado == "Libre"

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable(enabled = isEnabled) { onSeatClick(seat) }
            .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${seat.fila}-${seat.columna}",
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected || !isEnabled) Color.White else Color.Black
        )
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}