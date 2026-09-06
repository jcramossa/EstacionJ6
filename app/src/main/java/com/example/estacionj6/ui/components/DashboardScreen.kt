package com.example.estacionj6.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.estacionj6.data.model.HubUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: HubUiState,
    onToggleRelay: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Hub J6", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Indicador de error si ocurre
            state.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }


            // Tarjeta de Presencia LD2410
            PresenceCard(isGlobalPresence = state.isPresenceDetected, isMoving = state.isMovingPresence, isStatic = state.isStaticPresence)

            // Tarjetas Ambientales (Temperatura, Humedad, Presión)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Temperatura",
                    value = String.format("%.1f °C", state.temperature),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Humedad",
                    value = String.format("%.1f %%", state.humidity),
                    modifier = Modifier.weight(1f)
                )
            }

            MetricCard(
                title = "Presión Atmosférica",
                value = String.format("%.1f hPa", state.pressure),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Control de Batería J6 y Relé
            BatteryControlCard(
                batteryLevel = state.phoneBatteryLevel,
                isCharging = state.isPhoneCharging,
                isRelayOn = state.isRelayOn,
                onToggleRelay = onToggleRelay
            )
        }
    }
}

@Composable
fun PresenceCard(
    isGlobalPresence: Boolean,
    isMoving: Boolean,
    isStatic: Boolean
) {
    val bgColor = if (isGlobalPresence) Color(0xFF2E7D32) else Color(0xFF424242)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isGlobalPresence) "PRESENCIA DETECTADA" else "HABITACIÓN VACÍA",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )

            if (isGlobalPresence) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PresenceChip(label = "Movimiento", isActive = isMoving)
                    PresenceChip(label = "Estático", isActive = isStatic)
                }
            }
        }
    }
}

@Composable
fun PresenceChip(label: String, isActive: Boolean) {
    Surface(
        color = if (isActive) Color(0xFF81C784) else Color(0xFF616161),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = "$label: ${if (isActive) "ON" else "OFF"}",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BatteryControlCard(
    batteryLevel: Int,
    isCharging: Boolean,
    isRelayOn: Boolean,
    onToggleRelay: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gestión de Batería (Smart Hub)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Nivel J6: $batteryLevel%")
                    Text(
                        text = if (isCharging) "Cargando..." else "En batería",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (isRelayOn) "Cargador ON" else "Cargador OFF")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isRelayOn,
                        onCheckedChange = onToggleRelay
                    )
                }
            }
        }
    }
}