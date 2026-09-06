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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.estacionj6.data.model.HubUiState
import com.example.estacionj6.data.model.Alarm
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
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
    val dimAlpha by animateFloatAsState(targetValue = if (state.isDimmed) 0.92f else 0f, label = "dimAlpha")
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

        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- COLUMNA 1: RELOJ + AMBIENTE ---
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

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MiniClimateCard(title = "Int.", temp = state.temperature, hum = state.humidity, icon = Icons.Default.Home, accentColor = Color(0xFF42A5F5), modifier = Modifier.weight(1f))
                    MiniClimateCard(title = "Ext.", temp = state.extTemperature, hum = state.extHumidity, icon = Icons.Default.WbSunny, accentColor = Color(0xFFFFCA28), modifier = Modifier.weight(1f))
                    MiniPressureCard(pressure = state.pressure, modifier = Modifier.weight(1f))
                }
            }

            // --- COLUMNA 2: ESTADO Y CONTROL ---
            Column(
                modifier = Modifier.width(215.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PresenceMiniCard(isDetected = state.isPresenceDetected)
                SystemStatusCard(level = state.phoneBatteryLevel, isCharging = state.isPhoneCharging, isRelayOn = state.isRelayOn, onToggleRelay = onToggleRelay)
            }

            // --- COLUMNA 3: ALARMAS ---
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()) // Scroll vertical añadido
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
                // Espacio extra al final para que la última alarma no quede tapada por el botón
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
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp, start = 32.dp, end = 32.dp)
                ) {
                    Text(text = text, color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), textAlign = TextAlign.Center)
                }
            }
        }

        // --- BOTÓN DE VOZ ---
        FloatingActionButton(
            onClick = onVoiceMicClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).size(if (state.isListening || state.isThinking) 72.dp else 56.dp),
            containerColor = when {
                state.isThinking -> Color(0xFFFFA000)
                state.isListening -> Color.Red
                else -> Color(0xFF6200EE)
            },
            contentColor = Color.White
        ) {
            Icon(imageVector = when {
                state.isThinking -> Icons.Default.AutoAwesome
                state.isListening -> Icons.Default.GraphicEq
                else -> Icons.Default.Mic
            }, contentDescription = "Voz")
        }

        // --- CAPA DE OSCURECIMIENTO ---
        if (dimAlpha > 0f && !state.isAlarmFiring) {
            Box(modifier = Modifier.fillMaxSize().alpha(dimAlpha).background(Color.Black))
        }

        // --- OVERLAY DE ALARMA ---
        if (state.isAlarmFiring) {
            AlarmFiringOverlay(
                message = state.firingAlarmLabel,
                onDismiss = onDismissAlarm
            )
        }

        // --- REPRODUCTOR DE YOUTUBE (PANTALLA COMPLETA) ---
        if (state.isYoutubeActive && state.youtubeVideoId != null) {
            YouTubeOverlay(
                videoId = state.youtubeVideoId,
                onClose = onToggleMusic // Reutilizamos para parar
            )
        }

        // --- REPRODUCTOR DE PLEX (PANTALLA COMPLETA) ---
        if (state.isPlexActive && state.plexMediaUrl != null) {
            PlexOverlay(
                title = state.plexTitle,
                url = state.plexMediaUrl,
                player = player,
                onClose = onToggleMusic
            )
        }
    }
}

@Composable
fun PlexOverlay(title: String?, url: String, player: Player, onClose: () -> Unit) {
    LaunchedEffect(url) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
        }
    }
}

@Composable
fun YouTubeOverlay(videoId: String, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                YouTubePlayerView(context).apply {
                    enableAutomaticInitialization = false
                    initialize(object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.loadVideo(videoId, 0f)
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
        }
    }
}

@Composable
fun AlarmItem(alarm: Alarm, onDelete: (String) -> Unit, onToggle: (String, Boolean) -> Unit) {
    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (alarm.isReminder) Icons.Default.NotificationsActive else Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (alarm.isReminder) Color.Yellow else Color.Red,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                
                // Mostrar mensaje/label si existe
                alarm.label?.let {
                    Text(text = it, color = if (alarm.isReminder) Color.Yellow.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1)
                }

                // Mostrar repetición o fecha
                val infoText = when {
                    alarm.specificDate != null -> alarm.specificDate
                    alarm.repeatDays != null -> {
                        val daysMap = mapOf(1 to "D", 2 to "L", 3 to "M", 4 to "X", 5 to "J", 6 to "V", 7 to "S")
                        alarm.repeatDays.joinToString(" ") { daysMap[it] ?: "" }
                    }
                    else -> "Todos los días"
                }
                Text(text = infoText ?: "", color = Color.Cyan, fontSize = 11.sp)

                IconButton(onClick = { onDelete(alarm.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
            Switch(checked = alarm.isEnabled, onCheckedChange = { onToggle(alarm.id, it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Cyan))
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
        modifier = Modifier.fillMaxSize().background(color),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Alarm, contentDescription = null, tint = Color.White, modifier = Modifier.size(120.dp).scale(scale))
            
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

@Composable
fun MiniClimateCard(title: String, temp: Float, hum: Float, icon: ImageVector, accentColor: Color, modifier: Modifier = Modifier) {
    Surface(color = Color(0xFF121212), shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = String.format(Locale.getDefault(), "%.1f°", temp), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Text(text = String.format(Locale.getDefault(), "%.0f%% Hum", hum), color = Color.Gray, fontSize = 16.sp)
        }
    }
}

@Composable
fun PresenceMiniCard(isDetected: Boolean) {
    val bgColor by animateColorAsState(targetValue = if (isDetected) Color(0xFF1B5E20) else Color(0xFF333333), label = "presenceColor")
    Surface(color = bgColor, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(imageVector = if (isDetected) Icons.Default.Person else Icons.Default.PersonOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = if (isDetected) "PRESENCIA" else "VACÍO", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun SystemStatusCard(level: Int, isCharging: Boolean, isRelayOn: Boolean, onToggleRelay: (Boolean) -> Unit) {
    Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Batería J6", color = Color.Gray, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull, contentDescription = null, tint = if (level < 20) Color.Red else Color.Green, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "$level%", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Switch(checked = isRelayOn, onCheckedChange = onToggleRelay, colors = SwitchDefaults.colors(checkedThumbColor = Color.Green, checkedTrackColor = Color.Green.copy(alpha = 0.5f)))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = if (isRelayOn) "Cargador Activo" else "Cargador Apagado", color = if (isRelayOn) Color.Green else Color.Gray, fontSize = 11.sp)
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
                Icon(
                    imageVector = if (isRadio) Icons.Default.Radio else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isRadio) Color(0xFF00BCD4) else Color(0xFFE91E63),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRadio) "Radio Online" else "Navidrome",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Carátula o Icono de Radio
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF333333)
                    ) {
                        if (coverUrl != null) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = "Carátula",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isRadio) Icons.Default.Radio else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(text = artist ?: "Desconocido", color = Color.Gray, fontSize = 14.sp, maxLines = 1)
                    }

                    IconButton(
                        onClick = onToggleMusic,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            } else {
                Text(text = "Sin reproducción", color = Color.Gray, fontSize = 14.sp)
                Text(text = "Pide una canción o radio", color = Color(0xFF444444), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun MiniPressureCard(pressure: Float, modifier: Modifier = Modifier) {
    Surface(color = Color(0xFF121212), shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Air, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Pres.", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = String.format(Locale.getDefault(), "%.0f", pressure), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Text(text = "hPa", color = Color.Gray, fontSize = 14.sp)
        }
    }
}
