package com.sen.di_pan.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserType {
    PEGAWAI,
    PESERTA_PLK,
    OPERATOR
}

@Serializable
enum class AttendanceStatus(val label: String) {
    TIDAK_HADIR("Tidak Hadir"),
    HADIR("Hadir"),
    PULANG("Pulang"),
    CUTI("Cuti"),
    PERJALANAN_DINAS("Perjalanan Dinas")
}

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val nip: String = "",
    val devisi: String = "",
    val bidangKeahlian: String = "",
    val asalSekolah: String = "",
    val type: UserType = UserType.PEGAWAI,
    val status: AttendanceStatus = AttendanceStatus.TIDAK_HADIR,
    val note: String = "",
    val lastAutoResetDate: String = "", // Format: YYYY-MM-DD
    val isVerified: Boolean = false
)

@Serializable
data class AttendanceLog(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val devisi: String = "",
    val status: String = "",
    val timestamp: Long = 0,
    val date: String = "", // Format: YYYY-MM-DD
    val time: String = ""  // Format: HH:mm:ss
)
