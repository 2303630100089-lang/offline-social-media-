package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Prototype offline transaction. NOT real banking infrastructure. */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val txId: String,
    val fromWallet: String,
    val toWallet: String,
    val amount: Double,
    val currency: String = "MESH",
    val status: String = "pending", // pending, confirmed, failed
    val signature: String,          // Ed25519 signed receipt
    val timestamp: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val note: String? = null
)
