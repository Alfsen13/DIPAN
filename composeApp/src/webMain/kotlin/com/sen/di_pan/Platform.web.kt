package com.sen.di_pan

class WebPlatform : Platform {
    override val name: String = "Web Browser"
}

actual fun getPlatform(): Platform = WebPlatform()
