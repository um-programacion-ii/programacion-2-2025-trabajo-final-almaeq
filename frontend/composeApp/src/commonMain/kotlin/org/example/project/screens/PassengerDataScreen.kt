package org.example.project.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.model.Seat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerDataScreen(
    selectedSeats: List<Seat>,
    onBack: () -> Unit,
    onConfirmPurchase: (List<String>) -> Unit
) {
    // Inicializamos una lista de strings vacíos del mismo tamaño que la cantidad de asientos
    val passengerNames = remember {
        mutableStateListOf<String>().apply {
            repeat(selectedSeats.size) { add("") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Datos de los Asistentes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                contentPadding = PaddingValues(16.dp)
            ) {
                // El botón se habilita solo si TODOS los campos tienen algo escrito
                val allFilled = passengerNames.all { it.isNotBlank() }

                Button(
                    onClick = { onConfirmPurchase(passengerNames.toList()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = allFilled
                ) {
                    Text("Confirmar Compra")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Ingresa el nombre y apellido para cada entrada:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(selectedSeats) { index, seat ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Asiento: Fila ${seat.fila} - Columna ${seat.columna}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = passengerNames[index],
                                onValueChange = { newText ->
                                    passengerNames[index] = newText
                                },
                                label = { Text("Nombre Completo") },
                                placeholder = { Text("Ej: Juan Perez") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }
    }
}