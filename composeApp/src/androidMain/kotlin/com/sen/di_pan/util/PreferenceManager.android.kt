package com.sen.di_pan.util

import android.content.Context
import android.content.SharedPreferences

class AndroidPreferenceManager(context: Context) : PreferenceManager {
    private val prefs: SharedPreferences = context.getSharedPreferences("dipan_prefs", Context.MODE_PRIVATE)

    override fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String): String? {
        return prefs.getString(key, null)
    }
}

actual fun createPreferenceManager(context: Any?): PreferenceManager {
    return AndroidPreferenceManager(context as Context)
}
