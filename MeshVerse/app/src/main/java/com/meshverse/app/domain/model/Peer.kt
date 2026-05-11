package com.meshverse.app.domain.model

data class Peer(
    val peerId: String,
    val deviceName: String,
    val userId: String?,
    val connectionType: ConnectionType,
    val signalStrength: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distance: Float? = null,
    val isConnected: Boolean = false,
    val isRelayNode: Boolean = false,
    val routingCost: Int = 1,
    val lastSeen: Long = System.currentTimeMillis(),
    val publicKey: String? = null,
    val capabilities: List<PeerCapability> = emptyList(),
    val batteryLevel: Int? = null,
    val reputation: Int = 100
)

enum class ConnectionType { BLE, BLUETOOTH, WIFI_DIRECT, NEARBY, LAN, NFC }
enum class PeerCapability { RELAY, GATEWAY, AI, MEDIA_CACHE }
