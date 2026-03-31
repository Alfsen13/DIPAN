package com.sen.di_pan.util

import java.util.prefs.Preferences

class JvmPreferenceManager : PreferenceManager {
    private val prefs = Preferences.userNodeForPackage(this::class.java)

    override fun saveString(key: String, value: String) {
        prefs.put(key, value)
    }

    override fun getString(key: String): String? {
        return prefs.get(key, null)
    }
}

actual fun createPreferenceManager(context: Any?): PreferenceManager {
    return JvmPreferenceManager()
}
