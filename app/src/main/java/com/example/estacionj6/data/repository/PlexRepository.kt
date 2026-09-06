package com.example.estacionj6.data.repository

import com.example.estacionj6.data.model.PlexMetadata
import com.example.estacionj6.data.remote.PlexApi

class PlexRepository(
    private val api: PlexApi,
    private val token: String,
    private val baseUrl: String
) {
    suspend fun searchMedia(query: String, season: Int? = null, episode: Int? = null): Result<PlexMetadata?> = runCatching {
        val response = api.search(query, token)
        val firstResult = response.container.metadata?.firstOrNull()
        
        // Si el resultado es una serie (show)
        if (firstResult?.type == "show") {
            val showKey = firstResult.key // ratingKey
            
            // 1. Obtener temporadas
            val seasonsResp = api.getChildren(showKey, token)
            val seasons = seasonsResp.container.metadata ?: emptyList()
            
            // Seleccionar temporada (por defecto la 1 si no se especifica)
            val targetSeason = if (season != null) {
                seasons.firstOrNull { it.index == season }
            } else {
                seasons.firstOrNull { it.index != 0 } // Evitar especiales si es posible
            } ?: seasons.firstOrNull()

            if (targetSeason != null) {
                // 2. Obtener episodios de esa temporada
                val episodesResp = api.getChildren(targetSeason.key, token)
                val episodes = episodesResp.container.metadata ?: emptyList()
                
                // Seleccionar episodio (por defecto el 1 si no se especifica)
                val targetEpisode = if (episode != null) {
                    episodes.firstOrNull { it.index == episode }
                } else {
                    episodes.firstOrNull()
                }
                
                return@runCatching targetEpisode ?: firstResult
            }
        }
        
        firstResult
    }

    fun getStreamUrl(fileKey: String): String {
        // Añadimos parámetros para forzar Direct Play en el J6 si es posible
        return "$baseUrl$fileKey?X-Plex-Token=$token"
    }

    fun getThumbUrl(thumbKey: String?): String? {
        if (thumbKey == null) return null
        return "$baseUrl$thumbKey?X-Plex-Token=$token"
    }
}
