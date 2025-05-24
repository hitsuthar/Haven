package com.hitsuthar.june.utils.dDLProviders

import com.hitsuthar.june.utils.DocumentFetcher
import com.hitsuthar.june.utils.formattedQuery
import com.hitsuthar.june.utils.vegaMoviesUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document


fun vegaMoviesSearch(query: String, fetcher: DocumentFetcher): String? {
  val searchUrl = "$vegaMoviesUrl/?s=${formattedQuery(query)}"

  return try {
    val document: Document = Jsoup.parse(fetcher.fetchWithRetries(searchUrl))
    val firstResultUrl = document.select("div.post-thumbnail a").firstOrNull()?.attr("href")
    if (firstResultUrl.isNullOrBlank()) {
      null
    } else {
      firstResultUrl
    }
  } catch (e: Exception) {
    null
  }
}