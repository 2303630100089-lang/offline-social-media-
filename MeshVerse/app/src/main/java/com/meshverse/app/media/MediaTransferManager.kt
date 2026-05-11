package com.meshverse.app.media

import android.util.Base64
import com.google.gson.Gson
import com.meshverse.app.domain.model.MeshPacket
import com.meshverse.app.domain.model.PacketType
import com.meshverse.app.mesh.MeshNetworkManager
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaTransferManager @Inject constructor(
    private val meshNetworkManager: MeshNetworkManager,
    private val gson: Gson
) {

    data class ChunkEnvelope(
        val mediaId: String,
        val fileName: String,
        val mimeType: String,
        val chunkIndex: Int,
        val totalChunks: Int,
        val checksum: String,
        val dataBase64: String
    )

    data class TransferSummary(val chunkCount: Int, val checksum: String)

    fun chunkCount(data: ByteArray, chunkSize: Int = DEFAULT_CHUNK_SIZE): Int =
        (data.size + chunkSize - 1) / chunkSize

    fun sha256(data: ByteArray): String = MessageDigest
        .getInstance("SHA-256")
        .digest(data)
        .joinToString("") { "%02x".format(it) }

    suspend fun sendMedia(
        peerId: String,
        fileName: String,
        mimeType: String,
        data: ByteArray,
        chunkSize: Int = DEFAULT_CHUNK_SIZE
    ): TransferSummary {
        val total = chunkCount(data, chunkSize)
        val mediaId = UUID.randomUUID().toString()
        val checksum = sha256(data)
        val source = meshNetworkManager.getLocalNodeId().ifBlank { "local-node" }

        for (index in 0 until total) {
            val start = index * chunkSize
            val end = minOf(start + chunkSize, data.size)
            val chunk = data.copyOfRange(start, end)
            val envelope = ChunkEnvelope(
                mediaId = mediaId,
                fileName = fileName,
                mimeType = mimeType,
                chunkIndex = index,
                totalChunks = total,
                checksum = checksum,
                dataBase64 = Base64.encodeToString(chunk, Base64.NO_WRAP)
            )
            meshNetworkManager.sendPacket(
                MeshPacket(
                    sourceId = source,
                    destinationId = peerId,
                    senderId = source,
                    payloadType = PacketType.MEDIA_CHUNK,
                    payload = gson.toJson(envelope).toByteArray(),
                    ttl = 6
                )
            )
        }
        return TransferSummary(total, checksum)
    }

    companion object {
        const val DEFAULT_CHUNK_SIZE = 48 * 1024
    }
}
