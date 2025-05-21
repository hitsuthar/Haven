package com.hitsuthar.june.utils.dDLProviders

import android.util.Log
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.utils.DocumentFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder

suspend fun getOpenDirectoryDDL(
    title: String,
    year: Int? = null,
    type: String,
    season: Int? = null,
    episode: Int? = null,
    fetcher: DocumentFetcher
): Result<List<DDLStream>> = withContext(Dispatchers.IO) {
    Log.d("opendirectory", "hell yeahhh open directory is executing")
    val query = buildString {
        append(title)
        when (type) {
            "movie" -> if (year != null) append(" $year")
            "tv" -> {
                append(" S")
                append(season!!.toString().padStart(2, '0'))
                append("E")
                append(episode!!.toString().padStart(2, '0'))
            }
        }
    }.trim()

    val url = "https://filepursuit.com/pursuit?q=${
        URLEncoder.encode(
            query, "UTF-8"
        )
    }&type=video&sort=sizedesc"
    val document: Document = Jsoup.parse(fetcher.fetchWithRetries(url))
    val ddlStreams: MutableList<DDLStream> = emptyList<DDLStream>().toMutableList()
    try {
        document.select("div.file-post-item").forEach { row ->
            val link = Regex("'([^']+)'").find(row.select("a").attr("onclick"))?.groupValues?.get(1)
            if (link != null) ddlStreams.add(
                DDLStream(
                    row.select("h5").text(),
                    link,
                    row.select("div span.bg-primary").last()?.text()
                        ?.replace(oldValue = " ", newValue = "")
                )
            )
        }
    } catch (e: Exception) {
        Result.success(ddlStreams)
    }
    Log.d("opendirectory", ddlStreams.toString())
    Result.success(ddlStreams)
}