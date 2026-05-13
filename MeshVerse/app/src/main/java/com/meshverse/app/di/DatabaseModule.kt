package com.meshverse.app.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.meshverse.app.data.local.MeshVerseDatabase
import com.meshverse.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "meshverse.db"
    private const val PREFS_NAME = "meshverse_db_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    private val secureRandom = java.security.SecureRandom()

    /**
     * Retrieve or generate a random 256-bit passphrase stored in
     * EncryptedSharedPreferences (backed by the Android Keystore).
     * The passphrase is never stored in plaintext.
     */
    private fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = androidx.security.crypto.EncryptedSharedPreferences.create(
            PREFS_NAME,
            "meshverse_db_key",
            context,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) {
            return android.util.Base64.decode(existing, android.util.Base64.NO_WRAP)
        }
        val bytes = ByteArray(32).also { secureRandom.nextBytes(it) }
        prefs.edit()
            .putString(KEY_DB_PASSPHRASE, android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
            .apply()
        return bytes
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MeshVerseDatabase {
        SQLiteDatabase.loadLibs(context)
        val passphrase = getOrCreatePassphrase(context)
        val factory: SupportSQLiteOpenHelper.Factory = SupportFactory(passphrase)
        return Room.databaseBuilder(
            context,
            MeshVerseDatabase::class.java,
            DB_NAME
        )
        .openHelperFactory(factory)
        .addMigrations(MeshVerseDatabase.MIGRATION_1_2)
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
    @Provides fun providePollDao(db: MeshVerseDatabase): PollDao = db.pollDao()
    @Provides fun provideWalkieTalkieRoomDao(db: MeshVerseDatabase): WalkieTalkieRoomDao = db.walkieTalkieRoomDao()
    @Provides fun provideCommentDao(db: MeshVerseDatabase): CommentDao = db.commentDao()
}
