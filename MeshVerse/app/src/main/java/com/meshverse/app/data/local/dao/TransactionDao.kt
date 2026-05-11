package com.meshverse.app.data.local.dao

import androidx.room.*
import com.meshverse.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity)

    @Update
    suspend fun update(tx: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE fromWallet = :walletId OR toWallet = :walletId ORDER BY timestamp DESC")
    fun getTransactionsForWallet(walletId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'pending' ORDER BY timestamp ASC")
    suspend fun getPendingTransactions(): List<TransactionEntity>
}
