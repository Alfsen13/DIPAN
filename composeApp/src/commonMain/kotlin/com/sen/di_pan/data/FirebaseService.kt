package com.sen.di_pan.data

import com.sen.di_pan.model.User
import com.sen.di_pan.model.UserType
import com.sen.di_pan.model.AttendanceStatus
import com.sen.di_pan.model.AttendanceLog
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Clock as DateTimeClock

class FirebaseService {
    private val root = try {
        Firebase.database("https://di-pan-default-rtdb.asia-southeast1.firebasedatabase.app/").reference()
    } catch (e: Exception) {
        println("Firebase Initialization Error: ${e.message}")
        null
    }
    
    private val usersRef = root?.child("users")
    private val logsRef = root?.child("logs")
    val auth = Firebase.auth

    fun getUsers(): Flow<List<User>> {
        val ref = usersRef ?: return flowOf(emptyList())
        return ref.valueEvents.map { snapshot ->
            snapshot.children.mapNotNull { 
                try {
                    it.value<User>()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    
    suspend fun getUserById(id: String): User? {
        val ref = usersRef ?: return null
        return try {
            ref.child(id).valueEvents.map { it.value<User>() }.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun getUserFlow(id: String): Flow<User?> {
        val ref = usersRef ?: return flowOf(null)
        return ref.child(id).valueEvents.map { 
            try {
                it.value<User>()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun updateUserPresence(user: User) {
        val ref = usersRef ?: return
        try {
            ref.child(user.id).setValue(user)
            
            // LOGIKA HISTORI LOG
            // Hanya simpan log jika statusnya adalah HADIR, CUTI, atau PERJALANAN_DINAS
            if (user.status == AttendanceStatus.HADIR || 
                user.status == AttendanceStatus.CUTI || 
                user.status == AttendanceStatus.PERJALANAN_DINAS) {
                
                val logs = logsRef ?: return
                val now = DateTimeClock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val today = now.date.toString()
                
                // Cek duplikasi: Apakah user sudah punya log dengan status yang sama di hari yang sama?
                val existingLogsSnapshot = logs.valueEvents.map { snapshot ->
                    snapshot.children.mapNotNull { it.value<AttendanceLog>() }
                }.firstOrNull() ?: emptyList()

                val isDuplicate = existingLogsSnapshot.any { 
                    it.userId == user.id && it.date == today && it.status == user.status.label 
                }

                if (!isDuplicate) {
                    val logId = logs.push().key ?: ""
                    val newLog = AttendanceLog(
                        id = logId,
                        userId = user.id,
                        userName = user.name,
                        devisi = user.devisi,
                        status = user.status.label,
                        timestamp = DateTimeClock.System.now().toEpochMilliseconds(),
                        date = today,
                        time = "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
                    )
                    logs.child(logId).setValue(newLog)
                }
            }
        } catch (e: Exception) {
            println("Error updating presence: ${e.message}")
        }
    }
    
    suspend fun addUser(user: User) {
        usersRef?.child(user.id)?.setValue(user)
    }

    suspend fun verifyUser(userId: String) {
        usersRef?.child(userId)?.child("isVerified")?.setValue(true)
    }

    suspend fun deleteUser(userId: String) {
        usersRef?.child(userId)?.removeValue()
    }

    fun getLogsByDevisi(devisi: String): Flow<List<AttendanceLog>> {
        val ref = logsRef ?: return flowOf(emptyList())
        return ref.valueEvents.map { snapshot ->
            snapshot.children.mapNotNull { 
                try {
                    it.value<AttendanceLog>()
                } catch (e: Exception) {
                    null
                }
            }
            .filter { it.devisi == devisi }
            .sortedByDescending { it.timestamp }
        }
    }

    fun getUserLogs(userId: String): Flow<List<AttendanceLog>> {
        val ref = logsRef ?: return flowOf(emptyList())
        return ref.valueEvents.map { snapshot ->
            snapshot.children.mapNotNull { 
                try {
                    it.value<AttendanceLog>()
                } catch (e: Exception) {
                    null
                }
            }
            .filter { it.userId == userId }
            .sortedByDescending { it.timestamp }
        }
    }

    fun getOperatorDevisiList(): Flow<List<String>> {
        return getUsers().map { users ->
            users.filter { it.type == UserType.OPERATOR }
                .map { it.devisi }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
    }
}
