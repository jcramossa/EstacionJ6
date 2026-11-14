package com.example.estacionj6.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.estacionj6.data.model.HubUiState
import com.example.estacionj6.data.model.Alarm
import com.example.estacionj6.data.repository.HubRepository
import com.example.estacionj6.data.repository.MusicRepository
import com.example.estacionj6.data.repository.RadioRepository
import com.example.estacionj6.data.repository.PlexRepository
import com.example.estacionj6.data.repository.WeatherRepository
import com.example.estacionj6.util.VoiceAssistantManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(
    private val repository: HubRepository,
    private val musicRepository: MusicRepository,
    private val radioRepository: RadioRepository,
    private val plexRepository: PlexRepository,
    private val weatherRepository: WeatherRepository,
    private val voiceManager: VoiceAssistantManager,
    val player: ExoPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(HubUiState())
    val uiState: StateFlow<HubUiState> = _uiState.asStateFlow()

    private val _manualWakeActive = MutableStateFlow(false)

    init {
        loadAlarms()
        startClock()
        startPolling()
        setupPlayer()
        setupVoiceManager()
    }

    private fun loadAlarms() {
        Log.e("SH_LOG", "Cargando alarmas...")
        val alarms = repository.getAlarms()
        Log.e("SH_LOG", "Total alarmas: ${alarms.size}")
        _uiState.update { it.copy(alarms = alarms) }
    }

    private fun setupVoiceManager() {
        viewModelScope.launch {
            voiceManager.isListening.collect { isListening ->
                _uiState.update { it.copy(isListening = isListening) }
                // Atenuar volumen si está escuchando
                if (isListening) {
                    player.volume = 0.2f
                } else {
                    player.volume = 1.0f
                }
            }
        }
        viewModelScope.launch {
            voiceManager.isThinking.collect { isThinking ->
                _uiState.update { it.copy(isThinking = isThinking) }
            }
        }
        viewModelScope.launch {
            voiceManager.transcript.collect { transcript ->
                if (transcript.isNotEmpty()) {
                    _uiState.update { it.copy(voiceTranscript = transcript) }
                    viewModelScope.launch {
                        delay(5000)
                        _uiState.update { it.copy(voiceTranscript = null) }
                    }
                }
            }
        }
    }

    private fun setupPlayer() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                _uiState.update { it.copy(isMusicLoading = playbackState == Player.STATE_BUFFERING) }
            }
        })
    }

    private fun startClock() {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
            val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            while (true) {
                val now = Date()
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                val currentSecond = calendar.get(Calendar.SECOND)
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val todayStr = dateParser.format(now)

                _uiState.update { currentState ->
                    val firedAlarm = currentState.alarms.firstOrNull { alarm ->
                        if (!alarm.isEnabled) return@firstOrNull false
                        val timeMatch = alarm.hour == currentHour && alarm.minute == currentMinute && currentSecond == 0
                        if (!timeMatch) return@firstOrNull false
                        
                        when {
                            alarm.specificDate != null -> alarm.specificDate == todayStr
                            alarm.repeatDays != null -> alarm.repeatDays.contains(currentDayOfWeek)
                            else -> true
                        }
                    }
                    
                    if (firedAlarm != null) {
                        if (firedAlarm.isReminder) {
                            firedAlarm.label?.let { msg -> voiceManager.speak("Atención, recordatorio: $msg") }
                        } else {
                            voiceManager.speak("Alarma activada")
                        }
                    }

                    currentState.copy(
                        currentTime = timeFormat.format(now),
                        currentDate = dateFormat.format(now).replaceFirstChar { it.uppercase() },
                        isAlarmFiring = if (firedAlarm != null && !firedAlarm.isReminder) true else currentState.isAlarmFiring,
                        firingAlarmLabel = firedAlarm?.label ?: currentState.firingAlarmLabel
                    )
                }
                delay(1000)
            }
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                refreshData()
                delay(5000)
            }
        }
    }

    suspend fun refreshData() {
        val batteryInfo = repository.getPhoneBatteryInfo()

        // Actualizar batería siempre, incluso si falla el ESP32
        _uiState.update { currentState ->
            currentState.copy(
                phoneBatteryLevel = batteryInfo.level,
                isPhoneCharging = batteryInfo.isCharging
            )
        }

        // Obtener clima exterior y ubicación de la web (Open-Meteo + Geocoder)
        val weatherResult = withContext(Dispatchers.IO) {
            weatherRepository.fetchWeather()
        }
        weatherResult.onSuccess { weatherData ->
            _uiState.update { currentState ->
                currentState.copy(
                    extTemperature = weatherData.temperature,
                    extHumidity = weatherData.humidity,
                    extPressure = weatherData.pressure,
                    extApparentTemp = weatherData.apparentTemperature,
                    windSpeed = weatherData.windSpeed,
                    windDirection = weatherData.windDirection,
                    precipitation = weatherData.precipitation,
                    cloudCover = weatherData.cloudCover,
                    weatherDescription = weatherData.weatherDescription,
                    uvIndex = weatherData.uvIndex,
                    weatherLocationName = weatherData.locationName
                )
            }
        }

        val internalResult = repository.fetchStatus()

        internalResult.onSuccess { status ->
            _uiState.update { currentState ->
                currentState.copy(
                    temperature = status.ahtTemp,
                    humidity = status.ahtHum,
                    pressure = status.bmpPressure,
                    isPresenceDetected = status.presence,
                    isMovingPresence = status.movement,
                    isStaticPresence = status.staticPresence,
                    isRelayOn = status.relay,
                    isDimmed = !status.presence && !_manualWakeActive.value && !currentState.isAlarmFiring,
                    isLoading = false,
                    error = null
                )
            }
            evaluateBatteryLogic(batteryInfo.level, status.relay)
        }.onFailure { exception ->
            Log.e("SH_LOG", "Error obteniendo status de ESP32 (.13): ${exception.localizedMessage}", exception)
            _uiState.update { it.copy(isLoading = false, error = "Error ESP32 (.13): ${exception.localizedMessage}") }
        }
    }

    private var lastRelayCommand: Boolean? = null

    private fun evaluateBatteryLogic(batteryLevel: Int, isRelayCurrentlyOn: Boolean) {
        if (batteryLevel == -1) return // Ignorar si hay error de lectura
        
        Log.d("SH_LOG", "Batería: $batteryLevel% | Relay (ESP32): $isRelayCurrentlyOn | Last Cmd: $lastRelayCommand")

        viewModelScope.launch {
            // REGLA 1: Si baja del 20% y no está cargando -> ENCENDER
            if (batteryLevel <= 20 && !isRelayCurrentlyOn) {
                Log.e("SH_LOG", ">>> ACCIÓN: Batería baja. Encendiendo relé.")
                toggleRelay(true)
                lastRelayCommand = true
            }
            // REGLA 2: Si llega al 80% y está cargando -> APAGAR
            else if (batteryLevel >= 80 && isRelayCurrentlyOn) {
                Log.e("SH_LOG", ">>> ACCIÓN: Batería cargada. Apagando relé.")
                toggleRelay(false)
                lastRelayCommand = false
            }
            // REGLA 3: Seguridad. Si por alguna razón está fuera del rango 20-80 y 
            // el estado no coincide con lo esperado, lo corregimos (opcional, pero ayuda)
            // Si está a 79% y está APAGADO, no debe hacer nada.
        }
    }

    fun manualWake() {
        viewModelScope.launch {
            _manualWakeActive.value = true
            _uiState.update { it.copy(isDimmed = false) }
            delay(30_000)
            _manualWakeActive.value = false
            _uiState.update { it.copy(isDimmed = !it.isPresenceDetected && !it.isAlarmFiring) }
        }
    }

    fun toggleRelay(turnOn: Boolean) {
        Log.e("SH_LOG", ">>> toggleRelay llamado con: $turnOn")
        viewModelScope.launch(Dispatchers.IO) {
            repository.setRelayState(turnOn).onSuccess {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isRelayOn = turnOn) }
                }
            }.onFailure { exception ->
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = "Fallo al conmutar relé: ${exception.message}") }
                }
            }
        }
    }

    fun addAlarm(hour: Int, minute: Int, repeatDays: List<Int>? = null, specificDate: String? = null, label: String? = null, isReminder: Boolean = false) {
        Log.e("SH_LOG", "Añadiendo ${if (isReminder) "Recordatorio" else "Alarma"}: $hour:$minute")
        val newAlarm = Alarm(hour = hour, minute = minute, repeatDays = repeatDays, specificDate = specificDate, label = label, isReminder = isReminder)
        repository.saveAlarm(newAlarm)
        loadAlarms()
    }

    fun deleteAlarm(alarmId: String) {
        repository.deleteAlarm(alarmId)
        loadAlarms()
    }

    fun toggleAlarm(alarmId: String, enabled: Boolean) {
        repository.toggleAlarm(alarmId, enabled)
        loadAlarms()
    }

    fun dismissAlarm() {
        _uiState.update { it.copy(isAlarmFiring = false) }
    }

    fun startVoiceAssistant() {
        Log.e("SH_LOG", "Iniciando asistente de voz...")
        voiceManager.startListening { transcript ->
            Log.e("SH_LOG", "Voz recibida: $transcript")
            voiceManager.processIntent(transcript) { command ->
                viewModelScope.launch {
                    handleAssistantCommand(command)
                }
            }
        }
    }

    private fun handleAssistantCommand(command: VoiceAssistantManager.AssistantCommand) {
        Log.e("SH_LOG", "Acción IA: ${command.javaClass.simpleName}")
        when (command) {
            is VoiceAssistantManager.AssistantCommand.MusicPlay -> playMusic(command.query)
            is VoiceAssistantManager.AssistantCommand.RadioPlay -> playRadio(command.query)
            is VoiceAssistantManager.AssistantCommand.YouTubePlay -> playYouTube(command.query)
            is VoiceAssistantManager.AssistantCommand.PlexPlay -> playPlex(command.query, command.season, command.episode)
            is VoiceAssistantManager.AssistantCommand.MusicStop -> stopMusic()
            is VoiceAssistantManager.AssistantCommand.RelayOn -> toggleRelay(true)
            is VoiceAssistantManager.AssistantCommand.RelayOff -> toggleRelay(false)
            is VoiceAssistantManager.AssistantCommand.AlarmSet -> {
                addAlarm(command.hour, command.minute, command.repeatDays, command.specificDate, command.label, command.isReminder)
            }
            else -> {}
        }
    }

    fun stopMusic() {
        player.stop()
        _uiState.update { it.copy(
            musicTitle = null, 
            musicArtist = null, 
            isPlaying = false,
            isRadioActive = false,
            radioName = null,
            isYoutubeActive = false,
            youtubeVideoId = null,
            isPlexActive = false,
            plexTitle = null,
            plexMediaUrl = null
        ) }
    }

    fun playMusic(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMusicLoading = true, isRadioActive = false) }
            Log.e("SH_LOG", "Navidrome -> Buscando: $query")
            musicRepository.searchSongs(query).onSuccess { songs ->
                Log.e("SH_LOG", "Navidrome -> Encontradas: ${songs.size}")
                songs.firstOrNull()?.let { song ->
                    Log.e("SH_LOG", "Navidrome -> Play: ${song.title}")
                    val streamUrl = musicRepository.getStreamUrl(song.id)
                    player.setMediaItem(MediaItem.fromUri(streamUrl))
                    player.prepare()
                    player.play()
                    _uiState.update { it.copy(
                        musicTitle = song.title,
                        musicArtist = song.artist,
                        musicCoverUrl = musicRepository.getCoverArtUrl(song.coverArt),
                        isMusicLoading = false
                    ) }
                } ?: run {
                    Log.e("SH_LOG", "Navidrome -> No hubo coincidencias")
                    _uiState.update { it.copy(error = "No encontré: $query", isMusicLoading = false) }
                }
            }.onFailure {
                Log.e("SH_LOG", "Navidrome -> ERROR RED/API", it)
                _uiState.update { it.copy(error = "Error Navidrome", isMusicLoading = false) }
            }
        }
    }

    fun playRadio(query: String) {
        viewModelScope.launch {
            stopMusic() // Parar cualquier cosa previa
            _uiState.update { it.copy(isMusicLoading = true, isRadioActive = true) }
            Log.e("SH_LOG", "Radio -> Buscando: $query")
            radioRepository.searchStation(query).onSuccess { station ->
                if (station != null) {
                    Log.e("SH_LOG", "Radio -> Play: ${station.name}")
                    player.setMediaItem(MediaItem.fromUri(station.url))
                    player.prepare()
                    player.play()
                    _uiState.update { it.copy(
                        radioName = station.name,
                        musicTitle = "En directo",
                        musicArtist = station.name,
                        musicCoverUrl = station.favicon,
                        isMusicLoading = false
                    ) }
                } else {
                    Log.e("SH_LOG", "Radio -> No encontrada")
                    _uiState.update { it.copy(error = "Radio no encontrada: $query", isMusicLoading = false) }
                }
            }.onFailure {
                Log.e("SH_LOG", "Radio -> ERROR", it)
                _uiState.update { it.copy(error = "Error al buscar radio", isMusicLoading = false) }
            }
        }
    }

    fun playYouTube(query: String) {
        viewModelScope.launch {
            stopMusic()
            Log.e("SH_LOG", "YouTube -> Solicitado: $query")
            _uiState.update { it.copy(
                isYoutubeActive = true,
                youtubeVideoId = "dQw4w9WgXcQ" // Prueba
            ) }
        }
    }

    fun playPlex(query: String, season: Int? = null, episode: Int? = null) {
        viewModelScope.launch {
            stopMusic()
            Log.e("SH_LOG", "Plex -> Buscando: $query (T$season E$episode)")
            plexRepository.searchMedia(query, season, episode).onSuccess { metadata ->
                if (metadata != null) {
                    val fileKey = metadata.media?.firstOrNull()?.parts?.firstOrNull()?.fileKey
                    if (fileKey != null) {
                        val streamUrl = plexRepository.getStreamUrl(fileKey)
                        Log.e("SH_LOG", "Plex -> Play: ${metadata.title}")
                        // Por ahora Plex lo tratamos como vídeo directo
                        _uiState.update { it.copy(
                            isPlexActive = true,
                            plexTitle = metadata.title,
                            plexMediaUrl = streamUrl
                        ) }
                    }
                } else {
                    _uiState.update { it.copy(error = "No encontrado en Plex: $query") }
                }
            }.onFailure {
                Log.e("SH_LOG", "Plex -> ERROR", it)
            }
        }
    }

    fun toggleMusic() {
        if (player.isPlaying) player.pause() else player.play()
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
        voiceManager.release()
    }
}
