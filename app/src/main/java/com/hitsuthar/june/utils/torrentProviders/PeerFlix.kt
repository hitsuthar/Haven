package com.hitsuthar.june.utils.torrentProviders

import com.hitsuthar.june.utils.createMagnetLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

private interface PeerFlixService {
    @GET("language=en%7Csort=seed-desc/stream/{type}/{imdb_id}.json")
    suspend fun getPeerFlix(
        @Path("type") type: String,
        @Path("imdb_id") imdbId: String?
    ): TorrentResponse
}

private val retrofit = Retrofit.Builder()
    .baseUrl("https://peerflix-addon.onrender.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val torrentService = retrofit.create(PeerFlixService::class.java)

suspend fun getPeerFlix(type: String, imdbId: String?): Response<List<TorrentStream>> {
    return withContext(Dispatchers.IO) {
        try {
            val response = torrentService.getPeerFlix(type, imdbId).torrents
            Response.success(response.map { item ->
                if (item.sources?.isNotEmpty() == true) {
                    val trackers = item.sources.filter { it.startsWith("tracker:") }
                    item.magnet = createMagnetLink(item.infoHash!!, trackers)
                }
                item
            })
        } catch (e: Exception) {
            Response.error(500, okhttp3.ResponseBody.create(null, "Error: ${e.message}"))
        }
    }
}