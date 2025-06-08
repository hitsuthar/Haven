package com.hitsuthar.june.utils

import android.text.TextUtils
import java.util.regex.Pattern

object EmojiDetector {
  // Regex pattern to match all Unicode emojis (updated for latest Unicode standards)
  private val EMOJI_PATTERN = Pattern.compile(
    "[\\p{So}\\p{Cs}\\p{Co}]" +
            "|[\uD83C\uDF00-\uD83D\uDDFF]" +
            "|[\uD83E\uDD00-\uD83E\uDDFF]" +
            "|[\uD83D\uDE00-\uD83D\uDE4F]" +
            "|[\uD83D\uDE80-\uD83D\uDEFF]" +
            "|[\u2600-\u26FF]" +
            "|[\u2700-\u27BF]" +
            "|[\uFE00-\uFE0F]"
  )

  // Checks if a string contains ONLY emojis (and whitespace)
  fun isPureEmoji(text: String): Boolean {
    if (TextUtils.isEmpty(text.trim())) return false

    val cleanText = text.replace("\\s".toRegex(), "") // Remove whitespace
    if (cleanText.isEmpty()) return false

    // Check if every remaining character is an emoji
    return cleanText.all { isSingleEmoji(it.toString()) }
  }

  // Checks if a single character/string is an emoji
  private fun isSingleEmoji(char: String): Boolean {
    return EMOJI_PATTERN.matcher(char).matches()
  }
}