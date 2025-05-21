package com.hitsuthar.june.utils

fun createMagnetLink(infoHash: String, trackers: List<String>): String {
    val base = "magnet:?xt=urn:btih:$infoHash"
    val trackersParam = trackers.joinToString("&tr=") { it.removePrefix("tracker:") }
    return "$base&tr=$trackersParam"
}