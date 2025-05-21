package com.hitsuthar.june.utils.torrentProviders

import android.util.Log
import com.hitsuthar.june.utils.formatBytes
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


interface TorrentCSVService {
    @GET("search")
    suspend fun getTorrentCSV(
        @Query("q") query: String,
    ): TorrentResponse
}

private const val BASE_URL = "https://torrents-csv.com/service/"
private const val TAG = "TorrentCSVProvider"


object TorrentCSVApi {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val torrentCSVService: TorrentCSVService by lazy {
        retrofit.create(TorrentCSVService::class.java)
    }
}

suspend fun getTorrentCSV(query: String): Response<List<TorrentStream>> {
    Log.d(TAG, "Query: $query")
    val response = TorrentCSVApi.torrentCSVService.getTorrentCSV(
        query.replace(
            oldValue = ":",
            newValue = ""
        )
    ).torrents
    Log.d(TAG, "Response: $response")
    return Response.success(response.map {
        it.size = formatBytes(it.sizeBytes!!)
        it
    })
}