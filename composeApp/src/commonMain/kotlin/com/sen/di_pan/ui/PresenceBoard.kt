package com.sen.di_pan.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sen.di_pan.model.User
import com.sen.di_pan.model.UserType
import com.sen.di_pan.model.AttendanceStatus
import com.sen.di_pan.model.AttendanceLog
import com.sen.di_pan.data.FirebaseService
import com.sen.di_pan.util.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresenceBoard(
    currentUser: User,
    context: Any? = null,
    onLogout: () -> Unit
) {
    val firebaseService = remember { FirebaseService() }
    val scope = rememberCoroutineScope()
    val users by firebaseService.getUsers().collectAsState(initial = emptyList())
    val userLogs by firebaseService.getUserLogs(currentUser.id).collectAsState(initial = emptyList())
    
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val todayStr = now.date.toString()

    LaunchedEffect(users, todayStr) {
        users.forEach { user ->
            if ((user.type == UserType.PEGAWAI || user.type == UserType.PESERTA_PLK) && 
                user.lastAutoResetDate != todayStr) {
                scope.launch {
                    firebaseService.updateUserPresence(
                        user.copy(
                            status = AttendanceStatus.TIDAK_HADIR,
                            lastAutoResetDate = todayStr,
                            note = ""
                        )
                    )
                }
            }
        }
    }

    val latestCurrentUser = users.find { it.id == currentUser.id } ?: currentUser

    MainScreen(
        currentUser = latestCurrentUser,
        allUsers = users,
        userLogs = userLogs,
        context = context,
        onUpdateStatus = { updatedUser ->
            scope.launch { 
                firebaseService.updateUserPresence(
                    updatedUser.copy(lastAutoResetDate = todayStr)
                )
            }
        },
        onLogout = onLogout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentUser: User,
    allUsers: List<User>,
    userLogs: List<AttendanceLog>,
    context: Any?,
    onUpdateStatus: (User) -> Unit,
    onLogout: () -> Unit
) {
    var showScanner by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingLocation by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val locationService = remember { createLocationService(context) }
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val todayStr = now.date.toString()

    if (showScanner) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            QRScannerView(
                onCodeScanned = { code ->
                    val expectedHadir = "DIPAN_STATUS_HADIR_$todayStr"
                    val expectedPulang = "DIPAN_STATUS_TIDAK_HADIR_$todayStr"

                    val newStatus = when (code) {
                        expectedHadir -> AttendanceStatus.HADIR
                        expectedPulang -> AttendanceStatus.PULANG
                        else -> null
                    }

                    if (newStatus != null) {
                        scope.launch {
                            isCheckingLocation = true
                            errorMessage = null
                            val location = locationService.getCurrentLocation()
                            
                            if (location != null) {
                                val distance = calculateDistance(
                                    location.first, location.second,
                                    LocationConfig.OFFICE_LATITUDE, LocationConfig.OFFICE_LONGITUDE
                                )
                                
                                if (distance <= LocationConfig.MAX_RADIUS_METERS) {
                                    onUpdateStatus(currentUser.copy(status = newStatus, note = ""))
                                    showScanner = false
                                } else {
                                    val distInt = distance.toInt()
                                    errorMessage = "Gagal! Anda berada $distInt m dari kantor. Maksimal 50m."
                                }
                            } else {
                                errorMessage = "Gagal mendapatkan lokasi. Pastikan GPS aktif dan izin diberikan."
                            }
                            isCheckingLocation = false
                        }
                    } else {
                        errorMessage = "QR Code tidak valid atau sudah kadaluarsa!"
                    }
                },
                onClose = { 
                    showScanner = false 
                    errorMessage = null
                }
            )
            
            if (isCheckingLocation) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("Memverifikasi lokasi...", color = Color.White)
                    }
                }
            }

            // Scanner Overlay Close Button
            IconButton(
                onClick = { showScanner = false },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }

            errorMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "DIPAN", 
                            fontWeight = FontWeight.ExtraBold, 
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                when (currentUser.type) {
                    UserType.PEGAWAI -> PegawaiDashboard(currentUser, userLogs, context, onUpdateStatus, onOpenScanner = { showScanner = true })
                    UserType.PESERTA_PLK -> PesertaDashboard(currentUser, allUsers, userLogs, context, onOpenScanner = { showScanner = true })
                    else -> PesertaViewPanel(allUsers)
                }
            }
        }
    }
}

@Composable
fun PegawaiDashboard(
    user: User, 
    logs: List<AttendanceLog>,
    context: Any?,
    onUpdateStatus: (User) -> Unit,
    onOpenScanner: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Status Saya", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Riwayat", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        Spacer(Modifier.height(16.dp))

        AnimatedContent(targetState = selectedTab) { target ->
            when (target) {
                0 -> PegawaiControlPanel(user, onUpdateStatus, onOpenScanner)
                1 -> UserLogList(logs, context)
            }
        }
    }
}

@Composable
fun PesertaDashboard(
    user: User, 
    allUsers: List<User>, 
    logs: List<AttendanceLog>,
    context: Any?,
    onOpenScanner: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Presensi", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Riwayat", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        Spacer(Modifier.height(16.dp))

        AnimatedContent(targetState = selectedTab) { target ->
            when (target) {
                0 -> PesertaControlPanel(user, allUsers, onOpenScanner)
                1 -> UserLogList(logs, context)
            }
        }
    }
}

