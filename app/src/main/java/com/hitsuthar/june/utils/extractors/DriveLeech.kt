package com.hitsuthar.june.utils.extractors

import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.utils.DocumentFetcher
import org.jsoup.Jsoup

fun getDriveLeech(url: String, fetcher: DocumentFetcher): DDLStream? {
    return try {
        val doc = Jsoup.parse(fetcher.fetchWithRetries(url.replace("file", "zfile")))
        val title = doc.select("div > h5").text()
        val ddl = doc.select("a.btn").attr("href")
        if (ddl.isNotEmpty() || ddl.isNotBlank()) {
            DDLStream(title, ddl)
        } else {
            null
        }

    } catch (e: Exception) {
        null
    }
}