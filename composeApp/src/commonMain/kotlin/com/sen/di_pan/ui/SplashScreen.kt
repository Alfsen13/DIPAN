package com.sen.di_pan.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dipan.composeapp.generated.resources.Res
import dipan.composeapp.generated.resources.ic_app_logo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Animasi Scale untuk Logo
    val scale = remember { Animatable(0f) }
    
    // Animasi Alpha untuk Teks
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Jalankan animasi secara berurutan
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        scale.animateTo(1f)
        
        alpha.animateTo(1f, animationSpec = tween(1000))
        
        // Tunggu sebentar lalu pindah ke halaman berikutnya
        delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Bingkai Logo Splash
            Surface(
                modifier = Modifier
                    .size(150.dp)
                    .scale(scale.value)
                    .clip(RoundedCornerShape(32.dp)),
                color = Color.White,
                tonalElevation = 12.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(Res.drawable.ic_app_logo),
                        contentDescription = "Logo Splash",
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nama Aplikasi dengan Animasi Fade-In
            Text(
                text = "DIPAN",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(alpha.value)
            )
            
            Text(
                text = "Digital Presence & Activity Network",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f * alpha.value),
                modifier = Modifier.alpha(alpha.value)
            )
        }
        
        // Label Versi di bagian bawah
        Text(
            text = "Version 1.0.0",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
