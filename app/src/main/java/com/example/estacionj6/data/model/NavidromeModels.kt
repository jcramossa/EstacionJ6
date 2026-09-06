package com.example.estacionj6.data.model

import com.google.gson.annotations.SerializedName

data class SubsonicResponse(
    @SerializedName("subsonic-response") val response: SubsonicContent
)

data class SubsonicContent(
    @SerializedName("status") val status: String,
    @SerializedName("version") val version: String,
    @SerializedName("searchResult3") val searchResult: SearchResult? = null,
    @SerializedName("artist") val artist: NavidromeArtist? = null,
    @SerializedName("album") val album: NavidromeAlbum? = null
)

data class SearchResult(
    @SerializedName("song") val songs: List<NavidromeSong>? = null,
    @SerializedName("artist") val artists: List<NavidromeArtist>? = null,
    @SerializedName("album") val albums: List<NavidromeAlbum>? = null
)

data class NavidromeArtist(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("album") val albums: List<NavidromeAlbum>? = null
)

data class NavidromeAlbum(
    @SerializedName("id") val id: String,
    @SerializedName("name") val title: String,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("song") val songs: List<NavidromeSong>? = null
)

data class NavidromeSong(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String? = null,
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("coverArt") val coverArt: String? = null
)

// Estado para la UI de música
data class MusicUiState(
    val currentSong: NavidromeSong? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val error: String? = null
)
