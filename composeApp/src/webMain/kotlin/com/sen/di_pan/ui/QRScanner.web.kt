package com.sen.di_pan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
actual fun QRScannerView(
    onCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("QR Scanner Web", style = MaterialTheme.typography.headlineSmall)
                Text("Simulasi pemindaian untuk versi browser", color = Color.Gray)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onCodeScanned("DIPAN_STATUS_BERADA") }) {
                        Text("Simulasi Hadir")
                    }
                    Button(
                        onClick = { onCodeScanned("DIPAN_STATUS_TIDAK_BERADA") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Simulasi Tidak Hadir")
                    }
                }
                
                TextButton(onClick = onClose, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Tutup")
                }
            }
        }
    }
}
