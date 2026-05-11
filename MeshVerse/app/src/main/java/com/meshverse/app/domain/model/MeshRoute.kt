package com.meshverse.app.domain.model

data class MeshRoute(
    val routeId: String,
    val destinationId: String,
    val nextHopId: String,
    val hopCount: Int,
    val routingCost: Int,
    val sequenceNumber: Int = 0,
    val isValid: Boolean = true,
    val expiresAt: Long,
    val packetDeliveryRate: Float = 1.0f
)
