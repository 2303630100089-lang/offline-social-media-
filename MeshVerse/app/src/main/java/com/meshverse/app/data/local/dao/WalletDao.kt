package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wallet: WalletEntity)

    @Update
    suspend fun update(wallet: WalletEntity)

    @Query("SELECT * FROM wallet WHERE userId = :userId LIMIT 1")
    fun getWalletForUser(userId: String): Flow<WalletEntity?>
}
