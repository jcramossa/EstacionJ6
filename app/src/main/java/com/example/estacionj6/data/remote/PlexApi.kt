package com.example.estacionj6.data.remote

import com.example.estacionj6.data.model.PlexResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface PlexApi {
    @GET("library/sections/all")
    suspend fun getAllLibraries(
        @Query("X-Plex-Token") token: String
    ): PlexResponse

    @GET("search")
    suspend fun search(
        @Query("query") query: String,
        @Query("X-Plex-Token") token: String
    ): PlexResponse

    @GET("library/metadata/{key}")
    suspend fun getMetadata(
        @Path("key") key: String,
        @Query("X-Plex-Token") token: String
    ): PlexResponse

    @GET("library/metadata/{key}/children")
    suspend fun getChildren(
        @Path("key") key: String,
        @Query("X-Plex-Token") token: String
    ): PlexResponse
}
