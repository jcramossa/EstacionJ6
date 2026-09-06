package com.example.estacionj6.data.remote

import com.example.estacionj6.data.model.EspHomeStatus
import com.example.estacionj6.data.model.ExternalSensorStatus
import okhttp3.ResponseBody
import retrofit2.http.*

interface EspHomeApi {

    // --- ENDPOINT UNIFICADO INTERNO (.13) ---
    @GET("api/status")
    suspend fun getStatus(): EspHomeStatus

    // --- ENDPOINT EXTERIOR (.12) ---
    @GET
    suspend fun getExternalStatus(@Url url: String = "http://192.168.1.12/status"): ExternalSensorStatus

    // --- ACCIONES ---
    @POST("api/rele/{action}")
    suspend fun controlRelay(
        @Path("action") action: String
    ): ResponseBody
}