package com.sen.di_pan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sen.di_pan.data.FirebaseService
import com.sen.di_pan.model.User
import com.sen.di_pan.model.UserType
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    firebaseService: FirebaseService,
    onAuthSuccess: (User) -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var bidangUrusan by remember { mutableStateOf("") }
    var asalSekolah by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf(UserType.PEGAWAI) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLogin) "Login DIPAN" else "Daftar Akun Baru",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (!isLogin) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (userType == UserType.PEGAWAI) {
                OutlinedTextField(
                    value = bidangUrusan,
                    onValueChange = { bidangUrusan = it },
                    label = { Text("Bidang Urusan") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = asalSekolah,
                    onValueChange = { asalSekolah = it },
                    label = { Text("Asal Sekolah") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Mendaftar sebagai:", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = userType == UserType.PEGAWAI,
                    onClick = { userType = UserType.PEGAWAI }
                )
                Text("Pegawai")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = userType == UserType.PESERTA_PLK,
                    onClick = { userType = UserType.PESERTA_PLK }
                )
                Text("Peserta PLK")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        error = null
                        try {
                            if (isLogin) {
                                val result = firebaseService.auth.signInWithEmailAndPassword(email, password)
                                val user = firebaseService.getUserById(result.user?.uid ?: "")
                                if (user != null) {
                                    onAuthSuccess(user)
                                } else {
                                    error = "Data user tidak ditemukan di database."
                                }
                            } else {
                                if (name.isEmpty() || (userType == UserType.PEGAWAI && bidangUrusan.isEmpty()) || (userType == UserType.PESERTA_PLK && asalSekolah.isEmpty())) {
                                    error = "Semua field wajib diisi"
                                    isLoading = false
                                    return@launch
                                }
                                val result = firebaseService.auth.createUserWithEmailAndPassword(email, password)
                                val newUser = User(
                                    id = result.user?.uid ?: "",
                                    name = name,
                                    email = email,
                                    bidangUrusan = if (userType == UserType.PEGAWAI) bidangUrusan else "",
                                    asalSekolah = if (userType == UserType.PESERTA_PLK) asalSekolah else "",
                                    type = userType
                                )
                                firebaseService.addUser(newUser)
                                onAuthSuccess(newUser)
                            }
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLogin) "Login" else "Daftar")
            }

            TextButton(onClick = { isLogin = !isLogin }) {
                Text(if (isLogin) "Belum punya akun? Daftar di sini" else "Sudah punya akun? Login")
            }
        }
    }
}
