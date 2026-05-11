package com.meshverse.app.domain.model

import java.util.UUID

/**
 * A community-generated map report that propagates over the mesh network.
 * Examples: "Road blocked", "Food available", "Medical help here".
 * Reports propagate as MAP_REPORT packets and expire automatically.
 */
data class MapReport(
    val reportId: String = UUID.randomUUID().toString(),
    val authorId: String,
    val category: ReportCategory,
    val title: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L, // 24h default
    val upvotes: Int = 0,
    val isVerified: Boolean = false,
    /** Number of mesh hops this report has travelled. */
    val propagationHops: Int = 0
)

enum class ReportCategory(val emoji: String, val label: String) {
    ROAD_BLOCKED("🚧", "Road Blocked"),
    FOOD_AVAILABLE("🍱", "Food Available"),
    MEDICAL_HELP("🏥", "Medical Help"),
    POLICE_CHECKPOINT("🚔", "Police Checkpoint"),
    CHARGING_AVAILABLE("🔋", "Charging Available"),
    SHELTER("🏠", "Shelter"),
    WATER_AVAILABLE("💧", "Water Available"),
    DANGER("⚠️", "Danger"),
    INTERNET_AVAILABLE("📶", "Internet Available"),
    FUEL_AVAILABLE("⛽", "Fuel Available"),
    GENERAL("📍", "General")
}
