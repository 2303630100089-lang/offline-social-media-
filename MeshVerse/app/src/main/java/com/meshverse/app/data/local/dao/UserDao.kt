package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Update
    suspend fun update(user: UserEntity)

    @Delete
    suspend fun delete(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE isLocalUser = 1 LIMIT 1")
    fun getLocalUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isLocalUser = 1 LIMIT 1")
    suspend fun getLocalUserOnce(): UserEntity?

    @Query("SELECT * FROM users WHERE isLocalUser = 0 ORDER BY lastSeen DESC")
    fun getAllRemoteUsers(): Flow<List<UserEntity>>

    @Query("UPDATE users SET lastSeen = :timestamp WHERE userId = :userId")
    suspend fun updateLastSeen(userId: String, timestamp: Long)

    @Query("UPDATE users SET latitude = :lat, longitude = :lon WHERE userId = :userId")
    suspend fun updateLocation(userId: String, lat: Double, lon: Double)
}
