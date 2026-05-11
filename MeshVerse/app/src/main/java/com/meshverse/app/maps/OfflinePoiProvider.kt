package com.meshverse.app.maps

data class OfflinePoi(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: String
)

object OfflinePoiProvider {
    fun defaultPois(): List<OfflinePoi> = listOf(
        OfflinePoi("City Hospital", 28.6129, 77.2295, "hospital"),
        OfflinePoi("Community Shelter", 28.6180, 77.2050, "shelter"),
        OfflinePoi("Fuel Station", 28.6201, 77.1987, "fuel"),
        OfflinePoi("Charging Point", 28.6079, 77.2142, "charging")
    )
}
