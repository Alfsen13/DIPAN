package com.sen.di_pan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sen.di_pan.data.FirebaseService
import com.sen.di_pan.model.User
import com.sen.di_pan.ui.AuthScreen
import com.sen.di_pan.ui.PresenceBoard
import kotlinx.coroutines.launch

@Composable
fun App() {
    val firebaseService = remember { FirebaseService() }
    val scope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf<User?>(null) }
    var isCheckingAuth by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val authUser = firebaseService.auth.currentUser
        if (authUser != null) {
            currentUser = firebaseService.getUserById(authUser.uid)
        }
        isCheckingAuth = false
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isCheckingAuth) {
                // Loading screen while checking session
            } else if (currentUser == null) {
                AuthScreen(
                    firebaseService = firebaseService,
                    onAuthSuccess = { user ->
                        currentUser = user
                    }
                )
            } else {
                PresenceBoard(
                    currentUser = currentUser!!,
                    onLogout = {
                        scope.launch {
                            firebaseService.auth.signOut()
                            currentUser = null
                        }
                    }
                )
            }
        }
    }
}
