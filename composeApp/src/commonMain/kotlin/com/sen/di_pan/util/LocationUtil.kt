package com.sen.di_pan.util

import kotlin.math.*

object LocationConfig {
    // Koordinat Dinas Pendidikan Provinsi Sumatera Barat
    // Alamat: Jl. Jend. Sudirman No.52, Jati Baru, Kec. Padang Tim., Kota Padang, Sumatera Barat 25129
    const val OFFICE_LATITUDE = -0.938927
    const val OFFICE_LONGITUDE = 100.362635
    const val MAX_RADIUS_METERS = 50.0
}

private fun toRadians(degrees: Double): Double = degrees * PI / 180.0

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0 // Radius bumi dalam meter
    val dLat = toRadians(lat2 - lat1)
    val dLon = toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(toRadians(lat1)) * cos(toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

interface LocationService {
    suspend fun getCurrentLocation(): Pair<Double, Double>?
}

expect fun createLocationService(context: Any?): LocationService
