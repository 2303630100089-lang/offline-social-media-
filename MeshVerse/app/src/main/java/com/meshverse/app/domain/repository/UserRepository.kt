package com.meshverse.app.domain.repository

import com.meshverse.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getLocalUser(): Flow<User?>
    suspend fun getLocalUserOnce(): User?
    suspend fun saveUser(user: User)
    suspend fun updateLocation(userId: String, lat: Double, lon: Double)
    fun getAllRemoteUsers(): Flow<List<User>>
    suspend fun getUserById(userId: String): User?
}
