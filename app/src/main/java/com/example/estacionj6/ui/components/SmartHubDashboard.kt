package com.example.estacionj6.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.estacionj6.data.model.HubUiState
import com.example.estacionj6.data.model.Alarm
import java.util.*

@Composable
fun SmartHubDashboard(
    state: HubUiState,
    onToggleRelay: (Boolean) -> Unit,
    onManualWake: () -> Unit,
    onVoiceMicClick: () -> Unit,
    onToggleMusic: () -> Unit,
    onDeleteAlarm: (String) -> Unit,
    onToggleAlarm: (String, Boolean) -> Unit,
    onDismissAlarm: () -> Unit,
    player: Player
) {
    val dimAlpha by animateFloatAsState(
        targetValue = if (state.isDimmed) 0.95f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "dimAlpha"
    )
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onManualWake() })
            }
    ) {
        // Fondo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(colors = listOf(Color(0xFF1A1A1A), Color.Black), radius = 1000f))
        )

        // Banner de error de conexión / Wi-Fi si ocurre
        state.error?.let { errorMsg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .background(Color.Red.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = errorMsg, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- COLUMNA 1 / CARD 1: RELOJ Y FECHA ---
            Column(
                modifier = Modifier.width(215.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.currentTime,
                    fontSize = 90.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-4).sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = state.currentDate,
                    fontSize = 18.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // --- COLUMNA 2 / CARD 2: DATO DE CLIMA IN VS OUT + BATERÍA Y RELÉ ---
            ClimateAndStatusCard(
                state = state,
                onToggleRelay = onToggleRelay
            )

            // --- COLUMNA 3: ALARMAS ---
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Alarmas",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (state.alarms.isEmpty()) {
                    Text(text = "Sin alarmas", color = Color.Gray, fontSize = 12.sp)
                } else {
                    state.alarms.forEach { alarm ->
                        AlarmItem(alarm = alarm, onDelete = onDeleteAlarm, onToggle = onToggleAlarm)
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }

            // --- COLUMNA 4: MÚSICA ---
            Column(
                modifier = Modifier.width(225.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MusicCard(
                    title = if (state.isRadioActive) state.radioName else state.musicTitle,
                    artist = if (state.isRadioActive) "Radio en directo" else state.musicArtist,
                    coverUrl = state.musicCoverUrl,
                    isPlaying = state.isPlaying,
                    isLoading = state.isMusicLoading,
                    isRadio = state.isRadioActive,
                    onToggleMusic = onToggleMusic
                )
            }

            Spacer(modifier = Modifier.width(20.dp))
        }

        // --- TRANSCRIPCIÓN DE VOZ ---
        state.voiceTranscript?.let { text ->
            if (state.isListening || text.isNotEmpty()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
        }

        // --- BOTÓN FLOTANTE DE MICRÓFONO / GRABACIÓN DE VOZ PARA ALARMAS Y COMANDOS ---
        FloatingActionButton(
            onClick = onVoiceMicClick,
            containerColor = if (state.isListening) Color.Red else Color(0xFF42A5F5),
            contentColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp)
        ) {
            Icon(
                imageVector = if (state.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Grabar Voz / Configurar Alarma",
                modifier = Modifier.size(32.dp)
            )
        }

        // --- OVERLAY DE ATENUACIÓN / APAGADO DE PANTALLA ---
        if (dimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = dimAlpha }
                    .background(Color.Black)
            )
        }

        // --- OVERLAY DE ALARMA ACTIVADA ---
        if (state.isAlarmFiring) {
            AlarmFiringOverlay(
                message = state.firingAlarmLabel,
                onDismiss = onDismissAlarm
            )
        }
    }
}

@Composable
fun ClimateAndStatusCard(
    state: HubUiState,
    onToggleRelay: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.width(360.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- ENCABEZADO: LOCALIDAD Y DESCRIPCIÓN CLIMA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFFFCA28),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.weatherLocationName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = state.weatherDescription,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // --- 2 COLUMNAS DE VALORES: IN VS OUT ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // COLUMNA 1: IN (Sensor ESP32 .13)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color(0xFF42A5F5),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "IN (Sensor)",
                            color = Color(0xFF42A5F5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Temp: ${String.format(Locale.getDefault(), "%.1f", state.temperature)} °C",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hum: ${String.format(Locale.getDefault(), "%.1f", state.humidity)} %",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Presión: ${String.format(Locale.getDefault(), "%.0f", state.pressure)} hPa",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // COLUMNA 2: OUT (Servicio Web)
                Column(modifier = Modifier.weight(1.2f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = Color(0xFFFFCA28),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OUT (Web)",
                            color = Color(0xFFFFCA28),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Temp: ${String.format(Locale.getDefault(), "%.1f", state.extTemperature)} °C",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Hum: ${String.format(Locale.getDefault(), "%.1f", state.extHumidity)} %",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Presión: ${String.format(Locale.getDefault(), "%.0f", state.extPressure)} hPa",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Sensación: ${String.format(Locale.getDefault(), "%.1f", state.extApparentTemp)} °C",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Viento: ${String.format(Locale.getDefault(), "%.1f", state.windSpeed)} km/h",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Dirección: ${state.windDirection}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Precip: ${String.format(Locale.getDefault(), "%.1f", state.precipitation)} mm",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Nubes: ${String.format(Locale.getDefault(), "%.0f", state.cloudCover)} %",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // --- ÚLTIMA FILA DE CARD 2: BATERÍA Y CONMUTADOR DE CARGA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Batería J6", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (state.isPhoneCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                            contentDescription = null,
                            tint = if (state.phoneBatteryLevel < 20) Color.Red else Color.Green,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${state.phoneBatteryLevel}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = if (state.isRelayOn) "Cargador Activo" else "Cargador Apagado",
                        color = if (state.isRelayOn) Color.Green else Color.White,
                        fontSize = 10.sp
                    )
                }

                Switch(
                    checked = state.isRelayOn,
                    onCheckedChange = onToggleRelay,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Green,
                        checkedTrackColor = Color.Green.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
fun MusicCard(
    title: String?,
    artist: String?,
    coverUrl: String?,
    isPlaying: Boolean,
    isLoading: Boolean,
    isRadio: Boolean = false,
    onToggleMusic: () -> Unit
) {
    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.DarkGray, RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRadio) Icons.Default.Radio else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title ?: "Sin reproducción",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = artist ?: "Selecciona música o radio",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            IconButton(
                onClick = onToggleMusic,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color.White, RoundedCornerShape(20.dp))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    onDelete: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit
) {
    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                alarm.label?.let {
                    Text(text = it, color = Color.Gray, fontSize = 11.sp)
                }
                IconButton(onClick = { onDelete(alarm.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle(alarm.id, it) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.Cyan)
            )
        }
    }
}

@Composable
fun AlarmFiringOverlay(message: String?, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "alarm")
    val color by infiniteTransition.animateColor(
        initialValue = Color.Red.copy(alpha = 0.6f),
        targetValue = Color.Black.copy(alpha = 0.9f),
        animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
        label = "alarmColor"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
        label = "alarmScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
            )

            if (message != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Red),
                modifier = Modifier.size(200.dp, 80.dp),
                shape = RoundedCornerShape(40.dp)
            ) {
                Text(text = "DETENER", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
