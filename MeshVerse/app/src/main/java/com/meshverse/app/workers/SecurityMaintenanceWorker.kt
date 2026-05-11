package com.meshverse.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meshverse.app.security.KeyManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SecurityMaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val keyManager: KeyManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        keyManager.rotateIdentityKeyPair()
        return Result.success()
    }
}
