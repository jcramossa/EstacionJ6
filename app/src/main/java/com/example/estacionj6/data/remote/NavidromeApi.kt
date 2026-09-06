package com.example.estacionj6.data.remote

import com.example.estacionj6.data.model.SubsonicResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NavidromeApi {

    @GET("rest/search3.view")
    suspend fun search(
        @Query("query") query: String,
        @Query("u") user: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "SmartHub",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getArtist.view")
    suspend fun getArtist(
        @Query("id") id: String,
        @Query("u") user: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "SmartHub",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(
        @Query("id") id: String,
        @Query("u") user: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "SmartHub",
        @Query("f") format: String = "json"
    ): SubsonicResponse
}
