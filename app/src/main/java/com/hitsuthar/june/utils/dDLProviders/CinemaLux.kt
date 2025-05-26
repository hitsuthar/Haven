package com.hitsuthar.june.utils.dDLProviders

import android.util.Log
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.screens.MediaContent
import com.hitsuthar.june.utils.DocumentFetcher
import com.hitsuthar.june.utils.cinemaLuxUrl
import com.hitsuthar.junescrapper.extractors.Extractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class Movie(
  val url: String,
  // Other fields can be added as needed
)


suspend fun cinemaLuxSearch(query: String, fetcher: DocumentFetcher): String? =
  withContext(Dispatchers.IO) {
    val formattedQuery = query.trim().replace(" ", "+")
    val searchUrl = "$cinemaLuxUrl/wp-json/dooplay/search/?keyword=$formattedQuery&nonce=13ada4c688"
    try {
      val json = fetcher.fetchWithRetries(searchUrl)
      val firstResult = Json.decodeFromString<Map<String, Movie>>(json).values.first()
      Log.d("cinemaluxsearch", firstResult.url)
      firstResult.url
    } catch (e: Exception) {
      e.message?.let { Log.e("cinemaluxsearch", it) }
      null
    }
  }

suspend fun getCinemaLuxDDL(
  title: String,
  year: Int? = null,
  type: String,
  season: Int? = null,
  episode: Int? = null,
  fetcher: DocumentFetcher
): Result<MediaContent> = withContext(Dispatchers.IO) {

  try {
    val searchResultUrls = cinemaLuxSearch(
      if (type == "movie") "${title.trim().replace(" ", "+")}+$year"
      else title.trim().replace(" ", "+"), fetcher
    )
    if (searchResultUrls.isNullOrBlank()) {
      return@withContext Result.failure(NoResultsException("No movies found for '$title'"))
    }
    return@withContext when (type) {
      "movie" -> fetchMovieContent(listOf(searchResultUrls), fetcher)
      else -> fetchTvContent(listOf(searchResultUrls), season, episode, fetcher)
    }

  } catch (e: Exception) {
    Log.e("HTTP_ERROR", "Failed to fetch page", e)
    Result.failure(e)
  }
}


private suspend fun fetchMovieContent(
  urls: List<String>, fetcher: DocumentFetcher
): Result<MediaContent.Movie> = withContext(Dispatchers.IO) {
  val ddlStreams = mutableListOf<DDLStream>()

  try {
    urls.map { url ->
      async {
        val document: Document = Jsoup.parse(fetcher.fetchWithRetries(url))
        document.select("div.wp-content div.ep-button-container a").map { element ->
          async {
            val link = element.attr("href")
            Log.d("CinemaLux", "Link: $link")
            if (link.contains("drive") && link.contains("linkstore")) {
              Jsoup.parse(fetcher.fetchWithRetries(link)).let { extractedHDMovies ->
                val luxDrive =
                  extractedHDMovies.select("meta[http-equiv=refresh]").first()?.attr("content")
                    ?.split("url=", ignoreCase = true)?.getOrNull(1)
                if (luxDrive != null) {
                  Extractor().getLuxeDrive(luxDrive, fetcher)?.let { stream ->
                    Log.d("Linkstore", "luxdriveDDL: $stream")
                    synchronized(ddlStreams) {  // Thread-safe addition
                      ddlStreams.add(stream)
                    }
                  }
                }
              }
            } else if (link.contains("hdmovie", ignoreCase = true)) {
              Extractor().getHDMovie(link, fetcher)?.let { hdMoviesExtraced ->
                Log.d(
                  "hdMoviesExtraced", "hdMoviesExtraced:" + hdMoviesExtraced.request.url.toString()
                )
                if (hdMoviesExtraced.request.url.toString().contains("drive")) {
                  Jsoup.parse(hdMoviesExtraced.body.string()).let { extractedHDMovies ->
                    val luxDrive =
                      extractedHDMovies.select("meta[http-equiv=refresh]").first()?.attr("content")
                        ?.split("url=", ignoreCase = true)?.getOrNull(1)
                    if (luxDrive != null) {
                      Extractor().getLuxeDrive(luxDrive, fetcher)?.let { stream ->
                        Log.d("Linkstore", "luxdriveDDL: $stream")
                        synchronized(ddlStreams) {  // Thread-safe addition
                          ddlStreams.add(stream)
                        }
                      }
                    }
                  }
                } else {
                  Jsoup.parse(hdMoviesExtraced.body.string()).select("div.main-content a")
                    .attr("href").let {
                      Jsoup.parse(fetcher.fetchWithRetries(it)).let { linkStore ->
                        val luxDrive =
                          linkStore.select("meta[http-equiv=refresh]").first()?.attr("content")
                            ?.split("url=", ignoreCase = true)?.getOrNull(1)
                        Log.d("Linkstore", "luxdrive: $luxDrive")
                        if (luxDrive != null) {
                          Extractor().getLuxeDrive(luxDrive, fetcher)?.let { stream ->
                            Log.d("Linkstore", "luxdriveDDL: $stream")
                            synchronized(ddlStreams) {  // Thread-safe addition
                              ddlStreams.add(stream)
                            }
                          }
                        }
                      }
                    }
                }
              }
            }
          }
        }.awaitAll()
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
    urls.map { url ->
      async {
        val document: Document = Jsoup.parse(fetcher.fetchWithRetries(url))
        document.select("div.wp-content div").filter { element ->
          element.select("h3").text().contains("Season $season", true) || element.select("a span")
            .text().contains("Season $season", ignoreCase = true)

        }.map { element ->
          Log.d("CinemaLux", "element: $element")
          async {
            val link = element.select("a").attr("href")
            Log.d("CinemaLux", "A element link $link")
            val extractedHDMovie = Extractor().getHDMovie(link, fetcher)?.body?.string()?.let {
              Jsoup.parse(it).select("div a").firstOrNull { aElement: Element ->
                val filteredEpisode = aElement.text().replace(Regex("""\s*\(.*?\)\s*"""), "")
                filteredEpisode.contains(
                  "Ep", ignoreCase = true
                ) && filteredEpisode.contains("$episode")
              }?.attr("href")
            }
            println("extracted $extractedHDMovie")
            if (extractedHDMovie?.contains("hubcloud", ignoreCase = true) == true) {
              Extractor().getHubCloudUrl(extractedHDMovie, fetcher)?.let { stream ->
                synchronized(episodesMap) {
                  episodesMap.getOrPut(episode!!) { mutableListOf() }.add(stream)
                }
              }
            } else null
          }
        }.awaitAll()
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