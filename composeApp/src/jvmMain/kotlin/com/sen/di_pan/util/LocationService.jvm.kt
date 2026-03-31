package com.sen.di_pan.util

class JvmLocationService : LocationService {
    override suspend fun getCurrentLocation(): Pair<Double, Double>? {
        return null
    }
}

actual fun createLocationService(context: Any?): LocationService {
    return JvmLocationService()
}
