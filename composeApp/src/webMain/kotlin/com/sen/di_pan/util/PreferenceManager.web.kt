package com.sen.di_pan.util

import kotlinx.browser.window

class WebPreferenceManager : PreferenceManager {
    override fun saveString(key: String, value: String) {
        window.localStorage.setItem(key, value)
    }

    override fun getString(key: String): String? {
        return window.localStorage.getItem(key)
    }
}

actual fun createPreferenceManager(context: Any?): PreferenceManager {
    return WebPreferenceManager()
}
