package com.meshverse.app.di

import android.content.Context
import androidx.room.Room
import com.meshverse.app.data.local.MeshVerseDatabase
import com.meshverse.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MeshVerseDatabase {
        return Room.databaseBuilder(
            context,
            MeshVerseDatabase::class.java,
            "meshverse.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides fun provideUserDao(db: MeshVerseDatabase): UserDao = db.userDao()
    @Provides fun provideMessageDao(db: MeshVerseDatabase): MessageDao = db.messageDao()
    @Provides fun provideConversationDao(db: MeshVerseDatabase): ConversationDao = db.conversationDao()
    @Provides fun providePeerDao(db: MeshVerseDatabase): PeerDao = db.peerDao()
    @Provides fun providePostDao(db: MeshVerseDatabase): PostDao = db.postDao()
    @Provides fun provideMeshRouteDao(db: MeshVerseDatabase): MeshRouteDao = db.meshRouteDao()
    @Provides fun provideMediaDao(db: MeshVerseDatabase): MediaDao = db.mediaDao()
    @Provides fun provideWalletDao(db: MeshVerseDatabase): WalletDao = db.walletDao()
    @Provides fun provideTransactionDao(db: MeshVerseDatabase): TransactionDao = db.transactionDao()
}
