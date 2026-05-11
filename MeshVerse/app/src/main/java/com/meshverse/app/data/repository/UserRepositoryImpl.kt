package com.meshverse.app.data.repository

import com.meshverse.app.data.local.dao.UserDao
import com.meshverse.app.data.local.entity.UserEntity
import com.meshverse.app.domain.model.User
import com.meshverse.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override fun getLocalUser(): Flow<User?> =
        userDao.getLocalUser().map { it?.toDomain() }

    override suspend fun getLocalUserOnce(): User? =
        userDao.getLocalUserOnce()?.toDomain()

    override suspend fun saveUser(user: User) =
        userDao.insert(user.toEntity())

    override suspend fun updateLocation(userId: String, lat: Double, lon: Double) =
        userDao.updateLocation(userId, lat, lon)

    override fun getAllRemoteUsers(): Flow<List<User>> =
        userDao.getAllRemoteUsers().map { list -> list.map { it.toDomain() } }

    override suspend fun getUserById(userId: String): User? =
        userDao.getById(userId)?.toDomain()

    private fun UserEntity.toDomain() = User(
        userId, username, displayName, avatarPath, publicKey,
        deviceFingerprint, isLocalUser, isAnonymous, lastSeen,
        reputation, latitude, longitude, bio, followersCount, followingCount
    )

    private fun User.toEntity() = UserEntity(
        userId, username, displayName, avatarPath, publicKey,
        deviceFingerprint, isLocalUser, isAnonymous, lastSeen,
        reputation, latitude, longitude, bio, followersCount, followingCount
    )
}
