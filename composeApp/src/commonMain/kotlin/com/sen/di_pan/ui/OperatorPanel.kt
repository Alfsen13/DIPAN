package com.sen.di_pan.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Verifikasi", "Kelola User", "Rekap Data")
    
    var showQRHadirDialog by remember { mutableStateOf(false) }
    var showQRPulangDialog by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val todayStr = now.date.toString()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PANEL OPERATOR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = currentUser.devisi.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Bantuan", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.padding(end = 8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (selectedTab == 0 || selectedTab == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QRActionButton(
                        label = "QR HADIR",
                        icon = Icons.Default.QrCode,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f),
                        onClick = { showQRHadirDialog = true }
                    )
                    QRActionButton(
                        label = "QR PULANG",
                        icon = Icons.Default.QrCode,
                        color = Color(0xFF7B1FA2),
                        modifier = Modifier.weight(1f),
                        onClick = { showQRPulangDialog = true }
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "OperatorTabTransition"
            ) { target ->
                when (target) {
                    0 -> VerificationList(filteredUsers.filter { !it.isVerified && it.type != UserType.OPERATOR }, firebaseService)
                    1 -> UserManagementList(filteredUsers, firebaseService)
                    2 -> RekapDataPanel(logs, context)
                }
            }
        }
    }

    // Dialog Bantuan Operator
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Bantuan Operator", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OperatorHelpItem(Icons.Default.QrCode, "QR Code: Buat dan bagikan kode QR ke pegawai agar mereka bisa melakukan presensi hari ini.")
                    OperatorHelpItem(Icons.Default.PersonAdd, "Verifikasi: Setujui pendaftaran user baru agar mereka bisa masuk ke sistem.")
                    OperatorHelpItem(Icons.Default.Group, "Kelola User: Lihat dan pantau status akun dari semua anggota devisi Anda.")
                    OperatorHelpItem(Icons.Default.Assessment, "Rekap Data: Unduh laporan kehadiran harian dalam format CSV untuk arsip.")
                }
            },
            confirmButton = {
                Button(onClick = { showHelp = false }, shape = RoundedCornerShape(12.dp)) { Text("Siap!") }
            },
            shape = RoundedCornerShape(28.dp)
        )
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
fun OperatorHelpItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun QRActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun QRDialog(label: String, data: String, color: Color, context: Any?, onDismiss: () -> Unit) {
    val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=500x500&data=$data"
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = label, 
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Black, 
                        color = color, 
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.size(260.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    AsyncImage(
                        model = qrUrl,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                Text("Scan QR ini untuk melakukan presensi", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(data, fontSize = 9.sp, color = Color.LightGray)
                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = { shareImageFromUrl(qrUrl, "QR_${label.replace(" ", "_")}.png", context) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text("BAGIKAN QR", fontWeight = FontWeight.Bold)
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Tutup", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun RekapDataPanel(logs: List<com.sen.di_pan.model.AttendanceLog>, context: Any?) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("RINGKASAN HARI INI", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val totalHadir = logs.filter { it.status == AttendanceStatus.HADIR.label }.size
            val totalPulang = logs.filter { it.status == AttendanceStatus.PULANG.label }.size
            
            StatCard("HADIR", totalHadir.toString(), Color(0xFF2E7D32), Icons.Default.CheckCircle, Modifier.weight(1f))
            StatCard("PULANG", totalPulang.toString(), Color(0xFF7B1FA2), Icons.Default.Home, Modifier.weight(1f))
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
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("UNDUH REKAP CSV", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("LOG AKTIVITAS TERBARU", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs.sortedByDescending { "${it.date} ${it.time}" }) { log ->
                LogItem(log)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun LogItem(log: com.sen.di_pan.model.AttendanceLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val color = if(log.status == AttendanceStatus.HADIR.label) Color(0xFF2E7D32) else Color(0xFF7B1FA2)
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if(log.status == AttendanceStatus.HADIR.label) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${log.date} • ${log.time}", fontSize = 11.sp, color = Color.Gray)
            }
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    log.status, 
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = color
                )
            }
        }
    }
}

@Composable
fun VerificationList(users: List<User>, firebaseService: FirebaseService) {
    val scope = rememberCoroutineScope()
    
    if (users.isEmpty()) {
        EmptyState("Tidak ada antrian verifikasi", Icons.AutoMirrored.Filled.FactCheck)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.LightGray))
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(user.type.name, fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(
                            onClick = { scope.launch { firebaseService.verifyUser(user.id) } },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Check, "Verify", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserManagementList(users: List<User>, firebaseService: FirebaseService) {
    if (users.isEmpty()) {
        EmptyState("Belum ada user terdaftar", Icons.Default.People)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.LightGray))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if(user.isVerified) Color(0xFFE8F5E9) else Color(0xFFFBE9E7),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        if(user.isVerified) "VERIFIED" else "PENDING",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if(user.isVerified) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(user.type.name, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Text(message, color = Color.Gray, textAlign = TextAlign.Center)
    }
}
