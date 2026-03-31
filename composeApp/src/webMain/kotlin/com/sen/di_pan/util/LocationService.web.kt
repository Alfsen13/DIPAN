package com.sen.di_pan.util

class WebLocationService : LocationService {
    override suspend fun getCurrentLocation(): Pair<Double, Double>? {
        return null
    }
}

actual fun createLocationService(context: Any?): LocationService {
    return WebLocationService()
}
