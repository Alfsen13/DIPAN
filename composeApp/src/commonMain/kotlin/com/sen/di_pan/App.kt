package com.sen.di_pan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sen.di_pan.data.FirebaseService
import com.sen.di_pan.model.User
import com.sen.di_pan.model.UserType
import com.sen.di_pan.ui.AuthScreen
import com.sen.di_pan.ui.PresenceBoard
import com.sen.di_pan.ui.OperatorPanel
import com.sen.di_pan.ui.SplashScreen
import com.sen.di_pan.util.PreferenceManager
import com.sen.di_pan.util.createPreferenceManager
import kotlinx.coroutines.launch

// Definisi Warna Kustom (Biru & Hijau Modern)
val DeepBlue = Color(0xFF1A237E)
val LightBlue = Color(0xFFE8EAF6)
val EmeraldGreen = Color(0xFF2E7D32)
val SoftGreen = Color(0xFFE8F5E9)

private val AppColorScheme = lightColorScheme(
    primary = DeepBlue,
    onPrimary = Color.White,
    primaryContainer = LightBlue,
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    secondaryContainer = SoftGreen,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    error = Color(0xFFB00020)
)

@Composable
fun App(context: Any? = null) {
    val firebaseService = remember { FirebaseService() }
    val scope = rememberCoroutineScope()
    val preferenceManager = remember { createPreferenceManager(context) }

    var showSplash by remember { mutableStateOf(true) }
    var currentAuthUser by remember { mutableStateOf(firebaseService.auth.currentUser) }
    
    val currentUser by if (currentAuthUser != null) {
        firebaseService.getUserFlow(currentAuthUser!!.uid).collectAsState(initial = null)
    } else {
        remember { mutableStateOf(null) }
    }

    var isCheckingAuth by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isCheckingAuth = false
    }

    MaterialTheme(
        colorScheme = AppColorScheme
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (showSplash) {
                SplashScreen(onTimeout = { showSplash = false })
            } else if (isCheckingAuth) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (currentUser == null) {
                AuthScreen(
                    firebaseService = firebaseService,
                    preferenceManager = preferenceManager,
                    onAuthSuccess = { user ->
                        currentAuthUser = firebaseService.auth.currentUser
                    }
                )
            } else {
                val user = currentUser!!
                
                if (!user.isVerified && user.type != UserType.OPERATOR) {
                    AlertDialog(
                        onDismissRequest = {
                            scope.launch {
                                firebaseService.auth.signOut()
                                currentAuthUser = null
                            }
                        },
                        title = { Text("Akses Dibatasi") },
                        text = { Text("Akun Anda belum diverifikasi oleh Operator. Silakan hubungi operator untuk verifikasi.") },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch {
                                    firebaseService.auth.signOut()
                                    currentAuthUser = null
                                }
                            }) {
                                Text("OK")
                            }
                        }
                    )
                } else {
                    when (user.type) {
                        UserType.OPERATOR -> {
                            OperatorPanel(
                                currentUser = user,
                                firebaseService = firebaseService,
                                context = context,
                                onLogout = {
                                    scope.launch {
                                        firebaseService.auth.signOut()
                                        currentAuthUser = null
                                    }
                                }
                            )
                        }
                        else -> {
                            PresenceBoard(
                                currentUser = user,
                                context = context,
                                onLogout = {
                                    scope.launch {
                                        firebaseService.auth.signOut()
                                        currentAuthUser = null
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
