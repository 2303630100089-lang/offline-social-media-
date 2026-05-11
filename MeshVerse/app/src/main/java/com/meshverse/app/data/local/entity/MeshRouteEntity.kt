package com.meshverse.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mesh_routes")
data class MeshRouteEntity(
    @PrimaryKey val routeId: String,
    val destinationId: String,       // Target peer/node
    val nextHopId: String,           // Next relay peer
    val hopCount: Int,
    val routingCost: Int,            // Lower = better
    val sequenceNumber: Int = 0,     // AODV sequence number
    val isValid: Boolean = true,
    val expiresAt: Long,             // Route expiry
    val discoveredAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis(),
    val packetDeliveryRate: Float = 1.0f // PDR metric
)
