package com.hitsuthar.june

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit


class SharedPreferencesManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "MyAppPrefs"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun <T : Any> saveData(key: String, value: T) {
        sharedPreferences.edit {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> throw IllegalArgumentException("Unsupported data type: ${value::class.java.name}")
            }
        }
        sharedPreferences.edit().apply()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getData(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is String -> sharedPreferences.getString(key, defaultValue) as T
            is Int -> sharedPreferences.getInt(key, defaultValue) as T
            is Boolean -> sharedPreferences.getBoolean(key, defaultValue) as T
            is Float -> sharedPreferences.getFloat(key, defaultValue) as T
            is Long -> sharedPreferences.getLong(key, defaultValue) as T
            else -> throw IllegalArgumentException("Unsupported default value type: ${defaultValue::class.java.name}")
        }
    }
}