package com.hitsuthar.june.utils.torrentProviders

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.hitsuthar.june.utils.createMagnetLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

data class TorrentResponse(
    @SerializedName(value = "streams", alternate = ["torrents"]) val torrents: List<TorrentStream>
)

data class TorrentStream(
    val name: String? = null,
    @SerializedName(value = "title", alternate = ["Name"]) val title: String? = null,
    @SerializedName(
        value = "infoHash", alternate = ["infohash", "hash"]
    ) var infoHash: String? = null,
    @SerializedName(value = "quality") val quality: String? = null,
    @SerializedName(value = "fileIdx") var fileIndex: Int? = null,
    val behaviorHints: BehaviorHints? = BehaviorHints(),
    @SerializedName(
        value = "magnet", alternate = ["Magnet", "magnet_url"]
    ) var magnet: String? = null,
    @SerializedName(value = "seeders", alternate = ["Seeders", "seeds"]) val seeders: Int? = null,
    @SerializedName(
        value = "peers", alternate = ["leechers", "Leechers"]
    ) val peers: Int? = null,
    @SerializedName("sources") val sources: List<String>? = null,
    @SerializedName(value = "size", alternate = ["Size"]) var size: String? = null,
    @SerializedName(value = "size_bytes") val sizeBytes: Long? = null,
    val uploaded: String = "",
    val Language: String = "",
)

data class BehaviorHints(
    val bingeGroup: String = "", val filename: String = ""
)

interface TorrentioService {
    @GET("sort=seeders/stream/{type}/{imdb_id}.json")
    suspend fun getTorrentio(
        @Path("type") type: String, @Path("imdb_id") imdbId: String?
    ): TorrentResponse
}

private val retrofit = Retrofit.Builder().baseUrl("https://torrentio.strem.fun/")
    .addConverterFactory(GsonConverterFactory.create()).build()

private val torrentService = retrofit.create(TorrentioService::class.java)

suspend fun getTorrentio(type: String, imdbId: String?): Response<List<TorrentStream>> {
    Log.d("getTorrentio", "imdbId: $imdbId")
    return withContext(Dispatchers.IO) {
        try {
            val response = torrentService.getTorrentio(type, imdbId).torrents
            Response.success(response.map { item ->
                if (item.sources?.isNotEmpty() == true) {
                    val trackers = item.sources.filter { it.startsWith("tracker:") }
                    item.magnet = createMagnetLink(item.infoHash!!, trackers)
                }
                item.fileIndex = item.fileIndex?.plus(1)
                item
            })
        } catch (e: Exception) {
            Response.error(500, okhttp3.ResponseBody.create(null, "Error: ${e.message}"))
        }
    }
}