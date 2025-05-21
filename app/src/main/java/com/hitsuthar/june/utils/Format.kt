package com.hitsuthar.june.utils

import android.annotation.SuppressLint
import kotlinx.datetime.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor

@SuppressLint("DefaultLocale")
fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024

    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        bytes >= kb -> String.format("%.2f KB", bytes / kb)
        else -> "$bytes Bytes"
    }
}
fun getFormattedDate(date: LocalDate?): String {
    val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val outputFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val formattedDate: TemporalAccessor = inputFormatter.parse(date.toString())
    return outputFormatter.format(formattedDate)
}
@SuppressLint("DefaultLocale")
fun getFormatedTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val hours = (milliseconds / (1000 * 60 * 60))

    return if (hours > 0) {
        String.format("%01d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

fun formattedQuery(query: String): String {
    return query.trim().replace(" ", "+")
}
