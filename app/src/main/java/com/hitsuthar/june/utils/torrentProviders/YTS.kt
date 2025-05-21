package com.hitsuthar.june.utils.torrentProviders

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Response


private const val BASE_URL = "https://itorrentsearch.vercel.app/api/"
private const val TAG = "YTSProvider"


suspend fun getYTS(query: String, year: Int?): Response<List<TorrentStream>> {
    return withContext(Dispatchers.IO) {
        val searchString = query
            .trim() // Trim leading and trailing spaces
            .replace(Regex(" +\\(.*|&|:"), "") // Replace patterns matching the regex
            .replace(Regex("\\s+"), "-")
        val url =
            "https://yts.do/movie/${searchString}-${year}/"

        try {
            val document: Document = Jsoup.connect(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.86 Safari/537.36"
                )
                .get()
            return@withContext Response.success(extractYTSData(document))
        } catch (e: Exception) {
            Log.e("HTTP_ERROR", "Failed to fetch page", e)
            return@withContext Response.success(null)
        }
    }
}

private fun extractYTSData(document: Document): List<TorrentStream> {
//    Log.d("DOC", "Doc\n" + document.body())
    val rows = document.select("div.modal-torrent")
//    Log.d("YTS", "Rows: $rows")
    val torrents = mutableListOf<TorrentStream>()

    for (row in rows) {
        val quality = row.select("div.modal-quality span").text()
        val fileSize = row.select("p.quality-size").last()?.text() ?: ""
        val magnetLink = row.select("a.magnet-download").attr("href")

        torrents.add(
            TorrentStream(
                magnet = magnetLink,
                size = fileSize,
                quality = quality
            )
        )
    }
    Log.d("YTS", "Title: $torrents")

//    Log.d("CloudTorrent", "Torrents: $torrents")
    return torrents
}