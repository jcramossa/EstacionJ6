package com.example.estacionj6.data.repository

import android.content.Context
import com.example.estacionj6.data.model.Alarm
import com.example.estacionj6.data.model.EspHomeStatus
import com.example.estacionj6.data.model.ExternalSensorStatus
import com.example.estacionj6.data.remote.EspHomeApi
import com.example.estacionj6.util.BatteryInfo
import com.example.estacionj6.util.BatteryManagerHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HubRepositoryImpl(
    private val api: EspHomeApi,
    private val context: Context
) : HubRepository {

    private val prefs = context.getSharedPreferences("smart_hub_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    override suspend fun fetchStatus(): Result<EspHomeStatus> = runCatching {
        api.getStatus()
    }

    override suspend fun fetchExternalStatus(): Result<ExternalSensorStatus> = runCatching {
        api.getExternalStatus()
    }

    override suspend fun setRelayState(turnOn: Boolean): Result<Unit> = runCatching {
        val action = if (turnOn) "on" else "off"
        api.controlRelay(action)
        Unit
    }

    override fun getPhoneBatteryInfo(): BatteryInfo {
        return BatteryManagerHelper.getBatteryInfo(context)
    }

    // --- GESTIÓN DE ALARMAS ---

    override fun getAlarms(): List<Alarm> {
        val json = prefs.getString("alarms", null) ?: return emptyList()
        val type = object : TypeToken<List<Alarm>>() {}.type
        return gson.fromJson(json, type)
    }

    override fun saveAlarm(alarm: Alarm) {
        val alarms = getAlarms().toMutableList()
        alarms.removeAll { it.id == alarm.id }
        alarms.add(alarm)
        saveAlarmsList(alarms)
    }

    override fun deleteAlarm(alarmId: String) {
        val alarms = getAlarms().toMutableList()
        alarms.removeAll { it.id == alarmId }
        saveAlarmsList(alarms)
    }

    override fun toggleAlarm(alarmId: String, enabled: Boolean) {
        val alarms = getAlarms().map {
            if (it.id == alarmId) it.copy(isEnabled = enabled) else it
        }
        saveAlarmsList(alarms)
    }

    private fun saveAlarmsList(alarms: List<Alarm>) {
        val json = gson.toJson(alarms)
        prefs.edit().putString("alarms", json).apply()
    }
}
