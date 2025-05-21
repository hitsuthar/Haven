package com.hitsuthar.june.utils.torrentProviders

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

private interface BrazucaService {
    @GET("stream/{type}/{imdb_id}.json")
    suspend fun getBrazuca(
        @Path("type") type: String,
        @Path("imdb_id") imdbId: String?
    ): TorrentResponse
}

private val retrofit = Retrofit.Builder()
    .baseUrl("https://94c8cb9f702d-brazuca-torrents.baby-beamup.club/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val torrentService = retrofit.create(BrazucaService::class.java)

suspend fun getBrazuca(type: String, imdbId: String?): List<TorrentStream> {
    return withContext(Dispatchers.IO) {
        val response = torrentService.getBrazuca(type, imdbId)
        response.torrents
    }
}