package com.example.estacionj6.data.remote

import com.example.estacionj6.data.model.EspHomeStatus
import okhttp3.ResponseBody
import retrofit2.http.*

interface EspHomeApi {

    // --- ENDPOINT UNIFICADO (.13) ---
    @GET("api/status")
    suspend fun getStatus(): EspHomeStatus

    // --- ACCIONES ---
    @POST("api/rele/{action}")
    suspend fun controlRelay(
        @Path("action") action: String
    ): ResponseBody
}
