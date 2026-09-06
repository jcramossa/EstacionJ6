package com.example.estacionj6.data.remote

import com.example.estacionj6.data.model.RadioStation
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RadioApi {
    @GET("json/stations/byname/{name}")
    suspend fun searchStations(
        @Path("name") name: String,
        @Query("limit") limit: Int = 5,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true
    ): List<RadioStation>
}
