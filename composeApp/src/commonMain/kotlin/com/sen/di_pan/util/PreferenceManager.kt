package com.sen.di_pan.util

interface PreferenceManager {
    fun saveString(key: String, value: String)
    fun getString(key: String): String?
}

expect fun createPreferenceManager(context: Any?): PreferenceManager
