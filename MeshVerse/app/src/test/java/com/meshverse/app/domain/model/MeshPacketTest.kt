package com.meshverse.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MeshPacketTest {

    @Test
    fun `packets with same packetId are equal regardless of payload differences`() {
        val first = MeshPacket(
            packetId = "same-id",
            sourceId = "a",
            destinationId = "b",
            senderId = "a",
            payloadType = PacketType.MESSAGE,
            payload = "hello".toByteArray(),
            ttl = 5
        )
        val second = MeshPacket(
            packetId = "same-id",
            sourceId = "x",
            destinationId = "y",
            senderId = "z",
            payloadType = PacketType.HEARTBEAT,
            payload = "different".toByteArray(),
            ttl = 1
        )

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `packets with different packetIds are not equal`() {
        val first = MeshPacket(
            packetId = "id-1",
            sourceId = "a",
            destinationId = "b",
            senderId = "a",
            payloadType = PacketType.MESSAGE,
            payload = byteArrayOf(1)
        )
        val second = first.copy(packetId = "id-2")

        assertNotEquals(first, second)
    }
}
