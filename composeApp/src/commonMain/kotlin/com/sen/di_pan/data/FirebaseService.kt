package com.sen.di_pan.data

import com.sen.di_pan.model.User
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull

class FirebaseService {
    private val database = Firebase.database("https://di-pan-default-rtdb.asia-southeast1.firebasedatabase.app/").reference("users")
    val auth = Firebase.auth

    fun getUsers(): Flow<List<User>> {
        return database.valueEvents.map { DataSnapshot ->
            DataSnapshot.children.map { it.value<User>() }
        }
    }
    
    suspend fun getUserById(id: String): User? {
        return database.child(id).valueEvents.map { it.value<User>() }.firstOrNull()
    }

    suspend fun updateUserPresence(user: User) {
        database.child(user.id).setValue(user)
    }
    
    suspend fun addUser(user: User) {
        database.child(user.id).setValue(user)
    }
}
