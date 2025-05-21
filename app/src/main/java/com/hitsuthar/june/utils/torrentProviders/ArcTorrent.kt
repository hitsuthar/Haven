package com.hitsuthar.june.utils.torrentProviders

import android.util.Log
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path


interface ArcTorrentService {
    @GET("piratebay/{query}")
    suspend fun getTheHiddenBay(
        @Path("query") query: String,
    ): List<TorrentStream>

    @GET("rarbg/{query}")
    suspend fun getRARBG(
        @Path("query") query: String,
    ): List<TorrentStream>

    @GET("1337x/{query}")
    suspend fun get1337x(
        @Path("query") query: String,
    ): List<TorrentStream>
}


private const val BASE_URL = "https://itorrentsearch.vercel.app/api/"
private const val TAG = "TheHiddenBayProvider"

object ArcTorrentApi {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val arcTorrentService: ArcTorrentService by lazy {
        retrofit.create(ArcTorrentService::class.java)
    }
}

suspend fun getTheHiddenBay(query: String): Response<List<TorrentStream>> {
    return try {
        val response = ArcTorrentApi.arcTorrentService.getTheHiddenBay(query)
        Log.d(TAG, "Response: $response")
        Response.success(response)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching The Hidden Bay data", e)
        Response.error(500, okhttp3.ResponseBody.create(null, "Error fetching data"))
    }
}

suspend fun getRARBG(query: String): Response<List<TorrentStream>> {
    return try {
        val response = ArcTorrentApi.arcTorrentService.getRARBG(query)
        Log.d(TAG, "Response: $response")
        Response.success(response)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching RARBG data", e)
        Response.error(500, okhttp3.ResponseBody.create(null, "Error fetching data"))
    }
}

suspend fun get1337x(query: String): Response<List<TorrentStream>> {
    return try {
        val response = ArcTorrentApi.arcTorrentService.get1337x(query)
        Log.d(TAG, "Response: $response")
        Response.success(response)
    } catch (e: Exception) {
        Log.e(TAG, "Error fetching RARBG data", e)
        Response.error(500, okhttp3.ResponseBody.create(null, "Error fetching data"))
    }
}