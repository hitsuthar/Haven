package com.hitsuthar.june.utils.dDLProviders

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.hitsuthar.june.screens.DDLStream
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
@OptIn(ExperimentalEncodingApi::class)
suspend fun getMoviesModDDL(
    title: String,
    year: Int? = null,
    type: String,
    season: Int? = null,
    episode: Int? = null,
    fetcher: DocumentFetcher
    ): Result<List<DDLStream>> = withContext(Dispatchers.IO) {
    try {
        val searchResultUrls = moviesModSearch(
            if (type == "movie") "${title.trim().replace(" ", "+")}+$year"
            else title.trim().replace(" ", "+"), fetcher
        )
        if (searchResultUrls.isEmpty()) return@withContext Result.failure(NoResultsException("No movies found for '$title'"))
        val ddlStreams: MutableList<DDLStream> = emptyList<DDLStream>().toMutableList()

        try {
            searchResultUrls.map {
                if (type == "movie") {
                    Jsoup.parse(fetcher.fetchWithRetries(it)).select("p a.maxbutton-1")
                        .map { element ->
                            async {
                                try {
                                    val link = """[?&]url=([^&]+)""".toRegex()
                                        .find(element.attr("href"))?.groupValues?.get(1)
                                        ?.let { Base64.Default.decode(it).decodeToString() }
                                    val ddlDocument = Jsoup.parse(fetcher.fetchWithRetries(link!!))
                                    val ubGamesLink =
                                        ddlDocument.select("a.maxbutton-fast-server-gdrive")
                                            .attr("href")
                                    val newUrl = getUnblockedGames(ubGamesLink, fetcher)
                                    if (newUrl != null && newUrl.contains("driveseed")) {
                                        getDriveSeed(newUrl, fetcher)?.let { stream ->
                                            synchronized(ddlStreams) {  // Thread-safe addition
                                                ddlStreams.add(stream)
                                            }
                                        }
                                    } else {
                                    }
                                } catch (e: Exception) {
                                    Log.e("ParallelProcessing", "Error processing element", e)
                                }
                            }
                        }.awaitAll()
                } else {
                    Jsoup.parse(fetcher.fetchWithRetries(it)).select("div p a").filter { element ->
                        (element.parent()?.previousElementSibling()?.select("h3")?.text()
                            ?.contains("Season $season") == true || element.parent()
                            ?.previousElementSibling()?.select("h3")?.text()
                            ?.contains("Season$season") == true || element.parent()
                            ?.previousElementSibling()?.select("h3")?.text()
                            ?.contains("Season 0$season") == true || element.parent()
                            ?.previousElementSibling()?.select("h3")?.text()
                            ?.contains("Season0$season") == true) && element.text()
                            .contains("link", ignoreCase = true)
                    }.map { element ->
                        async {
                            try {
                                val link = """[?&]url=([^&]+)""".toRegex()
                                    .find(element.attr("href"))?.groupValues?.get(1)
                                    ?.let { Base64.Default.decode(it).decodeToString() }
                                link?.let {
                                    val ubGamesLink =
                                        Jsoup.parse(fetcher.fetchWithRetries(link)).select("h3 a")
                                            .firstOrNull { it.text().contains("Episode $episode") }
                                            ?.attr("href")
                                    println("ubgamelink: $ubGamesLink")
                                    ubGamesLink?.let { gameLink ->
                                        val driveSeedLink = getUnblockedGames(gameLink, fetcher)
                                        println("driveseed: $driveSeedLink")
                                        if (driveSeedLink != null && driveSeedLink.contains(
                                                "driveseed",
                                                true
                                            )
                                        ) {
                                            getDriveSeed(driveSeedLink, fetcher)?.let { it1 ->
                                                ddlStreams.add(
                                                    it1
                                                )
                                            }

                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ParallelProcessing", "Error processing element", e)
                            }
                        }
                    }.awaitAll()
                }
            }
        } catch (e: Exception) {
            println(e)
            Result.success(ddlStreams)
        }
        Result.success(ddlStreams)
    } catch (e: Exception) {
        println(e)
        Result.failure(e)
    }
}