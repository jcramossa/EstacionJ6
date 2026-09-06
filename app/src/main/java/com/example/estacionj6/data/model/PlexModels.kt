package com.example.estacionj6.data.model

import com.google.gson.annotations.SerializedName

data class PlexResponse(
    @SerializedName("MediaContainer") val container: PlexContainer
)

data class PlexContainer(
    @SerializedName("Metadata") val metadata: List<PlexMetadata>? = null
)

data class PlexMetadata(
    @SerializedName("ratingKey") val key: String,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String, // movie, show, season, episode
    @SerializedName("index") val index: Int? = null, // Número de temporada o episodio
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("thumb") val thumb: String? = null,
    @SerializedName("Media") val media: List<PlexMedia>? = null
)

data class PlexMedia(
    @SerializedName("Part") val parts: List<PlexPart>? = null
)

data class PlexPart(
    @SerializedName("key") val fileKey: String
)
