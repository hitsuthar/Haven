package com.hitsuthar.june.utils.torrentProviders

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Response

suspend fun getTorrentGalaxy(query: String): Response<List<TorrentStream>> {
    return withContext(Dispatchers.IO) {
        val formattedQuery = query.trim().replace(" ", "+")
        val url =
            "https://torrentgalaxy.one/torrents.php?search=${formattedQuery}&sort=seeders&order=desc"

        try {
            val document: Document = Jsoup.connect(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.86 Safari/537.36"
                )
                .get()
            return@withContext Response.success(extractTorrentGalaxyData(document))
        } catch (e: Exception) {
            Log.e("HTTP_ERROR", "Failed to fetch page", e)
            return@withContext Response.success(null)
        }
    }
}

private fun extractTorrentGalaxyData(document: Document): List<TorrentStream> {
//    Log.d("DOC", "Doc\n" + document.body())
    val rows = document.select("div.tgxtablerow")
//    Log.d("TorrentGalaxy", "Rows: $rows")
    val torrents = mutableListOf<TorrentStream>()

    for (row in rows) {
        val titleElement: Element? = row.select("div.tgxtablecell.clickable-row").first()
        val magnetElement: Element? = row.select("a[href*='magnet']").first()
        val sizeElement: Element? =
            row.select("div.tgxtablecell span.badge.badge-secondary.txlight").first()
        val uploadedElement: Element? = row.select("td[data-title=Uploaded] span").first()
        val seedersElement: Element? =
            row.select("div.tgxtablecell font[color=green] b").first()
        val leechersElement: Element? = row.select("div.tgxtablecell font[color=#ff0000] b").first()


        val title = titleElement?.select("b")?.text() ?: "Unknown"
        val magnetLink = magnetElement?.attr("href") ?: "No Magnet Link"
        val size = sizeElement?.text() ?: "Unknown"
        val uploadedDate = uploadedElement?.text() ?: "Unknown"
        val seeders = seedersElement?.text()?.replace(",", "")?.toIntOrNull() ?: 0
        val leechers = leechersElement?.text()?.replace(",", "")?.toIntOrNull() ?: 0

        torrents.add(
            TorrentStream(
                title = title,
                magnet = magnetLink,
                size = size,
                uploaded = uploadedDate,
                seeders = seeders,
                peers = leechers
            )
        )
    }
    // Print scraped data
    val torrent = torrents.firstOrNull()
    if (torrent != null) {
        Log.d("CloudTorrent", "Title: ${torrent.title}")
        Log.d("CloudTorrent", "Magnet Link: ${torrent.magnet}")
        Log.d("CloudTorrent", "Size: ${torrent.size}")
    }
//    Log.d("CloudTorrent", "Torrents: $torrents")
    return torrents
}