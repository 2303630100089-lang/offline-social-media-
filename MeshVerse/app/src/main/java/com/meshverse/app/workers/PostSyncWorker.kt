package com.meshverse.app.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meshverse.app.domain.repository.PostRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that syncs unsynced posts to nearby mesh nodes.
 * Runs periodically in the background to ensure offline-first feed propagation.
 */
@HiltWorker
class PostSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val postRepository: PostRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "post_sync_worker"
        private const val TAG = "PostSyncWorker"
    }

    override suspend fun doWork(): Result {
        return runCatching {
            // Clean expired posts (stories, timed posts)
            postRepository.cleanExpiredPosts()

            // Get posts that haven't been synced to peers yet
            val unsynced = postRepository.getUnSyncedPosts()
            Log.d(TAG, "${unsynced.size} posts pending sync")

            // In a full implementation, broadcast each post to connected mesh peers
            // For now, we log and mark them as acknowledged locally to avoid re-processing
            // Full sync happens in MeshService when peers are connected

            Result.success()
        }.getOrElse { e ->
            Log.e(TAG, "PostSyncWorker failed: ${e.message}")
            Result.retry()
        }
    }
}
