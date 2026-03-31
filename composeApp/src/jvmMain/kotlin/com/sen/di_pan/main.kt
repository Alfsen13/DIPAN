package com.sen.di_pan

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState

fun main() {
    try {
        application {
            val windowState = rememberWindowState(width = 450.dp, height = 800.dp)
            
            Window(
                onCloseRequest = ::exitApplication,
                title = "DIPAN Desktop",
                state = windowState,
                resizable = true
            ) {
                App()
            }
        }
    } catch (e: Exception) {
        println("DIPAN Desktop Crash Log:")
        e.printStackTrace()
    }
}
