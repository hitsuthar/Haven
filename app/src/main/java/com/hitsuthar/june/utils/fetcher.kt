package com.hitsuthar.june.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class DocumentFetcher {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun fetchWithRetries(
        url: String,
        maxRetries: Int = 3,
        initialDelayMillis: Long = 1000,
        customTimeoutSeconds: Long? = null
    ): String {
        require(maxRetries > 0) { "Max retries must be positive" }
        require(initialDelayMillis >= 0) { "Initial delay must be non-negative" }

        // Use custom timeout if provided, otherwise use client's default
        val effectiveClient = customTimeoutSeconds?.let { timeout ->
            client.newBuilder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .build()
        } ?: client

        var lastError: Throwable? = null
        var currentDelay = initialDelayMillis

        repeat(maxRetries) { attempt ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "DocumentFetcher/1.0")
                    .build()

                effectiveClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected HTTP code ${response.code} for $url")
                    }

                    return response.body?.string()
                        ?: throw IOException("Empty response body from $url")
//                    return Jsoup.parse(body, url)
                }
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxRetries - 1) {
                    Thread.sleep(currentDelay)
                    currentDelay *= 2 // Exponential backoff
                }
            }
        }

        throw IOException("Failed to fetch $url after $maxRetries attempts", lastError)
    }

}