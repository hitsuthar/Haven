package com.hitsuthar.june.utils.dDLProviders

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.screens.MediaContent
import com.hitsuthar.june.utils.DocumentFetcher
import com.hitsuthar.june.utils.extractors.getUnblockedGames
import com.hitsuthar.june.utils.formattedQuery
import com.hitsuthar.june.utils.moviesModUrl
import com.hitsuthar.junescrapper.extractors.getDriveSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

suspend fun moviesModSearch(query: String, fetcher: DocumentFetcher): List<String> {
  val searchUrl = "$moviesModUrl/search/${formattedQuery(query)}"
  return withContext(Dispatchers.IO) {
    try {
      val document = Jsoup.parse(fetcher.fetchWithRetries(searchUrl))
      document.select("div.post-cards article > a").map {
        it.attr("href")
      }.ifEmpty { emptyList() }
    } catch (e: Exception) {
      Log.e("HTTP_ERROR", "Failed to fetch page", e)
      emptyList()
    }
  }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun getMoviesModDDL(
  title: String,
  year: Int? = null,
  type: String,
  season: Int? = null,
  episode: Int? = null,
  fetcher: DocumentFetcher
): Result<MediaContent> = withContext(Dispatchers.IO) {
  try {
    val searchResultUrls = moviesModSearch(
      if (type == "movie") "${title.trim().replace(" ", "+")}+$year"
      else title.trim().replace(" ", "+"), fetcher
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


@OptIn(ExperimentalEncodingApi::class)
private suspend fun fetchMovieContent(
  urls: List<String>, fetcher: DocumentFetcher
): Result<MediaContent.Movie> = withContext(Dispatchers.IO) {
  val ddlStreams = mutableListOf<DDLStream>()

  try {
    urls.map { url ->
      async {
        try {
          Jsoup.parse(fetcher.fetchWithRetries(url)).select("p a.maxbutton-1").forEach { element ->
            val link =
              """[?&]url=([^&]+)""".toRegex().find(element.attr("href"))?.groupValues?.get(1)
                ?.let { Base64.Default.decode(it).decodeToString() }

            link?.let {
              val ddlDocument = Jsoup.parse(fetcher.fetchWithRetries(it))
              val ubGamesLink = ddlDocument.select("a.maxbutton-fast-server-gdrive").attr("href")
              val newUrl = getUnblockedGames(ubGamesLink, fetcher)

              if (newUrl != null && newUrl.contains("driveseed")) {
                getDriveSeed(newUrl, fetcher)?.let { stream ->
                  synchronized(ddlStreams) {
                    ddlStreams.add(stream)
                  }
                }
              }
            }
          }
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

@OptIn(ExperimentalEncodingApi::class)
private suspend fun fetchTvContent(
  urls: List<String>, season: Int?, episode: Int?, fetcher: DocumentFetcher
): Result<MediaContent.TvSeries> = withContext(Dispatchers.IO) {

  val episodesMap = mutableMapOf<Int, MutableList<DDLStream>>() // episode number to streams

  try {
    urls.map { url ->
      async {
        try {
          Jsoup.parse(fetcher.fetchWithRetries(url)).select("div p a").filter { element ->
            val seasonMatch =
              element.parent()?.previousElementSibling()?.select("h3")?.text()?.let { text ->
                listOf(
                  "Season $season", "Season$season", "Season 0$season", "Season0$season"
                ).any { pattern -> text.contains(pattern) }
              } ?: false

            seasonMatch && element.text().contains("link", ignoreCase = true)
          }.forEach { element ->
            try {
              val link =
                """[?&]url=([^&]+)""".toRegex().find(element.attr("href"))?.groupValues?.get(1)
                  ?.let { Base64.Default.decode(it).decodeToString() }

              link?.let {
                val episodeLink = Jsoup.parse(fetcher.fetchWithRetries(link)).select("h3 a")
                  .firstOrNull { it.text().contains("Episode $episode") }?.attr("href")

                episodeLink?.let { gameLink ->
                  val driveSeedLink = getUnblockedGames(gameLink, fetcher)
                  if (driveSeedLink != null && driveSeedLink.contains("driveseed", true)) {
                    getDriveSeed(driveSeedLink, fetcher)?.let { stream ->
                      synchronized(episodesMap) {
                        episodesMap.getOrPut(episode!!) { mutableListOf() }.add(stream)
                      }
                    }
                  }
                }
              }
            } catch (e: Exception) {
              Log.e("TvContent", "Error processing episode link", e)
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