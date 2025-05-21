package com.hitsuthar.june.utils

import java.util.Random

class SimpleIdGenerator {

    private val ALPHANUMERIC: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val ID_LENGTH: Int = 6 // Adjust length as needed

    fun generateRandomId(): String {
        val random: Random = Random()
        val sb = StringBuilder(ID_LENGTH)
        for (i in 0 until ID_LENGTH) {
            val index: Int = random.nextInt(ALPHANUMERIC.length)
            sb.append(ALPHANUMERIC[index])
        }
        return sb.toString()
    }
}