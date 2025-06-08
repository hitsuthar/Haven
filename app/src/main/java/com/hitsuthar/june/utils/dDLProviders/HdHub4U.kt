package com.hitsuthar.june.utils.dDLProviders

import android.util.Log
import com.hitsuthar.june.hdHub4UUrl
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.screens.MediaContent
import com.hitsuthar.june.utils.DocumentFetcher
import com.hitsuthar.june.utils.extractors.Extractor
import com.hitsuthar.june.utils.formattedQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup


suspend fun hdHub4USearch(query: String, fetcher: DocumentFetcher): List<String> {
  val searchUrl = "$hdHub4UUrl/?s=${formattedQuery(query)}"
  Log.d("hdhub4u", "searchurl: $searchUrl")
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
): Result<MediaContent> = withContext(Dispatchers.IO) {
  try {
    val searchResultUrls = hdHub4USearch(
      if (type == "movie") "$title $year"
      else title, fetcher
    )
    if (searchResultUrls.isEmpty()) {
      Log.d("hdhub4u", "no result found")
      return@withContext Result.failure(NoResultsException("No movies found for '$title'"))
    }

    return@withContext when (type) {
      "movie" -> fetchMovieContent(searchResultUrls, fetcher)
      else -> fetchTvContent(searchResultUrls, season, episode, fetcher)
    }
  } catch (e: Exception) {
    println(e)
    Result.failure(e)
  }
}


private suspend fun fetchMovieContent(
  urls: List<String>, fetcher: DocumentFetcher
): Result<MediaContent.Movie> = withContext(Dispatchers.IO) {
  val ddlStreams = mutableListOf<DDLStream>()

  try {
    urls.map { resultUrl ->
      async {
        try {
          Jsoup.parse(fetcher.fetchWithRetries(resultUrl))
            .select("main h3 a[href], main h4 a[href]").map { link ->
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
                          Jsoup.parse(fetcher.fetchWithRetries(aHbLink.attr("href")))
                            .select("div a[href]").firstOrNull { aElemet ->
                              aElemet.attr("href").contains("hubcloud", true)
                            }?.attr("href")?.let { hubCloudUrl ->
                              Extractor().getHubCloudUrl(hubCloudUrl, fetcher)?.let { hubDriveUrl ->
                                ddlStreams.add(hubDriveUrl)
                              }
                            }
                        }
                      }
                  }
                } else if (url.contains("hubdrive", true)) {
                  val hubDrive = Jsoup.parse(fetcher.fetchWithRetries(url)).select("div a[href]")
                    .firstOrNull { aElemet ->
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
        } catch (e: Exception) {
          Log.e("MovieContent", "Error processing movie URL", e)
        }
      }
    }.awaitAll()

    if (ddlStreams.isNotEmpty()) {
      Result.success(MediaContent.Movie(ddlStreams))
    } else {
      Result.failure(NoResultsException("No streams found for movie"))
    }
  } catch (e: Exception) {
    Result.failure(e)
  }
}


private suspend fun fetchTvContent(
  urls: List<String>, season: Int?, episode: Int?, fetcher: DocumentFetcher
): Result<MediaContent.TvSeries> = withContext(Dispatchers.IO) {

  val episodesMap = mutableMapOf<Int, MutableList<DDLStream>>() // episode number to streams

  try {
    urls.map { resultUrl ->
      async {
        try {
          val doc = Jsoup.parse(fetcher.fetchWithRetries(resultUrl))
          // Select the parent div that contains all single episode links
          val singleEpisodeSection = doc.select("div:has(h2)")

          singleEpisodeSection.select("h4:has(span)").forEach { episodeHeader ->
            val episodeText = episodeHeader.text()

            val regex = """EPiSODE\s*(\d+)""".toRegex(RegexOption.IGNORE_CASE)
            val episodeNumber = regex.find(episodeText)?.groupValues?.get(1)?.toInt() ?: -1

            if (episodeNumber == episode) {
              // Process sibling elements until next episode section
              episodeHeader.nextElementSiblings().takeWhile { it.tagName() == "h4" }.map { h4 ->
                h4.select("a").map { a ->
                  Log.d("hdhub4u", "hubdrive: ${a.attr("href")}")
                  a.attr("href").let { aHref ->
                    if (aHref.contains("hubdrive")) {
                      val hubDrive =
                        Jsoup.parse(fetcher.fetchWithRetries(aHref)).select("div a[href]")
                          .firstOrNull { aElemet ->
                            aElemet.attr("href").contains("hubcloud", true)
                          }?.attr("href")?.let { hubDriveUrl ->
                            Extractor().getHubCloudUrl(hubDriveUrl, fetcher)?.let { stream ->
                              synchronized(episodesMap) {
                                episodesMap.getOrPut(episode) { mutableListOf() }.add(stream)
                              }
                            }
                          }
                    }
                  }
                }
              }
            }
          }


        } catch (e: Exception) {
          Log.e("TvContent", "Error processing TV URL", e)
        }
      }
    }.awaitAll()

    val episodes = episodesMap.map { (episodeNum, streams) ->
      MediaContent.TvSeries.Episode(
        number = episodeNum, streams = streams
      )
    }

    if (episodes.isNotEmpty()) {
      Result.success(
        MediaContent.TvSeries(
          seasons = listOf(
            MediaContent.TvSeries.Season(
              number = season!!, episodes = episodes
            )
          )
        )
      )
    } else {
      Result.failure(NoResultsException("No streams found for season $season episode $episode"))
    }
  } catch (e: Exception) {
    Result.failure(e)
  }
}