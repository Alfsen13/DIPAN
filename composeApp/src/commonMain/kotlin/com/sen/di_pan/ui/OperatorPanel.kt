package com.sen.di_pan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.sen.di_pan.data.FirebaseService
import com.sen.di_pan.model.User
import com.sen.di_pan.model.UserType
import com.sen.di_pan.model.AttendanceStatus
import com.sen.di_pan.util.shareCsvFile
import com.sen.di_pan.util.shareImageFromUrl
import kotlinx.coroutines.launch
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorPanel(
    currentUser: User,
    firebaseService: FirebaseService,
    context: Any?,
    onLogout: () -> Unit
) {
    val users by firebaseService.getUsers().collectAsState(initial = emptyList())
    val logs by firebaseService.getLogsByDevisi(currentUser.devisi).collectAsState(initial = emptyList())
    
    val filteredUsers = users.filter { it.devisi == currentUser.devisi && it.id != currentUser.id }
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Verifikasi", "Kelola User", "Rekap Data")
    
    var showQRHadirDialog by remember { mutableStateOf(false) }
    var showQRPulangDialog by remember { mutableStateOf(false) }
    
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val todayStr = now.date.toString()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Panel Operator", fontWeight = FontWeight.Bold)
                        Text("Devisi: ${currentUser.devisi}", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (selectedTab == 0 || selectedTab == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showQRHadirDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.QrCode, null)
                        Spacer(Modifier.width(4.dp))
                        Text("QR Hadir", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { showQRPulangDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.QrCode, null)
                        Spacer(Modifier.width(4.dp))
                        Text("QR Pulang", fontSize = 12.sp)
                    }
                }
            }

            when (selectedTab) {
                0 -> VerificationList(filteredUsers.filter { !it.isVerified && it.type != UserType.OPERATOR }, firebaseService)
                1 -> UserManagementList(filteredUsers, firebaseService)
                2 -> RekapDataPanel(logs, context)
            }
        }
    }

    if (showQRHadirDialog) {
        QRDialog(
            label = "QR HADIR",
            data = "DIPAN_STATUS_HADIR_$todayStr",
            color = Color(0xFF2E7D32),
            context = context,
            onDismiss = { showQRHadirDialog = false }
        )
    }

    if (showQRPulangDialog) {
        QRDialog(
            label = "QR PULANG",
            data = "DIPAN_STATUS_TIDAK_HADIR_$todayStr",
            color = Color(0xFF7B1FA2),
            context = context,
            onDismiss = { showQRPulangDialog = false }
        )
    }
}

@Composable
fun QRDialog(label: String, data: String, color: Color, context: Any?, onDismiss: () -> Unit) {
    val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=500x500&data=$data"
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.size(250.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    AsyncImage(
                        model = qrUrl,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                Text(data, fontSize = 10.sp, color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = { shareImageFromUrl(qrUrl, "QR_${label.replace(" ", "_")}.png", context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download / Bagikan QR")
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
fun RekapDataPanel(logs: List<com.sen.di_pan.model.AttendanceLog>, context: Any?) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ringkasan Hari Ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val totalHadir = logs.filter { it.status == AttendanceStatus.HADIR.label }.size
            val totalPulang = logs.filter { it.status == AttendanceStatus.PULANG.label }.size
            
            StatCard("Total Hadir", totalHadir.toString(), Color(0xFF2E7D32), Modifier.weight(1f))
            StatCard("Total Pulang", totalPulang.toString(), Color(0xFF7B1FA2), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                val csvContent = StringBuilder()
                csvContent.append("Nama,Status,Tanggal,Waktu\n")
                logs.forEach { log ->
                    csvContent.append("${log.userName},${log.status},${log.date},${log.time}\n")
                }
                shareCsvFile(csvContent.toString(), "Rekap_Kehadiran_Devisi.csv", context)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Download Laporan Devisi (CSV)")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Riwayat Aktivitas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            items(logs) { log ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(log.userName, fontWeight = FontWeight.Bold)
                            Text(log.status, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${log.date} ${log.time}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun VerificationList(unverifiedUsers: List<User>, firebaseService: FirebaseService) {
    val scope = rememberCoroutineScope()
    if (unverifiedUsers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tidak ada user baru", color = Color.Gray)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(unverifiedUsers) { user ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.name, fontWeight = FontWeight.Bold)
                            Text(user.email, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { scope.launch { firebaseService.verifyUser(user.id) } }) {
                            Text("Verifikasi")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserManagementList(allUsers: List<User>, firebaseService: FirebaseService) {
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf<User?>(null) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(allUsers) { user ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.name, fontWeight = FontWeight.Bold)
                        Text(user.type.name, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { showDeleteDialog = user }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                    }
                }
            }
        }
    }
}
