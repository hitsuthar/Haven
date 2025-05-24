package com.hitsuthar.junescrapper.extractors

import android.util.Log
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.utils.DocumentFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException

@Serializable
private data class Response(
    val url: String
)

suspend fun getDriveSeed(url: String, fetcher: DocumentFetcher): DDLStream? = withContext(Dispatchers.IO) {
    try {
        val DL = Jsoup.parse(fetcher.fetchWithRetries(url))
        if (DL.select("a.btn-warning").text().contains(" Resume Cloud ")) {
            println(DL.select("a.btn-warning").attr("href"))
        }
        val resumeBotUrl = DL.select("a.btn-light").attr("href")
        println("resumeurl $resumeBotUrl")

        val resumeBotResponse = Jsoup.connect(resumeBotUrl).execute()
        val resumeBotDoc = resumeBotResponse.parse()

        val title = resumeBotDoc.select("div.card-header").text()

        val workerSeedUrl = resumeBotUrl.substringBefore("&do=")
        val token = """formData\.append\('token',\s*'([^']+)'\)""".toRegex()
            .find(resumeBotDoc.select("script:containsData(download)").html())?.groupValues?.get(1)
        val sessionID = resumeBotResponse.header("Set-Cookie")
            ?.let { """PHPSESSID=([^;]+)""".toRegex().find(it)?.groupValues?.get(1) }

        val client = OkHttpClient()
        val requestBody = token?.let {
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("token", it)
                .build()
        }

        val postRequest = requestBody?.let {
            Request.Builder()
                .url(workerSeedUrl)
                .post(it)
                .addHeader("Cookie", "PHPSESSID=${sessionID ?: ""}")
                .build()
        }

        postRequest?.let { request ->
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val responseBody = response.body?.string()
                responseBody?.let { body ->
                    val ddlUrl = Json.decodeFromString<Response>(body).url
                    return@withContext DDLStream(title, ddlUrl)
                }
            }
        }

        return@withContext null
    } catch (e: Exception) {
        println(e)
        return@withContext null
    }
}