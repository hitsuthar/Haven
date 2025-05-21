package com.hitsuthar.junescrapper.extractors

import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.utils.DocumentFetcher
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLEncoder

@Serializable
data class JsonItem(
    val token: String,
    val id: Long,
    val time: Long,
    val post: String,
    val redirect: String,
    val cacha: String,
    val new: Boolean,
    val link: String
)

class Extractor {

    fun extract(url: String, fetcher: DocumentFetcher): DDLStream? {
        if (url.contains("hubcloud", ignoreCase = true)) {
            val ddl = getHubCloudUrl(url, fetcher)
            return ddl
        } else return null
    }

    fun getHubCloudUrl(
        url: String,
        fetcher: DocumentFetcher
    ): DDLStream? {
        return try {
            val doc = Jsoup.parse(fetcher.fetchWithRetries(url))

            val link = if (url.contains("drive") || url.contains("hubcloud", ignoreCase = true) && !url.contains("video", ignoreCase = true)) {
                val scriptTag = doc.selectFirst("script:containsData(url)")?.toString() ?: ""
                Regex("var url = '([^']*)'").find(scriptTag)?.groupValues?.get(1) ?: ""
            } else doc.selectFirst("div.vd > center > a")?.attr("href") ?: ""

            extractFromHUbCloud(link, fetcher)
        } catch (e: Exception) {
            null
        }
    }

    fun extractFromHUbCloud(url: String, fetcher: DocumentFetcher): DDLStream? {
        val document = Jsoup.parse(fetcher.fetchWithRetries(url))
        var ddl: String? = null
        val title = document.select("title").text()
        val size = document.select("li i[id=size]").text().replace(oldValue = " ", newValue = "")
        document.selectFirst("div.card-body")?.select("h2 a.btn")?.map { element ->
            val link1 = element.attr("href")

            val text = element.text()
            when {
                text.contains("Pixel", ignoreCase = true) -> ddl = link1
                text.contains("Download [FSL Server]") -> ddl = link1
                text.contains("Download File") -> ddl = link1
                text.contains("Download [Server : 1]", ignoreCase = true) -> ddl = link1
                else -> null
            }
        }
        return if (ddl != null) {
            DDLStream(title, ddl!!, size = size)
        } else null
    }

    fun getHDMovie(url: String, fetcher: DocumentFetcher): Document {
        val client = OkHttpClient.Builder().build()

        val jsCode = Jsoup.parse(fetcher.fetchWithRetries(url)).select("script").html()
        val postUrl = """"soralink_ajaxurl":"([^"]+)"""".toRegex().find(jsCode)?.groupValues?.get(1)
        val jsonData =
            """var\s+item\s*=\s*(\{.*?\});""".toRegex(RegexOption.DOT_MATCHES_ALL).find(jsCode)?.groupValues?.get(1)
        val item = Json.decodeFromString<JsonItem>(jsonData!!)
        val soraLink = """"soralink_z"\s*:\s*"([^"]+)"""".toRegex().find(jsCode)?.groupValues?.get(1)
        val postData = "token=${
            URLEncoder.encode(item.token, "UTF-8")
        }&id=${
            item.id
        }&time=${
            item.time
        }&post=${
            URLEncoder.encode(item.post, "UTF-8")
        }&redirect=${
            URLEncoder.encode(item.redirect, "UTF-8")
        }&cacha=${
            URLEncoder.encode(item.cacha, "UTF-8")
        }&new=${
            item.new
        }&link=${
            URLEncoder.encode(item.link, "UTF-8")
        }&action=$soraLink".toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val postRequest = Request.Builder().url(postUrl!!).post(postData).build()
        val postResponse = client.newCall(postRequest).execute().body?.string()
        return Jsoup.parse(postResponse!!)
    }
    
    fun getGDFlix(url: String, fetcher: DocumentFetcher) {
        // TODO:
    }
}