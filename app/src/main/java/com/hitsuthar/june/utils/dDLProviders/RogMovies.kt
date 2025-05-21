package com.hitsuthar.june.utils.dDLProviders

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.utils.DocumentFetcher
import com.hitsuthar.june.utils.formattedQuery
import com.hitsuthar.june.utils.rogMoviesUrl
import com.hitsuthar.junescrapper.extractors.Extractor
import kotlinx.coroutines.Dispatchers
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
            Jsoup.parse(fetcher.fetchWithRetries(searchUrl), "UTF-8")
                .select("div.blog-pic a.blog-img")
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

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
suspend fun getRogMoviesDDL(
    title: String,
    year: Int? = null,
    type: String,
    season: Int? = null,
    episode: Int? = null,
    fetcher: DocumentFetcher
): Result<List<DDLStream>> = withContext(Dispatchers.IO) {
    try {
        val searchResultUrls = rogMoviesSearch(title, year, type, fetcher)
        if (searchResultUrls.isEmpty()) return@withContext Result.failure(NoResultsException("No movies found for '$title'"))

        val ddlStreams = mutableListOf<DDLStream>()

        try {
            searchResultUrls.map { resultURL ->
                when (type) {
                    "movie" -> {
                        Jsoup.parse(fetcher.fetchWithRetries(resultURL))
                            .select("div.entry-content > p > a")
                            .parallelStream().map { element ->
                                val link = element.attr("href")
                                val nextDrive =
                                    Jsoup.parse(fetcher.fetchWithRetries(link)).select("p a")
                                        .firstOrNull { nextDriveElement ->
                                            nextDriveElement.select("button").any { button ->
                                                button.text().contains("V-Cloud", ignoreCase = true)
                                            }
                                        }?.attr("href")
                                if (nextDrive != null) {
                                    val vcloud = Jsoup.parse(fetcher.fetchWithRetries(nextDrive))
                                        .select("script:containsData(url)").html()
                                    val hubCloud =
                                        Regex("var url = '([^']*)'").find(vcloud)?.groupValues?.get(
                                            1
                                        )
                                    if (hubCloud != null) {
                                        val ddl = Extractor().extractFromHUbCloud(hubCloud, fetcher)
                                        if (ddl != null) {
                                            ddlStreams.add(ddl)
                                            println(ddl)
                                        }
                                    }
                                }
                            }.toList()
                    }

                    "tv" -> {
                        println(resultURL)
                        Jsoup.parse(fetcher.fetchWithRetries(resultURL))
                            .select("div.entry-content > p a").filter { a ->
                                a.parent()?.previousElementSibling()?.text()
                                    ?.contains("Season $season") == true && a.text()
                                    .contains("V-Cloud", ignoreCase = true) || a.text()
                                    .contains("G-Direct", ignoreCase = true)
                            }.map { a: Element ->
                                val nextDriveURL = a.attr("href")
//												println(nextDriveURL)
                                val episodeH4 =
                                    Jsoup.parse(fetcher.fetchWithRetries(nextDriveURL)).select("h4")
                                        .firstOrNull { h4 ->
                                            h4.text().contains("Ep", ignoreCase = true)
                                            h4.text().contains(episode.toString())
                                        }
                                if (episodeH4 != null) {
                                    // The <p> we want is the next sibling element after the h4
                                    val downloadP = episodeH4.nextElementSibling()

                                    if (downloadP != null && downloadP.tagName() == "p") {
                                        // Extract all href links from the <p> element
                                        val vcloudURL =
                                            downloadP.select("a").firstOrNull { episodeA ->
                                                episodeA.attr("href")
                                                    .contains(
                                                        "vcloud",
                                                        ignoreCase = true
                                                    ) && !episodeA.attr("href")
                                                    .contains("api", ignoreCase = true)
                                            }?.attr("href")
                                        if (vcloudURL != null) {
                                            val hubCloud = Regex("var url = '([^']*)'").find(
                                                Jsoup.parse(fetcher.fetchWithRetries(vcloudURL))
                                                    .select("script:containsData(url)").html()
                                            )?.groupValues?.get(1)
                                            if (hubCloud != null) {
                                                val ddl =
                                                    Extractor().extractFromHUbCloud(
                                                        hubCloud,
                                                        fetcher
                                                    )
                                                if (ddl != null) {
                                                    ddlStreams.add(ddl)
                                                    println(ddl)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    }

                    else -> {
                        Result.success(ddlStreams)
                    }
                }
            }
        } catch (e: Exception) {
            Result.success(ddlStreams)
        }


        Result.success(ddlStreams)
    } catch (e: Exception) {
        println(e)
        Result.failure(e)
    }
}