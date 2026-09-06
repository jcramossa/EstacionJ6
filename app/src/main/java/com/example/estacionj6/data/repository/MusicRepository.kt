package com.example.estacionj6.data.repository

import com.example.estacionj6.data.model.NavidromeSong
import com.example.estacionj6.data.remote.NavidromeApi
import java.security.MessageDigest
import java.util.*

class MusicRepository(
    private val api: NavidromeApi,
    private val user: String,
    private val pass: String,
    private val baseUrl: String
) {

    suspend fun searchSongs(query: String): Result<List<NavidromeSong>> = runCatching {
        val salt = UUID.randomUUID().toString().substring(0, 8)
        val token = md5(pass + salt)
        
        val response = api.search(query, user, token, salt)
        val result = response.response.searchResult

        // 1. Si hay canciones directas, devolverlas
        val directSongs = result?.songs
        if (!directSongs.isNullOrEmpty()) {
            return@runCatching directSongs
        }

        // 2. Si no hay canciones pero hay un artista, buscar sus canciones
        val artistId = result?.artists?.firstOrNull()?.id
        if (artistId != null) {
            val artistResp = api.getArtist(artistId, user, token, salt)
            val albumId = artistResp.response.artist?.albums?.firstOrNull()?.id
            if (albumId != null) {
                val albumResp = api.getAlbum(albumId, user, token, salt)
                return@runCatching albumResp.response.album?.songs ?: emptyList()
            }
        }

        // 3. Si no hay artista pero hay un álbum, buscar canciones del álbum
        val albumId = result?.albums?.firstOrNull()?.id
        if (albumId != null) {
            val albumResp = api.getAlbum(albumId, user, token, salt)
            return@runCatching albumResp.response.album?.songs ?: emptyList()
        }

        emptyList()
    }

    fun getStreamUrl(songId: String): String {
        val salt = UUID.randomUUID().toString().substring(0, 8)
        val token = md5(pass + salt)
        return "${baseUrl}rest/stream.view?id=$songId&u=$user&t=$token&s=$salt&v=1.16.1&c=SmartHub&f=json"
    }

    fun getCoverArtUrl(coverArtId: String?): String? {
        if (coverArtId == null) return null
        val salt = UUID.randomUUID().toString().substring(0, 8)
        val token = md5(pass + salt)
        return "${baseUrl}rest/getCoverArt.view?id=$coverArtId&u=$user&t=$token&s=$salt&v=1.16.1&c=SmartHub"
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
