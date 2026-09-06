package com.example.estacionj6.data.repository

import com.example.estacionj6.data.model.RadioStation
import com.example.estacionj6.data.remote.RadioApi

class RadioRepository(private val api: RadioApi) {
    suspend fun searchStation(query: String): Result<RadioStation?> = runCatching {
        val results = api.searchStations(query)
        results.firstOrNull()
    }
}
