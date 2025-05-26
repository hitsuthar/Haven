package com.hitsuthar.june.utils.dDLProviders

import android.util.Log
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.screens.MediaContent
import com.hitsuthar.june.utils.DocumentFetcher
import com.hitsuthar.june.utils.formattedQuery
import com.hitsuthar.june.utils.rogMoviesUrl
import com.hitsuthar.junescrapper.extractors.Extractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

suspend fun rogMoviesSearch(
  query: String, year: Int?, type: String?, fetcher: DocumentFetcher
): List<String> {
  println(query)
  val searchUrl =
    if (type == "movie") "$rogMoviesUrl/?s=${formattedQuery("$query $year")}" else "$rogMoviesUrl/?s=${
      formattedQuery(query)
    }"
  return withContext(Dispatchers.IO) {
    try {
      Jsoup.parse(fetcher.fetchWithRetries(searchUrl), "UTF-8").select("div.blog-pic a.blog-img")
        .filter { element: Element ->
          element.attr("title").contains(query, true)
        }.map {
          it.attr("href")
        }
    } catch (e: Exception) {
      Log.e("HTTP_ERROR", "Failed to fetch page", e)
      emptyList()
    }


  }
}

suspend fun getRogMoviesDDL(
  title: String,
  year: Int? = null,
  type: String,
  season: Int? = null,
  episode: Int? = null,
  fetcher: DocumentFetcher
): Result<MediaContent> = withContext(Dispatchers.IO) {
  try {
    val searchResultUrls = rogMoviesSearch(title, year, type, fetcher)
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


private suspend fun fetchMovieContent(
  urls: List<String>, fetcher: DocumentFetcher
): Result<MediaContent.Movie> = withContext(Dispatchers.IO) {
  val ddlStreams = mutableListOf<DDLStream>()

  try {
    urls.map { url ->
      async {
        try {
          Jsoup.parse(fetcher.fetchWithRetries(url)).select("div.entry-content > p > a")
            .map { element ->
              async {
                val link = element.attr("href")
                Jsoup.parse(fetcher.fetchWithRetries(link)).select("p a")
                  .firstOrNull { elementA ->
                    elementA.attr("href").contains(
                      "vcloud", ignoreCase = true
                    ) && !elementA.attr("href").contains("api", ignoreCase = true)
                  }?.attr("href")?.let { nextDriveURL ->
                    val vcloud = Jsoup.parse(fetcher.fetchWithRetries(nextDriveURL))
                      .select("script:containsData(url)").html()

                    Regex("var url = '([^']*)'").find(vcloud)?.groupValues?.get(
                      1
                    )?.let { hubCloudUrl ->
                      Extractor().extractFromHUbCloud(hubCloudUrl, fetcher)?.let { stream ->
                        synchronized(ddlStreams) {
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
    urls.map { url ->
      async {
        try {
          Jsoup.parse(fetcher.fetchWithRetries(url)).select("div.entry-content > p a").filter { a ->
            a.parent()?.previousElementSibling()?.text()
              ?.contains("Season $season") == true && a.text()
              .contains("V-Cloud", ignoreCase = true) || a.text()
              .contains("G-Direct", ignoreCase = true)
          }.map { a: Element ->
            async {
              val nextDriveURL = a.attr("href")
//												println(nextDriveURL)
              val episodeH4 =
                Jsoup.parse(fetcher.fetchWithRetries(nextDriveURL)).select("h4").firstOrNull { h4 ->
                  h4.text().contains("Ep", ignoreCase = true)
                  h4.text().contains(episode.toString())
                }
              if (episodeH4 != null) {
                // The <p> we want is the next sibling element after the h4
                val downloadP = episodeH4.nextElementSibling()

                if (downloadP != null && downloadP.tagName() == "p") {
                  // Extract all href links from the <p> element
                  downloadP.select("a").firstOrNull { episodeA ->
                    episodeA.attr("href").contains(
                      "vcloud", ignoreCase = true
                    ) && !episodeA.attr("href").contains("api", ignoreCase = true)
                  }?.attr("href")?.let { vcloudURL ->
                    Regex("var url = '([^']*)'").find(
                      Jsoup.parse(fetcher.fetchWithRetries(vcloudURL))
                        .select("script:containsData(url)").html()
                    )?.groupValues?.get(1)?.let { hubCloudUrl ->
                      Extractor().extractFromHUbCloud(hubCloudUrl, fetcher)?.let { stream ->
                        synchronized(episodesMap) {
                          episodesMap.getOrPut(episode!!) { mutableListOf() }.add(stream)
                        }
                      }
                    }
                  }
                }
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