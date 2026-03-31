package com.sen.di_pan.util

import java.awt.Desktop
import java.io.File

actual fun shareCsvFile(content: String, fileName: String, context: Any?) {
    try {
        val userHome = System.getProperty("user.home")
        val downloadFolder = File(userHome, "Downloads")
        if (!downloadFolder.exists()) downloadFolder.mkdirs()
        
        val file = File(downloadFolder, fileName)
        file.writeText(content)
        
        // Membuka folder download agar user bisa langsung melihat filenya
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(downloadFolder)
        }
        
        println("File berhasil disimpan di: ${file.absolutePath}")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
