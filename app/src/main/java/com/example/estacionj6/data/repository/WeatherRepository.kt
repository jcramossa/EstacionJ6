package com.example.estacionj6.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.util.Log
import com.example.estacionj6.data.remote.WeatherApi
import java.util.Locale

data class WeatherData(
    val temperature: Float,
    val humidity: Float,
    val pressure: Float,
    val apparentTemperature: Float,
    val windSpeed: Float,
    val windDirection: String,
    val precipitation: Float,
    val cloudCover: Float,
    val weatherDescription: String,
    val uvIndex: Float,
    val locationName: String
)

class WeatherRepository(
    private val api: WeatherApi,
    private val context: Context,
    private val defaultLat: Double = 40.4168,
    private val defaultLon: Double = -3.7038
) {
    @SuppressLint("MissingPermission")
    private fun getLocation(): Pair<Double, Double> {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return Pair(defaultLat, defaultLon)
        try {
            val gpsLocation = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null

            val netLocation = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null

            val bestLocation = when {
                gpsLocation != null && netLocation != null -> if (gpsLocation.time > netLocation.time) gpsLocation else netLocation
                gpsLocation != null -> gpsLocation
                else -> netLocation
            }

            if (bestLocation != null) {
                return Pair(bestLocation.latitude, bestLocation.longitude)
            }
        } catch (e: SecurityException) {
            Log.e("SH_LOG", "Permiso de ubicación no concedido: ${e.message}")
        }
        return Pair(defaultLat, defaultLon)
    }

    private fun getCityName(lat: Double, lon: Double): String {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                return address.locality ?: address.subAdminArea ?: address.adminArea ?: "Ubicación GPS"
            }
        } catch (e: Exception) {
            Log.e("SH_LOG", "Error obteniendo ciudad con Geocoder: ${e.message}")
        }
        return String.format(Locale.getDefault(), "%.1f, %.1f", lat, lon)
    }

    private fun getWeatherDescription(code: Int?): String {
        return when (code) {
            0 -> "Despejado ☀️"
            1, 2, 3 -> "Parcialmente nublado ⛅"
            45, 48 -> "Niebla 🌫️"
            51, 53, 55, 56, 57 -> "Llovizna 🌧️"
            61, 63, 65, 66, 67 -> "Lluvia 🌧️"
            71, 73, 75, 77 -> "Nieve ❄️"
            80, 81, 82 -> "Chubascos 🌦️"
            95, 96, 99 -> "Tormenta ⛈️"
            else -> "Clima variable 🌤️"
        }
    }

    private fun getWindDirectionCardinal(degrees: Float?): String {
        if (degrees == null) return "N/D"
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
        val index = (((degrees + 22.5f) % 360) / 45).toInt()
        return "${directions[index]} (${degrees.toInt()}°)"
    }

    suspend fun fetchWeather(): Result<WeatherData> = runCatching {
        val (lat, lon) = getLocation()
        val locationName = getCityName(lat, lon)
        Log.d("SH_LOG", "--> Solicitando clima detallado de Open-Meteo para '$locationName' (lat: $lat, lon: $lon)")
        val response = api.getCurrentWeather(lat, lon)
        val current = response.current ?: throw IllegalStateException("No se encontraron datos de clima actuales")
        
        val temp = current.temperature
        val hum = current.humidity
        val press = current.pressure ?: 0f
        val apparent = current.apparentTemperature ?: temp
        val windSpd = current.windSpeed ?: 0f
        val windDir = getWindDirectionCardinal(current.windDirection)
        val precip = current.precipitation ?: 0f
        val clouds = current.cloudCover ?: 0f
        val desc = getWeatherDescription(current.weatherCode)
        val uv = current.uvIndex ?: 0f

        Log.d("SH_LOG", "<-- Clima completo recibido ($locationName): Temp=$temp°C, Precip=$precip mm, Nubes=$clouds%")
        WeatherData(temp, hum, press, apparent, windSpd, windDir, precip, clouds, desc, uv, locationName)
    }.onFailure { e ->
        Log.e("SH_LOG", "!-- Error obteniendo clima completo: ${e.message}", e)
    }
}
