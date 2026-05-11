package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Prototype offline wallet. NOT real banking infrastructure. */
@Entity(tableName = "wallet")
data class WalletEntity(
    @PrimaryKey val walletId: String,
    val userId: String,
    val balance: Double = 0.0,
    val currency: String = "MESH", // Prototype token
    val publicAddress: String,
    val encryptedPrivateKey: String,
    val createdAt: Long = System.currentTimeMillis()
)
