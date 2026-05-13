package com.meshverse.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        TransactionEntity::class,
        PollEntity::class,
        PollVoteEntity::class,
        WalkieTalkieRoomEntity::class,
        CommentEntity::class
    ],
    version = 2,
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
    abstract fun pollDao(): PollDao
    abstract fun walkieTalkieRoomDao(): WalkieTalkieRoomDao
    abstract fun commentDao(): CommentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `polls` (
                        `pollId` TEXT NOT NULL,
                        `authorId` TEXT NOT NULL,
                        `conversationId` TEXT,
                        `communityId` TEXT,
                        `question` TEXT NOT NULL,
                        `optionsJson` TEXT NOT NULL,
                        `pollType` TEXT NOT NULL,
                        `isAnonymous` INTEGER NOT NULL,
                        `isMultiChoice` INTEGER NOT NULL,
                        `isQuizMode` INTEGER NOT NULL,
                        `correctOptionIndex` INTEGER,
                        `totalVotes` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `expiresAt` INTEGER,
                        `version` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL,
                        PRIMARY KEY(`pollId`)
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_polls_authorId` ON `polls` (`authorId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_polls_conversationId` ON `polls` (`conversationId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_polls_createdAt` ON `polls` (`createdAt`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_polls_expiresAt` ON `polls` (`expiresAt`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `poll_votes` (
                        `voteId` TEXT NOT NULL,
                        `pollId` TEXT NOT NULL,
                        `optionId` TEXT NOT NULL,
                        `voterId` TEXT,
                        `timestamp` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL,
                        PRIMARY KEY(`voteId`)
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_poll_votes_pollId_voterId` ON `poll_votes` (`pollId`, `voterId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_poll_votes_pollId` ON `poll_votes` (`pollId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_poll_votes_optionId` ON `poll_votes` (`optionId`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `walkie_talkie_rooms` (
                        `roomId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `channelType` TEXT NOT NULL,
                        `channelNumber` INTEGER NOT NULL,
                        `creatorId` TEXT NOT NULL,
                        `memberIdsJson` TEXT NOT NULL,
                        `isEncrypted` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `currentSpeakerId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `lastActivityAt` INTEGER NOT NULL,
                        `meshAddress` TEXT,
                        PRIMARY KEY(`roomId`)
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_walkie_talkie_rooms_creatorId` ON `walkie_talkie_rooms` (`creatorId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_walkie_talkie_rooms_channelType` ON `walkie_talkie_rooms` (`channelType`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_walkie_talkie_rooms_channelNumber` ON `walkie_talkie_rooms` (`channelNumber`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_walkie_talkie_rooms_lastActivityAt` ON `walkie_talkie_rooms` (`lastActivityAt`)")

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `comments` (
                        `commentId` TEXT NOT NULL,
                        `postId` TEXT NOT NULL,
                        `authorId` TEXT NOT NULL,
                        `authorName` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `parentCommentId` TEXT,
                        `depth` INTEGER NOT NULL,
                        `upvotes` INTEGER NOT NULL,
                        `downvotes` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL,
                        PRIMARY KEY(`commentId`)
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_comments_postId` ON `comments` (`postId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_comments_authorId` ON `comments` (`authorId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_comments_parentCommentId` ON `comments` (`parentCommentId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_comments_timestamp` ON `comments` (`timestamp`)")
            }
        }
    }
}
