package com.meshverse.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.meshverse.app.data.local.dao.*
import com.meshverse.app.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        MessageEntity::class,
        ConversationEntity::class,
        PeerEntity::class,
        PostEntity::class,
        MeshRouteEntity::class,
        MediaEntity::class,
        WalletEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MeshVerseDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun peerDao(): PeerDao
    abstract fun postDao(): PostDao
    abstract fun meshRouteDao(): MeshRouteDao
    abstract fun mediaDao(): MediaDao
    abstract fun walletDao(): WalletDao
    abstract fun transactionDao(): TransactionDao
}
