package com.hitsuthar.june.utils.dDLProviders

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.utils.DocumentFetcher
import com.hitsuthar.june.utils.formattedQuery
import com.hitsuthar.junescrapper.extractors.Extractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

suspend fun moviesDriveSearch(query: String, fetcher: DocumentFetcher): List<String> {
    val baseUrl = "https://moviesdrive.solutions"
    val searchUrl = "$baseUrl/?s=${formattedQuery(query)}"

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

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun getMoviesDriveDDL(
    title: String,
    year: Int? = null,
    type: String,
    season: Int? = null,
    episode: Int? = null,
    fetcher: DocumentFetcher
): Result<List<DDLStream>> = withContext(Dispatchers.IO) {
    try {
        val searchQuery = if (type == "movie") "${title.trim().replace(" ", "+")}+$year"
        else title.trim().replace(" ", "+")

        val searchResultUrls = moviesDriveSearch(searchQuery, fetcher)
        if (searchResultUrls.isEmpty()) return@withContext Result.failure(NoResultsException("No movies found for '$title'"))

        val ddlStreams = mutableListOf<DDLStream>()

        searchResultUrls.parallelStream().map { url ->
            if (type == "movie") {
                Jsoup.parse(fetcher.fetchWithRetries(url)).select("h5 a").parallelStream()
                    .map { element ->
                        val link = element.attr("href")

                        val ddlDocument = Jsoup.parse(fetcher.fetchWithRetries(link))

                        ddlStreams.add(
                            ddlDocument.select("a").firstOrNull { element ->
                                element.attr("href").contains("hubcloud", ignoreCase = true)
                            }?.attr("href")?.let { Extractor().getHubCloudUrl(it, fetcher) }!!
                        )
                    }.toList()
            } else {
                Jsoup.parse(fetcher.fetchWithRetries(url)).select("a").filter { element ->
//                        println("filter: " + element.parent()?.select("span")?.text())

                    !element.attr("href").contains("zip", true) || element.attr("href")
                        .contains("episode-$episode", true) || element.parent()?.select("span")
                        ?.text()
                        ?.contains("ep", ignoreCase = true) == true
                    !element.attr("href").contains("All-Episode", true) && !element.text()
                        .contains("zip", ignoreCase = true)

                }.parallelStream().map { element ->
                    val link = element.attr("href")
                    if (link.contains("graph", true)) {
                        if ((link.contains("episode$episode", true) || link.contains(
                                "episode-$episode",
                                true
                            )) && (link.contains("season$season", true) || link.contains(
                                "season-$season", true
                            ) || link.contains("S-$season", true) || link.contains(
                                "S-0$season", true
                            ) || link.contains("S$season", true) || link.contains(
                                "S0$season",
                                true
                            ))
                        ) {
                            println("graph: $link")
                            val hubCloud = Jsoup.parse(fetcher.fetchWithRetries(link)).select("a")
                                .firstOrNull {
                                    it.attr("href").contains("hubcloud", ignoreCase = true)
                                }?.attr("href")
                            if (hubCloud != null) {
                                val ddl = Extractor().getHubCloudUrl(hubCloud, fetcher)
                                if (ddl != null) {
                                    ddlStreams.add(ddl)
                                }
                            }
                        }
                    }
                    if (link.contains("mdrive", true)) {

                        val ddlDocument = Jsoup.parse(fetcher.fetchWithRetries(link))
                            .select("div.entry-content h5")
//                        println(ddlDocument.first()?.html())
                        if (ddlDocument.first()?.html()
                                ?.contains("Season $season") == true || ddlDocument.first()
                                ?.html()?.contains("Season-$season", true) == true
                        ) {
                            println("mdrive: $link")
                            val hubCloud = ddlDocument.firstOrNull { element ->
                                element.html()
                                    .contains(
                                        "hubcloud",
                                        ignoreCase = true
                                    ) && element.previousElementSibling()
                                    ?.text()
                                    ?.contains(
                                        "ep",
                                        ignoreCase = true
                                    ) == true && element.previousElementSibling()
                                    ?.select("span")?.first()?.text()
                                    ?.contains("$episode", ignoreCase = true) == true
                            }?.select("a")?.attr("href")
                            if (hubCloud != null) {
                                val ddl = Extractor().getHubCloudUrl(hubCloud, fetcher)
                                if (ddl != null) {
                                    ddlStreams.add(ddl)
                                }
                            }
                        }

                    }


                }.toList()
            }

        }.toList()

        Result.success(ddlStreams)
    } catch (e: Exception) {
        Log.e("HTTP_ERROR", "Failed to fetch page", e)
        Result.failure(e)
    }
}

// Custom exceptions
class NoResultsException(message: String) : Exception(message)
class NoStreamsException(message: String) : Exception(message)