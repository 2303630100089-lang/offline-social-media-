package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey val peerId: String,
    val deviceName: String,
    val userId: String?,
    val connectionType: String,  // ble, bluetooth, wifi_direct, nearby, lan, nfc
    val signalStrength: Int = 0, // RSSI
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distance: Float? = null, // Estimated meters
    val isConnected: Boolean = false,
    val isRelayNode: Boolean = false,
    val routingCost: Int = 1,    // Routing metric (lower = prefer)
    val lastSeen: Long = System.currentTimeMillis(),
    val firstSeen: Long = System.currentTimeMillis(),
    val publicKey: String? = null,
    val capabilities: String? = null, // JSON list: ["relay","gateway","ai"]
    val batteryLevel: Int? = null,
    val reputation: Int = 100
)
