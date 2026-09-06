package com.example.estacionj6.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

// Nuevo modelo para el endpoint /api/status (Sensor Interno .13)
data class EspHomeStatus(
    @SerializedName("rele") val relay: Boolean,
    @SerializedName("presencia") val presence: Boolean,
    @SerializedName("movimiento") val movement: Boolean,
    @SerializedName("persona_estatica") val staticPresence: Boolean,
    @SerializedName("dht_temperatura") val dhtTemp: Float,
    @SerializedName("dht_humedad") val dhtHum: Float,
    @SerializedName("aht_temperatura") val ahtTemp: Float,
    @SerializedName("aht_humedad") val ahtHum: Float,
    @SerializedName("bmp_temperatura") val bmpTemp: Float,
    @SerializedName("bmp_presion") val bmpPressure: Float
)

// Modelo para alarmas
data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val label: String? = null,
    val repeatDays: List<Int>? = null,
    val specificDate: String? = null,
    val isReminder: Boolean = false // true para Recordatorio, false para Alarma
)

// Modelo para Radio
data class RadioStation(
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String,
    @SerializedName("favicon") val favicon: String? = null,
    @SerializedName("tags") val tags: String? = null
)

// Modelo para el Sensor Exterior (.12)
data class ExternalSensorStatus(
    @SerializedName("device") val device: String,
    @SerializedName("temperature_aht") val tempAht: Float,
    @SerializedName("humidity") val humidity: Float,
    @SerializedName("pressure") val pressure: Float,
    @SerializedName("relay") val relay: Boolean
)

// Estado unificado que consumirá la interfaz gráfica en Compose
data class HubUiState(
    val currentTime: String = "00:00",
    val currentDate: String = "",
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val pressure: Float = 0f,
    val extTemperature: Float = 0f,
    val extHumidity: Float = 0f,
    val isPresenceDetected: Boolean = false,
    val isMovingPresence: Boolean = false,
    val isStaticPresence: Boolean = false,
    val isRelayOn: Boolean = false,
    val phoneBatteryLevel: Int = 0,
    val isPhoneCharging: Boolean = false,
    val isDimmed: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,

    // Música y Radio
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val musicCoverUrl: String? = null,
    val isPlaying: Boolean = false,
    val isMusicLoading: Boolean = false,
    val isRadioActive: Boolean = false,
    val radioName: String? = null,
    val youtubeVideoId: String? = null,
    val isYoutubeActive: Boolean = false,
    val plexTitle: String? = null,
    val isPlexActive: Boolean = false,
    val plexMediaUrl: String? = null,

    // Voz
    val isListening: Boolean = false,
    val isThinking: Boolean = false,
    val voiceTranscript: String? = null,

    // Alarmas
    val alarms: List<Alarm> = emptyList(),
    val isAlarmFiring: Boolean = false,
    val firingAlarmLabel: String? = null
)
