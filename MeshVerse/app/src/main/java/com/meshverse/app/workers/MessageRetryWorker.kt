package com.meshverse.app.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meshverse.app.domain.repository.MessageRepository
import com.meshverse.app.mesh.MeshNetworkManager
import com.google.gson.Gson
import com.meshverse.app.domain.model.MeshPacket
import com.meshverse.app.domain.model.PacketType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that retries pending messages.
 * Scheduled periodically so messages are not lost if the UI is killed.
 * Runs even when the app is in the background or after device restart.
 */
@HiltWorker
class MessageRetryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageRepository: MessageRepository,
    private val meshNetworkManager: MeshNetworkManager,
    private val gson: Gson
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "message_retry_worker"
        private const val TAG = "MessageRetryWorker"
    }

    override suspend fun doWork(): Result {
        return runCatching {
            val pendingMessages = messageRepository.getPendingMessages()
            Log.d(TAG, "Retrying ${pendingMessages.size} pending messages")

            pendingMessages.forEach { message ->
                runCatching {
                    val packet = MeshPacket(
                        sourceId = message.senderId,
                        destinationId = message.recipientId,
                        senderId = message.senderId,
                        payloadType = PacketType.MESSAGE,
                        payload = gson.toJson(message).toByteArray()
                    )
                    meshNetworkManager.sendPacket(packet)
                }.onFailure { e ->
                    Log.w(TAG, "Failed to retry message ${message.messageId}: ${e.message}")
                }
            }
            Result.success()
        }.getOrElse { e ->
            Log.e(TAG, "MessageRetryWorker failed: ${e.message}")
            Result.retry()
        }
    }
}
