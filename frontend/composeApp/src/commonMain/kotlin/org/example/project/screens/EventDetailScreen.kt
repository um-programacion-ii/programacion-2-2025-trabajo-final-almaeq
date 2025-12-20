package org.example.project.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.example.project.model.Event
import org.example.project.network.EventRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Long,
    onBack: () -> Unit,
    onBuyClick: (Long) -> Unit // Esto ahora navegará a la selección de asientos
) {
    val repository = remember { EventRepository() }
    val scope = rememberCoroutineScope()
    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }

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
                title = { Text("Información del Evento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            if (event != null) {
                BottomAppBar(contentPadding = PaddingValues(16.dp)) {
                    Button(
                        onClick = { onBuyClick(eventId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Seleccionar Asientos")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (event == null) {
                Text("No se pudo cargar el evento.", modifier = Modifier.align(Alignment.Center))
            } else {
                val currentEvent = event!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()) // Hacemos scrollable el contenido
                ) {
                    // 1. IMAGEN
                    if (!currentEvent.imagenUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = currentEvent.imagenUrl,
                            contentDescription = "Imagen del evento",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                            // AGREGA ESTO PARA VER ERRORES EN EL LOGCAT:
                            onError = { state ->
                                println("ERROR CARGANDO IMAGEN: ${state.result.throwable.message}")
                                println("URL QUE FALLÓ: ${currentEvent.imagenUrl}")
                            },
                            onLoading = {
                                println("Cargando imagen desde: ${currentEvent.imagenUrl}")
                            }
                        )
                    }

                    // 2. TÍTULO
                    Text(
                        text = currentEvent.titulo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. DATOS (Organizador, Fecha, Dirección)
                    InfoRow(icon = Icons.Default.Person, text = "Organiza: ${currentEvent.organizador ?: "Desconocido"}")
                    InfoRow(icon = Icons.Default.CalendarToday, text = "Fecha: ${currentEvent.fechaHora}")
                    if (!currentEvent.direccion.isNullOrEmpty()) {
                        InfoRow(icon = Icons.Default.LocationOn, text = currentEvent.direccion)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // 4. DESCRIPCIÓN
                    Text(
                        text = "Sobre el evento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentEvent.descripcion,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    // Espacio extra al final para que no tape el botón
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}