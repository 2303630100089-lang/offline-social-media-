package com.meshverse.app.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePoiProviderTest {

    @Test
    fun `defaultPois returns expected offline POIs`() {
        val pois = OfflinePoiProvider.defaultPois()

        assertEquals(4, pois.size)
        assertEquals(
            setOf("City Hospital", "Community Shelter", "Fuel Station", "Charging Point"),
            pois.map { it.name }.toSet()
        )
        assertEquals(setOf("hospital", "shelter", "fuel", "charging"), pois.map { it.category }.toSet())

        pois.forEach { poi ->
            assertTrue(poi.latitude in -90.0..90.0)
            assertTrue(poi.longitude in -180.0..180.0)
        }
    }
}
