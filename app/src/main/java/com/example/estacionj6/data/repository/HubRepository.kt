package com.example.estacionj6.data.repository

import com.example.estacionj6.data.model.EspHomeStatus
import com.example.estacionj6.data.model.ExternalSensorStatus
import com.example.estacionj6.data.model.Alarm
import com.example.estacionj6.util.BatteryInfo

interface HubRepository {
    suspend fun fetchStatus(): Result<EspHomeStatus>
    suspend fun fetchExternalStatus(): Result<ExternalSensorStatus>
    suspend fun setRelayState(turnOn: Boolean): Result<Unit>
    fun getPhoneBatteryInfo(): BatteryInfo

    // Gestión de Alarmas
    fun getAlarms(): List<Alarm>
    fun saveAlarm(alarm: Alarm)
    fun deleteAlarm(alarmId: String)
    fun toggleAlarm(alarmId: String, enabled: Boolean)
}