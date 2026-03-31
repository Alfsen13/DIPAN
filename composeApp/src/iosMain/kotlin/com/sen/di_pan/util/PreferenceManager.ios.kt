package com.sen.di_pan.util

import platform.Foundation.NSUserDefaults

class IosPreferenceManager : PreferenceManager {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun saveString(key: String, value: String) {
        defaults.setObject(value, key)
    }

    override fun getString(key: String): String? {
        return defaults.stringForKey(key)
    }
}

actual fun createPreferenceManager(context: Any?): PreferenceManager {
    return IosPreferenceManager()
}
