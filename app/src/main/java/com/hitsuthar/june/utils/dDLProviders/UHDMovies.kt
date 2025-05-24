package com.hitsuthar.june.utils.dDLProviders

import android.util.Log
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.utils.DocumentFetcher
import com.hitsuthar.june.utils.extractors.getDriveLeech
import com.hitsuthar.june.utils.extractors.getUnblockedGames
import com.hitsuthar.june.utils.formattedQuery
import com.hitsuthar.june.utils.uHDMoviesUrl
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
): Result<List<DDLStream>> = withContext(Dispatchers.IO) {

    val searchResultUrls = uHDMoviesSearch(
        if (type == "movie") "${title.trim().replace(" ", "+")}+$year"
        else title.trim().replace(" ", "+"), fetcher
    )

    if (searchResultUrls.isEmpty()) return@withContext Result.failure(NoResultsException("No movies found for '$title'"))
    val ddlStreams = mutableListOf<DDLStream>()

    searchResultUrls.map {
        val document = Jsoup.parse(fetcher.fetchWithRetries(it))

        if (type == "movie") {
            document.select("div.entry-content > p a.maxbutton-1").map { element ->
                async {
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
            }.awaitAll()
        } else if (type == "tv") {
            val episodesLink: MutableList<String> = emptyList<String>().toMutableList()
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

                    // Collect episode links
                    currentElement.select("a.maxbutton-gdrive-episode").filter { allButtons ->
                        allButtons.text().contains("Ep", ignoreCase = true) && allButtons.text()
                            .contains("$episode")
                    }.map { episodeLink ->
                        episodesLink.add(episodeLink.attr("href"))
                    }
//                        .parallelStream().map { button: Element ->
//                            val link = button.attr("href")
//                            val ubGameLink = getUnblockedGames(link, fetcher)
//                            if (ubGameLink.contains("driveleech")) {
//                                ddlStreams.add(getDriveLeech(ubGameLink, fetcher))
//                            }
//                        }.toList()

                    currentElement = currentElement.nextElementSibling()
                }
            }
            episodesLink.map { link ->
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
        } else {
            return@withContext Result.failure(NoStreamsException("No movies found for '$title'"))
        }
    }
    println("UHDMovies:$ddlStreams")
    Result.success(ddlStreams)
}