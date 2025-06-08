package com.hitsuthar.june.utils.dDLProviders

import android.util.Log
import com.hitsuthar.june.moviesDriveUrl
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

suspend fun moviesDriveSearch(query: String, fetcher: DocumentFetcher): List<String> {
  val searchUrl = "$moviesDriveUrl/?s=${formattedQuery(query)}"

  return withContext(Dispatchers.IO) {
    try {
      val document = Jsoup.parse(fetcher.fetchWithRetries(searchUrl))
      document.select("ul.recent-movies > li figcaption a").map { it.attr("href") }
        .ifEmpty { emptyList() }
    } catch (e: Exception) {
      Log.e("HTTP_ERROR", "Failed to fetch page", e)
      emptyList()
    }
  }
}

suspend fun getMoviesDriveDDL(
  title: String,
  year: Int? = null,
  type: String,
  season: Int? = null,
  episode: Int? = null,
  fetcher: DocumentFetcher
): Result<MediaContent> = withContext(Dispatchers.IO) {

  try {
    val searchResultUrls = moviesDriveSearch(
      if (type == "movie") "$title $year"
      else title, fetcher
    )
    if (searchResultUrls.isEmpty()) return@withContext Result.failure(NoResultsException("No movies found for '$title'"))
    return@withContext when (type) {
      "movie" -> fetchMovieContent(searchResultUrls, fetcher)
      else -> fetchTvContent(searchResultUrls, season, episode, fetcher)
    }
  } catch (e: Exception) {
    println(e)
    Result.failure(e)
  }
}

// Custom exceptions
class NoResultsException(message: String) : Exception(message)
class NoStreamsException(message: String) : Exception(message)


private suspend fun fetchMovieContent(
  urls: List<String>, fetcher: DocumentFetcher
): Result<MediaContent.Movie> = withContext(Dispatchers.IO) {
  val ddlStreams = mutableListOf<DDLStream>()
  try {
    urls.map { url ->
      async {
        try {
          Jsoup.parse(fetcher.fetchWithRetries(url)).select("h5 a").map { element ->
            async {
              val link = element.attr("href")

              val ddlDocument = Jsoup.parse(fetcher.fetchWithRetries(link))

              ddlStreams.add(
                ddlDocument.select("a").firstOrNull { aElement ->
                aElement.attr("href").contains("hubcloud", ignoreCase = true)
              }?.attr("href")?.let { Extractor().getHubCloudUrl(it, fetcher) }!!
              )
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
    urls.map { url ->
      async {
        try {
          Jsoup.parse(fetcher.fetchWithRetries(url)).select("a").filter { element ->
            !element.attr("href").contains("zip", true) || element.attr("href")
              .contains("episode-$episode", true) || element.parent()?.select("span")?.text()
              ?.contains("ep", ignoreCase = true) == true
            !element.attr("href").contains("All-Episode", true) && !element.text()
              .contains("zip", ignoreCase = true)

          }.map { element ->
            async {
              try {
                val link = element.attr("href")
                if (link.contains("graph", true)) {
                  if ((link.contains("episode$episode", true) || link.contains(
                      "episode-$episode", true
                    ))
                  ) {
                    println("graph: $link")
                    Jsoup.parse(fetcher.fetchWithRetries(link)).select("a").firstOrNull {
                      it.attr("href").contains("hubcloud", ignoreCase = true)
                    }?.attr("href")?.let {
                      Extractor().getHubCloudUrl(it, fetcher)?.let { stream ->
                        synchronized(episodesMap) {
                          episodesMap.getOrPut(episode!!) { mutableListOf() }.add(stream)
                        }
                      }
                    }
                  } else {
                  }
                } else if (link.contains("mdrive", true)) {

                  val ddlDocument =
                    Jsoup.parse(fetcher.fetchWithRetries(link)).select("div.entry-content h5")
                  if (ddlDocument.first()?.html()
                      ?.contains("Season $season") == true || ddlDocument.first()?.html()
                      ?.contains("Season-$season", true) == true
                  ) {
                    val episodeSections = ddlDocument.select("h5").filter { element ->
                      element.text().contains("Ep", true)
                    }
                    episodeSections.map { episodeElement ->
                      val episodeText = episodeElement.text()

                      // Check if this is the episode we want
                      if (episodeText.startsWith("Ep0$episode")) {
                        val size = episodeText.substringAfter("[").substringBefore("]")
                        println("Ep0$episode - $size")

                        // Get the next 3 links (assuming each episode has 3 download options)
                        episodeElement.nextElementSiblings().takeWhile { it.tagName() == "h5" }
                          .firstOrNull { link ->
                            link.select("a").attr("href").contains("hubcloud", true) && link.select(
                              "a"
                            ).attr("href").contains("drive", true)
                          }?.let { link ->

                            Extractor().getHubCloudUrl(link.select("a").attr("href"), fetcher)
                              ?.let { stream ->
                                synchronized(episodesMap) {
                                  episodesMap.getOrPut(episode!!) { mutableListOf() }.add(stream)
                                }
                              }
                          }
                      }
                    }
                  } else {
                  }
                } else {
                }
              } catch (e: Exception) {
                Log.e("TvContent", "Error processing episode link", e)
              }
            }
          }.awaitAll()
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