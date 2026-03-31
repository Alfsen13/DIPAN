package com.sen.di_pan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun QRScannerView(
    onCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    // Implementasi untuk iOS menggunakan AVFoundation biasanya dilakukan di sini.
    // Untuk saat ini kita berikan placeholder agar build Windows berhasil.
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text("QR Scanner iOS belum tersedia di Windows", color = Color.White)
    }
}
