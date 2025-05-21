package com.hitsuthar.june.utils.torrentProviders

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Response


suspend fun getEZTV(query: String): Response<List<TorrentStream>> {
    return withContext(Dispatchers.IO) {
        val formattedQuery = query.trim().replace(" ", "-")
        Log.d("query","query: $formattedQuery")
        val url =
            "https://eztvx.to/search/$formattedQuery"

        try {
            val document: Document = Jsoup.connect(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.86 Safari/537.36"
                )
                .get()
            return@withContext Response.success(extractEZTVData(document))
        } catch (e: Exception) {
            Log.e("HTTP_ERROR", "Failed to fetch page", e)
            return@withContext Response.success(null)
        }
    }
}

private fun extractEZTVData(document: Document): List<TorrentStream> {
//    Log.d("DOC", "Doc\n" + document.body())
    val rows = document.select("table.forum_header_border tbody tr.forum_header_border")
    val torrents = mutableListOf<TorrentStream>()

    for (row in rows) {
        val titleElement: Element? = row.select("td.forum_thread_post a.epinfo").first()
        val magnetElement: Element? = row.select("td.forum_thread_post a.magnet").first()
        val sizeElement: Element? = row.select("td.forum_thread_post:nth-child(3)").first()
        val seedersElement: Element? = row.select("td.forum_thread_post_end").first()

        val title = titleElement?.text() ?: "Unknown"
        val magnetLink = magnetElement?.attr("href") ?: "No Magnet Link"
        val size = sizeElement?.text() ?: "Unknown"
        val seeders = seedersElement?.text()?.toIntOrNull() ?: 0

        Log.d("Torrent", "Title: $titleElement")

        torrents.add(
            TorrentStream(
                title = title,
                size = size,
                seeders = seeders,
                magnet = magnetLink
            )
        )
    }
    return torrents
}