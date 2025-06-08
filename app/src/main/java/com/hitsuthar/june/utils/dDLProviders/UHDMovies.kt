package com.hitsuthar.june.utils.dDLProviders

import android.util.Log
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.screens.MediaContent
import com.hitsuthar.june.uHDMoviesUrl
import com.hitsuthar.june.utils.DocumentFetcher
import com.hitsuthar.june.utils.extractors.getDriveLeech
import com.hitsuthar.june.utils.extractors.getUnblockedGames
import com.hitsuthar.june.utils.formattedQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

suspend fun uHDMoviesSearch(query: String, fetcher: DocumentFetcher): List<String> {
  val searchUrl = "$uHDMoviesUrl/search/${formattedQuery(query)}"
  return withContext(Dispatchers.IO) {
    try {
      val document = Jsoup.parse(fetcher.fetchWithRetries(searchUrl))
      document.select("article.gridlove-post div.entry-image a").map {
        it.attr("href")
      }.ifEmpty { emptyList() }
    } catch (e: Exception) {
      emptyList()
    }
  }
}

suspend fun getUHDMoviesDDL(
  title: String,
  year: Int? = null,
  type: String,
  season: Int? = null,
  episode: Int? = null,
  fetcher: DocumentFetcher
): Result<MediaContent> = withContext(Dispatchers.IO) {
  val searchResultUrls = uHDMoviesSearch(
    if (type == "movie") "$title $year"
    else title, fetcher
  )
  if (searchResultUrls.isEmpty()) return@withContext Result.failure(NoResultsException("No movies found for '$title'"))

  return@withContext when (type) {
    "movie" -> fetchMovieContent(searchResultUrls, fetcher)
    else -> fetchTvContent(searchResultUrls, season, episode, fetcher)
  }
}

private suspend fun fetchMovieContent(
  urls: List<String>, fetcher: DocumentFetcher
): Result<MediaContent.Movie> = withContext(Dispatchers.IO) {
  val ddlStreams = mutableListOf<DDLStream>()
  try {
    urls.map { url ->
      async {
        try {
          val document = Jsoup.parse(fetcher.fetchWithRetries(url))
          document.select("div.entry-content > p a.maxbutton-1").map { element ->
            val link = element.attr("href")
            val ubGameLink = getUnblockedGames(link, fetcher)
            if (ubGameLink != null) {
              if (ubGameLink.contains("driveleech")) {
                getDriveLeech(ubGameLink, fetcher)?.let { stream ->
                  synchronized(ddlStreams) {  // Thread-safe addition
                    Log.d("UHDMovies", "stream: $stream")
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


private suspend fun fetchTvContent(
  urls: List<String>, season: Int?, episode: Int?, fetcher: DocumentFetcher
): Result<MediaContent.TvSeries> = withContext(Dispatchers.IO) {
  val episodesMap = mutableMapOf<Int, MutableList<DDLStream>>()
  try {
    urls.map { url ->
      async {
        try {
          val document = Jsoup.parse(fetcher.fetchWithRetries(url))
          val episodesLink = emptyList<String>().toMutableList()
          document.select("div.entry-content h2").filter { element ->
            element.text().contains("Season") && element.text()
              .contains(season.toString()) && !element.text().contains("-")
          }.map { element ->

            var currentElement = element.nextElementSibling()
            while (currentElement != null) {
              // Stop if we reach next season or separator
              if (currentElement.tagName() == "h2" || (currentElement.hasClass("mks_separator") && currentElement != element.nextElementSibling())) {
                break
              }
              currentElement.select("a.maxbutton-gdrive-episode").filter { allButtons ->
                allButtons.text().contains("Ep", ignoreCase = true) && allButtons.text()
                  .contains("$episode")
              }.map { episodeLink -> episodesLink.add(episodeLink.attr("href")) }
              currentElement = currentElement.nextElementSibling()
            }
          }
          episodesLink.map { link ->
            val ubGameLink = getUnblockedGames(link, fetcher)
            if (ubGameLink != null) {
              if (ubGameLink.contains("driveleech")) {
                getDriveLeech(ubGameLink, fetcher)?.let { stream ->
                  synchronized(episodesMap) {
                    episodesMap.getOrPut(episode!!) { mutableListOf() }.add(stream)
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