@Composable
fun UserLogList(logs: List<AttendanceLog>, context: Any?) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (logs.isNotEmpty()) {
            OutlinedButton(
                onClick = {
                    val csvContent = StringBuilder()
                    csvContent.append("Status,Tanggal,Waktu\n")
                    logs.forEach { log ->
                        csvContent.append("${log.status},${log.date},${log.time}\n")
                    }
                    shareCsvFile(csvContent.toString(), "Rekap_Kehadiran_Saya.csv", context)
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder(true)
            ) {
                Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ekspor Rekap Kehadiran (CSV)")
            }
        }

        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))
                    Text("Belum ada riwayat", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(logs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            Modifier.padding(16.dp), 
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(40.dp).background(
                                        color = getStatusColor(log.status).copy(alpha = 0.1f),
                                        shape = CircleShape
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getStatusIcon(log.status),
                                        contentDescription = null,
                                        tint = getStatusColor(log.status),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(log.status, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(log.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            Text(log.time, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PegawaiControlPanel(user: User, onUpdateStatus: (User) -> Unit, onOpenScanner: () -> Unit) {
    var showNoteDialog by remember { mutableStateOf(false) }
    var tempNote by remember { mutableStateOf(user.note) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Welcome Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
        ) {
            Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Selamat Datang,", style = MaterialTheme.typography.bodyMedium)
                    Text(user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Status Anda Saat Ini:", style = MaterialTheme.typography.labelSmall)
                    StatusBadgeLarge(user.status)
                }
                Icon(
                    Icons.Default.Person, 
                    contentDescription = null, 
                    modifier = Modifier.size(64.dp).alpha(0.2f)
                )
            }
        }

        // Action Scanner
        Text("Konfirmasi Kehadiran", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        
        Button(
            onClick = onOpenScanner,
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Scan QR Code", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Untuk Hadir atau Pulang", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        
        // Manual Updates
        Text("Update Status Lainnya", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusActionCard(
                title = "Izin / Cuti",
                icon = Icons.Default.EventBusy,
                color = Color(0xFFF9A825),
                modifier = Modifier.weight(1f),
                onClick = { onUpdateStatus(user.copy(status = AttendanceStatus.CUTI, note = "")) }
            )
            
            StatusActionCard(
                title = "Dinas Luar",
                icon = Icons.Default.DirectionsBus,
                color = Color(0xFF1565C0),
                modifier = Modifier.weight(1f),
                onClick = { showNoteDialog = true }
            )
        }
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Keterangan Dinas", fontWeight = FontWeight.Bold) },
            text = { 
                OutlinedTextField(
                    value = tempNote, 
                    onValueChange = { tempNote = it }, 
                    label = { Text("Tujuan / Keperluan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) 
            },
            confirmButton = { 
                Button(
                    onClick = { 
                        onUpdateStatus(user.copy(status = AttendanceStatus.PERJALANAN_DINAS, note = tempNote))
                        showNoteDialog = false 
                    },
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Simpan") } 
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun PesertaControlPanel(user: User, allUsers: List<User>, onOpenScanner: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Personal Status Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondary, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.School, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Peserta PLK / Magang", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.alpha(0.2f))
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Status Kehadiran:", style = MaterialTheme.typography.bodySmall)
                    StatusBadge(user.status)
                }
            }
        }

        Button(
            onClick = onOpenScanner,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.QrCodeScanner, null)
            Spacer(Modifier.width(12.dp))
            Text("Scan QR Sekarang", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.People, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Daftar Pegawai & Pembimbing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(Modifier.height(12.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(allUsers.filter { it.type == UserType.PEGAWAI }) { p ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(p.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(p.devisi, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        StatusBadge(p.status)
                    }
                }
            }
        }
    }
}

@Composable
fun PesertaViewPanel(users: List<User>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(16.dp)) {
        items(users) { u ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(u.name, fontWeight = FontWeight.Bold)
                        Text(u.type.name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    StatusBadge(u.status)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: AttendanceStatus) {
    val color = getStatusColor(status.label)
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = status.label,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusBadgeLarge(status: AttendanceStatus) {
    val color = getStatusColor(status.label)
    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(getStatusIcon(status.label), null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = status.label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun StatusActionCard(title: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

fun getStatusColor(statusLabel: String): Color {
    return when (statusLabel) {
        "Hadir" -> Color(0xFF2E7D32)
        "Pulang" -> Color(0xFF7B1FA2)
        "Cuti" -> Color(0xFFF9A825)
        "Perjalanan Dinas" -> Color(0xFF1565C0)
        else -> Color(0xFFC62828)
    }
}

fun getStatusIcon(statusLabel: String): ImageVector {
    return when (statusLabel) {
        "Hadir" -> Icons.Default.CheckCircle
        "Pulang" -> Icons.Default.Home
        "Cuti" -> Icons.Default.EventBusy
        "Perjalanan Dinas" -> Icons.Default.DirectionsBus
        else -> Icons.Default.Cancel
    }
}
