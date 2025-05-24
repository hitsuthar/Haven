package com.hitsuthar.june.utils.dDLProviders

import android.util.Log
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.utils.DocumentFetcher
import com.hitsuthar.june.utils.formattedQuery
import com.hitsuthar.june.utils.hdHub4UUrl
import com.hitsuthar.junescrapper.extractors.Extractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup


suspend fun hdHub4USearch(query: String, fetcher: DocumentFetcher): List<String> {
  val searchUrl = "$hdHub4UUrl/?s=${formattedQuery(query)}"
  return withContext(Dispatchers.IO) {
    try {
      val document = Jsoup.parse(fetcher.fetchWithRetries(searchUrl))
      document.select("section ul > li figure a").map {
        it.attr("href")
      }.ifEmpty { emptyList() }
    } catch (e: Exception) {
      Log.e("HTTP_ERROR", "Failed to fetch page", e)
      emptyList()
    }
  }
}

suspend fun getHdHub4UDDL(
  title: String,
  year: Int? = null,
  type: String,
  season: Int? = null,
  episode: Int? = null,
  fetcher: DocumentFetcher
): Result<List<DDLStream>> = withContext(Dispatchers.IO) {
  try {
    val searchResultUrls = hdHub4USearch(
      if (type == "movie") "${title.trim().replace(" ", "+")}+$year"
      else title.trim().replace(" ", "+"), fetcher
    )
    if (searchResultUrls.isEmpty()) return@withContext Result.failure(NoResultsException("No movies found for '$title'"))
    val ddlStreams = mutableListOf<DDLStream>()

    try {
      if (type == "movie") {
        searchResultUrls.map { result ->
          Jsoup.parse(fetcher.fetchWithRetries(result)).select("main h3 a[href], main h4 a[href]")
            .map { link ->
              async {
                val quality: String = link.text().trim()
                val url: String = link.attr("href")
                Log.d("HDHUB4U", "$quality → $url")
                if (url.contains("techyboy", true)) {
                  val extractedTechyBoyUrl = Extractor().getTechyBoy(url, fetcher)
                  if (extractedTechyBoyUrl != null && extractedTechyBoyUrl.contains("hblink")) {
                    Jsoup.parse(fetcher.fetchWithRetries(extractedTechyBoyUrl))
                      .select("div.entry-content a").map { aHbLink ->
                        if (aHbLink.attr("href").contains("hubdrive", true)) {
                          val hubCloud = Jsoup.parse(fetcher.fetchWithRetries(aHbLink.attr("href")))
                            .select("div a[href]").firstOrNull { aElemet ->
                              aElemet.attr("href").contains("hubcloud", true)
                            }?.attr("href")
                          if (hubCloud != null) {
                            val hubDrive = Extractor().getHubCloudUrl(hubCloud, fetcher)
                            if (hubDrive != null) {
                              ddlStreams.add(hubDrive)
                            }
                          }
                        }
                      }
                  }
                } else if (url.contains("hubdrive", true)) {
                  val hubDrive = Jsoup.parse(fetcher.fetchWithRetries(url))
                    .select("div a[href]").firstOrNull { aElemet ->
                      aElemet.attr("href").contains("hubcloud", true)
                    }?.attr("href")
                  if (hubDrive != null) {
                    Extractor().getHubCloudUrl(hubDrive, fetcher)?.let { stream ->
                      synchronized(ddlStreams) {  // Thread-safe addition
                        ddlStreams.add(stream)
                      }
                    }
                  }
                }
              }
            }.awaitAll()
        }
      } else if (type == "tv") {

      }


    } catch (e: Exception) {
      Log.e("HDHUB4U", e.message.toString())
      Result.success(ddlStreams)
    }
    Result.success(ddlStreams)

  } catch (e: Exception) {
    Log.e("HDHUB4U", e.message.toString())
    Result.failure(e)
  }
}
