package com.hitsuthar.june.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import retrofit2.Response


suspend fun getHubCloudUrl(
    url: String,
): Response<String> {
    return withContext(Dispatchers.IO) {
        try {
            var ddlLink: String = null.toString()

            val doc = Jsoup.connect(url).get()
            val link = if (url.contains("drive")) {
                val scriptTag = doc.selectFirst("script:containsData(url)")?.toString() ?: ""
                Regex("var url = '([^']*)'").find(scriptTag)?.groupValues?.get(1) ?: ""
            } else {
                doc.selectFirst("div.vd > center > a")?.attr("href") ?: ""
            }

            val document = Jsoup.connect(link).get()
            val div = document.selectFirst("div.card-body")
            div?.select("h2 a.btn")?.mapNotNull { element ->
                val link1 = element.attr("href")
                val text = element.text()
                when {
                    text.contains("Download [FSL Server]") -> ddlLink = link1
                    text.contains("Download File") -> ddlLink = link1
                    text.contains("PixelServer") -> ddlLink = link1
//                    text.contains("pixeldra") -> link1
//                    text.contains("BuzzServer") -> link1
                    else -> null
                }
            }

//            Log.d("getUrl", "Hubcloud DDL: $ddlLink")
            return@withContext Response.success(ddlLink)
        } catch (e: Exception) {
            Log.e("HTTP_ERROR", "Failed to fetch page", e)
            return@withContext Response.success(null)
        }
    }
}