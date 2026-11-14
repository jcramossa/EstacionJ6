package com.example.estacionj6.data.remote

import com.example.estacionj6.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,surface_pressure,apparent_temperature,wind_speed_10m,wind_direction_10m,weather_code,uv_index,precipitation,cloud_cover"
    ): WeatherResponse
}
