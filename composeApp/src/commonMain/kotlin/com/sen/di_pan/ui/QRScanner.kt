package com.sen.di_pan.ui

import androidx.compose.runtime.Composable

@Composable
expect fun QRScannerView(
    onCodeScanned: (String) -> Unit,
    onClose: () -> Unit
)
