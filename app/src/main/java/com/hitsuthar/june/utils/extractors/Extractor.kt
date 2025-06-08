package com.hitsuthar.june.utils.extractors

import android.util.Base64
import android.util.Log
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.utils.DocumentFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
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
    url: String, fetcher: DocumentFetcher
  ): DDLStream? {
    return try {
      val doc = Jsoup.parse(fetcher.fetchWithRetries(url))

      val link = if (url.contains("drive") || url.contains(
          "hubcloud", ignoreCase = true
        ) && !url.contains("video", ignoreCase = true)
      ) {
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

  suspend fun getHDMovie(url: String, fetcher: DocumentFetcher): Response? =
    withContext(Dispatchers.IO) {
      try {
        val client = OkHttpClient.Builder().build()

        val jsCode = Jsoup.parse(fetcher.fetchWithRetries(url)).select("script").html()
        val postUrl =
          """"soralink_ajaxurl":"([^"]+)"""".toRegex().find(jsCode)?.groupValues?.get(1)
        val jsonData =
          """var\s+item\s*=\s*(\{.*?\});""".toRegex(RegexOption.DOT_MATCHES_ALL)
            .find(jsCode)?.groupValues?.get(1)
        val item = Json.decodeFromString<JsonItem>(jsonData!!)
        val soraLink =
          """"soralink_z"\s*:\s*"([^"]+)"""".toRegex().find(jsCode)?.groupValues?.get(1)

        Log.d("getHDMovie", "SoraLink: $soraLink")
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
        val postResponse = client.newCall(postRequest).execute()
        Log.d("getHDMovie", "postResponse: $postResponse")
        postResponse
      } catch (e: Exception) {
        Log.e("getHDMovies", e.toString())
        null
      }
    }

  suspend fun getLinkStore(url: String, fetcher: DocumentFetcher): DDLStream? =
    withContext(Dispatchers.IO) {
      Log.d("getLinkStore", "url: $url")
      null
    }

  suspend fun getLuxeDrive(url: String, fetcher: DocumentFetcher): DDLStream? =
    withContext(Dispatchers.IO) {
      Log.d("getLuxeDrive", "url: $url")
      try {
        val document = Jsoup.parse(fetcher.fetchWithRetries(url))
        getGDFlix(
          document.select("div.mirror-buttons").select("a.gdflix").attr("href"), fetcher
        )
      } catch (e: Exception) {
        Log.e("getLuxeDrive", e.message.toString())
        null
      }

    }

  suspend fun getGDFlix(url: String, fetcher: DocumentFetcher): DDLStream? =
    withContext(Dispatchers.IO) {
      Log.d("getGDFlix", "url: $url")
      try {
        val document = Jsoup.parse(fetcher.fetchWithRetries(url))

        val name = document.select("li.list-group-item:contains(Name :)").first()?.text()
          ?.substringAfter("Name :")?.trim() ?: ""
        val size = document.select("li.list-group-item:contains(Size :)").first()?.text()
          ?.substringAfter("Size :")?.trim() ?: ""


        val pixelDrainUrl = document.select("div.text-center").select("a").filter { a ->
          a.attr("href").contains("pixeldrain", true)
        }.first().attr("href")


        DDLStream(name, pixelDrainUrl, size)
      } catch (e: Exception) {
        e.message?.let { Log.e("getGDFlix", it) }
        null
      }
    }

  private fun rot13(str: String): String {
    return str.replace(Regex("[a-zA-Z]")) { matchResult ->
      val char = matchResult.value[0]
      val isUpperCase = char <= 'Z'
      val baseCharCode = if (isUpperCase) 'A'.code else 'a'.code
      val charCode = char.code
      val rotatedCharCode = ((charCode - baseCharCode + 13) % 26) + baseCharCode
      rotatedCharCode.toChar().toString()
    }
  }

  fun decodeTechyboyString(encryptedString: String): JSONObject? {
    return try {
      // First base64 decode
      var decoded =
        String(android.util.Base64.decode(encryptedString, android.util.Base64.DEFAULT))

      // Second base64 decode
      decoded = String(android.util.Base64.decode(decoded, android.util.Base64.DEFAULT))

      // ROT13 decode
      decoded = rot13(decoded)

      // Third base64 decode
      decoded = String(android.util.Base64.decode(decoded, android.util.Base64.DEFAULT))

      // Parse JSON
      JSONObject(decoded) // or JSONArray if your decoded string is an array
    } catch (error: Exception) {
      println("Error decoding string: $error")
      null
    }
  }

  suspend fun getTechyBoy(url: String, fetcher: DocumentFetcher): String? =
    withContext(Dispatchers.IO) {
      val document = Jsoup.parse(fetcher.fetchWithRetries(url))
      val script = document.select("script").first()?.html()
      val regex = """s\(['"].*?['"],\s*['"](.*?)['"],.*?\)""".toRegex()
      val matchResult = script?.let { regex.find(it) }
      if (matchResult != null) {
        val encryptedString = matchResult.groupValues[1]
        // Parse JSON (using Kotlinx Serialization for real projects)
        val oValue = decodeTechyboyString(encryptedString).toString().trimIndent()
          .substringAfter("\"o\":\"").substringBefore("\"}")
        val decodedString = String(Base64.decode(oValue, Base64.DEFAULT))
        decodedString
      } else {
        null
      }
    }

  suspend fun getGoFile(url: String, fetcher: DocumentFetcher): DDLStream? =
    withContext(Dispatchers.IO) {
      try {
        val pattern = """https?://gofile\.io/d/([a-zA-Z0-9]+)""".toRegex()
        val id = pattern.find(url)?.groupValues?.get(1)

        val response =
          fetcher.fetchWithRetries("https://api.gofile.io/contents/$id?wt=4fd6sg89d7s6&contentFilter=&page=1&pageSize=1000&sortField=name&sortDirection=1")
        Log.d("getGoFile", "response: $response")
        null
      } catch (e: Exception) {
        Log.e("getGoFile", e.message.toString())
        null
      }
    }

}