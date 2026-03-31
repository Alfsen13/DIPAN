package com.sen.di_pan.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sen.di_pan.data.FirebaseService
import com.sen.di_pan.model.User
import com.sen.di_pan.model.UserType
import com.sen.di_pan.util.PreferenceManager
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    firebaseService: FirebaseService,
    preferenceManager: PreferenceManager,
    onAuthSuccess: (User) -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    val savedEmail = remember { preferenceManager.getString("saved_email") ?: "" }
    val savedPassword = remember { preferenceManager.getString("saved_password") ?: "" }

    var email by remember { mutableStateOf(savedEmail) }
    var password by remember { mutableStateOf(savedPassword) }
    var name by remember { mutableStateOf("") }
    var nip by remember { mutableStateOf("") }
    var devisi by remember { mutableStateOf("") }
    var bidangKeahlian by remember { mutableStateOf("") }
    var asalSekolah by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf(UserType.PEGAWAI) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(savedEmail.isNotEmpty()) }

    // Ambil daftar devisi operator secara real-time
    val operatorDevisiList by firebaseService.getOperatorDevisiList().collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / Title Section
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "DIPAN",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 4.sp
            )
            Text(
                text = if (isLogin) "Digital Presence & Activity Network" else "Buat akun baru untuk mulai",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Form Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    AnimatedVisibility(visible = !isLogin) {
                        Column {
                            CustomTextField(value = name, onValueChange = { name = it }, label = "Nama Lengkap", icon = Icons.Default.Person)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Role Selector
                            Text("Daftar Sebagai:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RoleChip(selected = userType == UserType.PEGAWAI, label = "Pegawai") { userType = UserType.PEGAWAI; devisi = "" }
                                RoleChip(selected = userType == UserType.PESERTA_PLK, label = "Peserta") { userType = UserType.PESERTA_PLK; devisi = "" }
                                RoleChip(selected = userType == UserType.OPERATOR, label = "Operator") { userType = UserType.OPERATOR; devisi = "" }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            when(userType) {
                                UserType.PEGAWAI -> {
                                    CustomTextField(value = nip, onValueChange = { nip = it }, label = "NIP", icon = Icons.Default.Badge)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    DevisiDropdown(
                                        value = devisi, 
                                        onValueChange = { devisi = it }, 
                                        options = operatorDevisiList,
                                        label = "Pilih Devisi"
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    CustomTextField(value = bidangKeahlian, onValueChange = { bidangKeahlian = it }, label = "Bidang Keahlian", icon = Icons.Default.Star)
                                }
                                UserType.PESERTA_PLK -> {
                                    CustomTextField(value = asalSekolah, onValueChange = { asalSekolah = it }, label = "Sekolah / Instansi", icon = Icons.Default.School)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    DevisiDropdown(
                                        value = devisi, 
                                        onValueChange = { devisi = it }, 
                                        options = operatorDevisiList,
                                        label = "Pilih Pembagian Devisi"
                                    )
                                }
                                UserType.OPERATOR -> {
                                    CustomTextField(value = devisi, onValueChange = { devisi = it }, label = "Unit Kerja / Devisi Baru", icon = Icons.Default.AdminPanelSettings)
                                    if (operatorDevisiList.isNotEmpty()) {
                                        Text("Devisi yang sudah ada: ${operatorDevisiList.joinToString()}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    CustomTextField(value = email, onValueChange = { email = it }, label = "Email", icon = Icons.Default.Email)
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(
                        value = password, 
                        onValueChange = { password = it }, 
                        label = "Password", 
                        icon = Icons.Default.Lock,
                        isPassword = true
                    )

                    if (isLogin) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                            Text("Ingat Saya", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
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
                                                if (rememberMe) {
                                                    preferenceManager.saveString("saved_email", email)
                                                    preferenceManager.saveString("saved_password", password)
                                                } else {
                                                    preferenceManager.saveString("saved_email", "")
                                                    preferenceManager.saveString("saved_password", "")
                                                }
                                                onAuthSuccess(user)
                                            } else {
                                                error = "Data user tidak ditemukan."
                                            }
                                        } else {
                                            val isValid = when(userType) {
                                                UserType.PEGAWAI -> name.isNotEmpty() && nip.isNotEmpty() && devisi.isNotEmpty() && bidangKeahlian.isNotEmpty()
                                                UserType.PESERTA_PLK -> name.isNotEmpty() && asalSekolah.isNotEmpty() && devisi.isNotEmpty()
                                                UserType.OPERATOR -> name.isNotEmpty() && devisi.isNotEmpty()
                                            }
                                            if (!isValid) {
                                                error = "Harap isi semua field"
                                                isLoading = false
                                                return@launch
                                            }
                                            val result = firebaseService.auth.createUserWithEmailAndPassword(email, password)
                                            val newUser = User(
                                                id = result.user?.uid ?: "",
                                                name = name,
                                                email = email,
                                                nip = if (userType == UserType.PEGAWAI) nip else "",
                                                devisi = devisi,
                                                bidangKeahlian = if (userType == UserType.PEGAWAI) bidangKeahlian else "",
                                                asalSekolah = if (userType == UserType.PESERTA_PLK) asalSekolah else "",
                                                type = userType,
                                                isVerified = userType == UserType.OPERATOR
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
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(if (isLogin) "Masuk" else "Daftar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { isLogin = !isLogin }) {
                Text(
                    text = if (isLogin) "Belum punya akun? Daftar" else "Sudah punya akun? Login",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevisiDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            )
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Belum ada operator terdaftar", color = Color.Gray) },
                    onClick = { expanded = false }
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
            focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        ),
        singleLine = true
    )
}

@Composable
fun RoleChip(selected: Boolean, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
