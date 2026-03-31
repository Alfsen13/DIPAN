package com.sen.di_pan.util

class IosLocationService : LocationService {
    override suspend fun getCurrentLocation(): Pair<Double, Double>? {
        // Implementasi iOS bisa ditambahkan nanti menggunakan CoreLocation
        return null
    }
}

actual fun createLocationService(context: Any?): LocationService {
    return IosLocationService()
}
