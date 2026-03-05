package com.sen.di_pan

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform