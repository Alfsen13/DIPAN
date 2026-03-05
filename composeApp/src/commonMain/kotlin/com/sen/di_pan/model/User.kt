package com.sen.di_pan.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserType {
    PEGAWAI,
    PESERTA_PLK
}

@Serializable
enum class AttendanceStatus(val label: String) {
    TIDAK_BERADA_DI_RUANGAN("Tidak Berada di Ruangan"),
    BERADA_DI_RUANGAN("Berada di Ruangan"),
    CUTI("Cuti"),
    PERJALANAN_DINAS("Perjalanan Dinas")
}

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val bidangUrusan: String = "",
    val asalSekolah: String = "",
    val type: UserType = UserType.PEGAWAI,
    val status: AttendanceStatus = AttendanceStatus.TIDAK_BERADA_DI_RUANGAN,
    val note: String = "",
    val lastAutoResetDate: String = "" // Format: YYYY-MM-DD
)
