package com.sen.di_pan.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

actual fun shareCsvFile(content: String, fileName: String, context: Any?) {
    val ctx = context as Context
    try {
        val file = File(ctx.cacheDir, fileName)
        file.writeText(content)
        
        val uri = FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Kehadiran DIPAN")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        ctx.startActivity(Intent.createChooser(intent, "Bagikan Laporan"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

actual fun shareImageFromUrl(url: String, fileName: String, context: Any?) {
    val ctx = context as? Context ?: return
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val bytes = URL(url).readBytes()
            val file = File(ctx.cacheDir, fileName)
            file.writeBytes(bytes)
            
            val uri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                file
            )
            
            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ctx.startActivity(Intent.createChooser(intent, "Simpan/Bagikan QR Code"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
