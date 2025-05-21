package com.hitsuthar.june.utils.torrentProviders

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

private interface ThePirateBayService {
    @GET("stream/{type}/{imdb_id}.json")
    suspend fun getThePirateBay(
        @Path("type") type: String,
        @Path("imdb_id") imdbId: String?
    ): TorrentResponse
}

private val retrofit = Retrofit.Builder()
    .baseUrl("https://thepiratebay-plus.strem.fun/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val torrentService = retrofit.create(ThePirateBayService::class.java)

suspend fun getThePirateBay(type: String, imdbId: String?): List<TorrentStream> {
    return withContext(Dispatchers.IO) {
        val response = torrentService.getThePirateBay(type, imdbId)
        response.torrents
    }
}