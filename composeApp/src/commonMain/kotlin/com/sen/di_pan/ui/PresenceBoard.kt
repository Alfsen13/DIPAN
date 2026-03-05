package com.sen.di_pan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sen.di_pan.model.User
import com.sen.di_pan.model.UserType
import com.sen.di_pan.model.AttendanceStatus
import com.sen.di_pan.data.FirebaseService
import kotlinx.coroutines.launch
import kotlinx.datetime.*

@Composable
fun PresenceBoard(
    currentUser: User,
    onLogout: () -> Unit
) {
    val firebaseService = remember { FirebaseService() }
    val scope = rememberCoroutineScope()
    val users by firebaseService.getUsers().collectAsState(initial = emptyList())

    // Logika Auto Reset jam 16:00 (4 Sore)
    LaunchedEffect(users) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val todayStr = now.date.toString() // Format otomatis: YYYY-MM-DD
        
        // now.hour selalu dalam format 24 jam (0-23)
        // Jadi jam 4 sore selalu 16, tidak peduli settingan tampilan HP user
        val isAfterResetTime = now.hour >= 16

        if (isAfterResetTime) {
            users.forEach { user ->
                if (user.type == UserType.PEGAWAI && 
                    user.status != AttendanceStatus.TIDAK_BERADA_DI_RUANGAN &&
                    user.lastAutoResetDate != todayStr) {
                    
                    scope.launch {
                        firebaseService.updateUserPresence(
                            user.copy(
                                status = AttendanceStatus.TIDAK_BERADA_DI_RUANGAN,
                                lastAutoResetDate = todayStr,
                                note = ""
                            )
                        )
                    }
                }
            }
        }
    }

    val latestCurrentUser = users.find { it.id == currentUser.id } ?: currentUser

    MainScreen(
        currentUser = latestCurrentUser,
        allUsers = users,
        onUpdateStatus = { updatedUser ->
            scope.launch { 
                firebaseService.updateUserPresence(updatedUser)
            }
        },
        onLogout = onLogout
    )
}

@Composable
fun MainScreen(
    currentUser: User,
    allUsers: List<User>,
    onUpdateStatus: (User) -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DIPAN - Digital Panel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onLogout) { Text("Logout") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentUser.type == UserType.PEGAWAI) {
            PegawaiControlPanel(currentUser, onUpdateStatus)
        } else {
            PesertaViewPanel(currentUser, allUsers)
        }
    }
}

@Composable
fun PegawaiControlPanel(user: User, onUpdateStatus: (User) -> Unit) {
    var showNoteDialog by remember { mutableStateOf(false) }
    var tempNote by remember { mutableStateOf(user.note) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Halo, ${user.name}", style = MaterialTheme.typography.headlineSmall)
            Text(user.bidangUrusan, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Status Anda saat ini:", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = user.status.label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (user.status == AttendanceStatus.PERJALANAN_DINAS && user.note.isNotEmpty()) {
                Text("Ket: ${user.note}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    Text("Update Status Kehadiran:", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AttendanceStatus.entries.forEach { status ->
            Button(
                onClick = {
                    if (status == AttendanceStatus.PERJALANAN_DINAS) {
                        showNoteDialog = true
                    } else {
                        onUpdateStatus(user.copy(status = status, note = ""))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = if (user.status == status) ButtonDefaults.buttonColors() 
                         else ButtonDefaults.filledTonalButtonColors()
            ) {
                Text(status.label)
            }
        }
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Keterangan Perjalanan Dinas") },
            text = {
                OutlinedTextField(
                    value = tempNote,
                    onValueChange = { tempNote = it },
                    label = { Text("Tujuan/Keterangan") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateStatus(user.copy(status = AttendanceStatus.PERJALANAN_DINAS, note = tempNote))
                    showNoteDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun PesertaViewPanel(currentUser: User, users: List<User>) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Halo, ${currentUser.name}", style = MaterialTheme.typography.titleMedium)
                Text("Asal Sekolah: ${currentUser.asalSekolah}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text("Daftar Status Pegawai:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(users.filter { it.type == UserType.PEGAWAI }) { pegawai ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pegawai.name, fontWeight = FontWeight.Bold)
                            Text(pegawai.bidangUrusan, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            if (pegawai.status == AttendanceStatus.PERJALANAN_DINAS && pegawai.note.isNotEmpty()) {
                                Text("Ket: ${pegawai.note}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        StatusBadge(pegawai.status)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: AttendanceStatus) {
    val color = when (status) {
        AttendanceStatus.BERADA_DI_RUANGAN -> Color(0xFF2E7D32)
        AttendanceStatus.TIDAK_BERADA_DI_RUANGAN -> Color(0xFFC62828)
        AttendanceStatus.CUTI -> Color(0xFFF9A825)
        AttendanceStatus.PERJALANAN_DINAS -> Color(0xFF1565C0)
    }
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.label,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
