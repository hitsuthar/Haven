package com.hitsuthar.june.utils

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.hitsuthar.june.TORRSERVER_BASE_URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class TorrServerTorrentResponse(
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String,
    @SerializedName("poster") val poster: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("name") val name: String,
    @SerializedName("hash") val hash: String,
    @SerializedName("stat") val stat: Int,
    @SerializedName("stat_string") val statString: String,
    @SerializedName("torrent_size") val torrentSize: Long,
    @SerializedName("download_speed") val downloadSpeed: Double,
    @SerializedName("total_peers") val totalPeers: Int,
    @SerializedName("pending_peers") val pendingPeers: Int,
    @SerializedName("active_peers") val activePeers: Int,
    @SerializedName("connected_seeders") val connectedSeeders: Int,
    @SerializedName("bytes_written") val bytesWritten: Long,
    @SerializedName("bytes_read") val bytesRead: Long,
    @SerializedName("file_stats") val fileStats: List<FileStat>
)

data class FileStat(
    @SerializedName("id") val id: Int?,
    @SerializedName("path") val path: String?,
    @SerializedName("length") val length: Long?,
)

data class TorrServerTorrentsRequest(
    @SerializedName("action") val action: String,
    @SerializedName("link") val link: String?,
)

interface TorrServerApiService {
    @POST("/torrents")
    suspend fun torrents(
        @Body torrents: TorrServerTorrentsRequest
    ): TorrServerTorrentResponse
}


class LoggingInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        Log.d("TorrServerClient", "Sending request: ${request.url} \n ${request.body}")
        return chain.proceed(request)
    }
}

private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
    .addInterceptor(LoggingInterceptor())
    .build()

object TorrServerClient {

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TORRSERVER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val torrServerApiService: TorrServerApiService by lazy {
        retrofit.create(TorrServerApiService::class.java)
    }
}


suspend fun postTorrents(action: String, link: String?): TorrServerTorrentResponse {
    return withContext(Dispatchers.IO) {
        try {
            val response = TorrServerClient.torrServerApiService.torrents(
                TorrServerTorrentsRequest(action, link)
            )
            response
        } catch (e: IOException) {
            throw IOException("Check your internet connection or libTorrServer.so availability", e)
        } catch (e: Exception) {
            throw Exception("Unexpected error: ${e.localizedMessage}", e)
        }
    }
}