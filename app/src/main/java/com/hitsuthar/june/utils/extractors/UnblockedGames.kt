package com.hitsuthar.june.utils.extractors

import com.hitsuthar.june.utils.DocumentFetcher
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun getUnblockedGames(uRL: String, fetcher: DocumentFetcher): String? {
    val ddlDocument: Document = Jsoup.parse(fetcher.fetchWithRetries(uRL))
    val form = ddlDocument.select("form#landing")
    val formUrl = form.attr("action")
    val wpHttpName = form.select("input").attr("name")
    val wpHttp2Value = form.select("input").attr("value")
    val formData = mapOf(
        wpHttpName to wpHttp2Value,
    )
    val postResponse = Jsoup.connect(formUrl).method(Connection.Method.POST).data(formData).execute()
    val form2 = postResponse.parse().select("form#landing")
    val formUrl2 = form2.attr("action")
    val wpHttpValue2 = form2.select("input[name=_wp_http2]").attr("value")
    val tokenValue = form2.select("input[name=token]").attr("value")
    val formData2 = mapOf(
        "_wp_http2" to wpHttpValue2,
        "token" to tokenValue,
    )
    val postResponse2 = Jsoup.connect(formUrl2).method(Connection.Method.POST).data(formData2).execute()
    val jsCode = postResponse2.parse().select("script:containsData(verify_button)").html()

    val cookieMatch = """s_343\('([^']+)',\s*'([^']+)',\s*(\d+)\)""".toRegex().find(jsCode)
    val urlMatch = """setAttribute\(\s*["']href["']\s*,\s*["']([^"']+)["']\)""".toRegex().find(jsCode)
    if (cookieMatch != null && urlMatch != null) {
        val (cookieName, cookieValue, expiration) = cookieMatch.destructured
        val (url) = urlMatch.destructured

        val getResponse = Jsoup.connect(url).cookie(cookieName, cookieValue).get()
        val anotherUrl = """url=(https?://[^\s]+)""".toRegex()
            .find(getResponse.select("meta[http-equiv=refresh]").attr("content"))?.groupValues?.get(1)
        val getAnotherResponse = anotherUrl?.let { Jsoup.connect(it).get() }

        val replaceRegex = getAnotherResponse?.select("script")?.html()
            ?.let { """replace\("([^"]+)"""".toRegex().find(it) }?.groupValues?.get(1)

        val newUrl = replaceRegex?.let { anotherUrl.replace(Regex("""/r\?.*"""), it) }
        if (newUrl != null) {
//            println(newUrl)
            return newUrl
        }
    }
    return null
}