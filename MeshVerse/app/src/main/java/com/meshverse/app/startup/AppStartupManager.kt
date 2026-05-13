package com.meshverse.app.startup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.meshverse.app.ai.LocalAiAssistant
import com.meshverse.app.data.local.MeshVerseDatabase
import com.meshverse.app.services.MeshService
import com.meshverse.app.services.SyncService
import com.meshverse.app.workers.SecurityMaintenanceWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private val Context.startupDataStore by preferencesDataStore(name = "startup_prefs")

data class StartupPermissionState(
    val missingStartupPermissions: List<String>,
    val missingOptionalPermissions: List<String>,
    val backgroundLocationGranted: Boolean,
    val overlayGranted: Boolean
) {
    val canStartMeshServices: Boolean
        get() = missingStartupPermissions.isEmpty()

    val hasOptionalFeaturesDisabled: Boolean
        get() = missingOptionalPermissions.isNotEmpty() || !backgroundLocationGranted || !overlayGranted
}

data class StartupInitializationResult(
    val ready: Boolean,
    val meshActive: Boolean,
    val warnings: List<String> = emptyList(),
    val fatalError: String? = null
)

@Singleton
class AppStartupManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val databaseProvider: Provider<MeshVerseDatabase>
) {
    companion object {
        private const val TAG = "AppStartupManager"
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    private val startupMutex = Mutex()
    private var meshServiceStarted = false
    private var syncServiceStarted = false
    private var securityWorkScheduled = false

    val onboardingCompleted: Flow<Boolean> = appContext.startupDataStore.data
        .map { preferences -> preferences[ONBOARDING_COMPLETED] ?: false }

    suspend fun markOnboardingComplete() {
        appContext.startupDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = true
        }
    }

    fun createPermissionState(): StartupPermissionState {
        val startupPermissions = requiredStartupPermissions()
        val optionalPermissions = optionalFeaturePermissions()
        val backgroundLocationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        val overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Settings.canDrawOverlays(appContext)

        return StartupPermissionState(
            missingStartupPermissions = startupPermissions.filterNot(::hasPermission),
            missingOptionalPermissions = optionalPermissions.filterNot(::hasPermission),
            backgroundLocationGranted = backgroundLocationGranted,
            overlayGranted = overlayGranted
        )
    }

    fun requiredStartupPermissions(): List<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun optionalFeaturePermissions(): List<String> = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    suspend fun initialize(permissionState: StartupPermissionState): StartupInitializationResult =
        startupMutex.withLock {
            val warnings = linkedSetOf<String>()
            delay(300)

            val databaseReady = warmDatabase(warnings)
            if (!databaseReady) {
                return StartupInitializationResult(
                    ready = false,
                    meshActive = false,
                    warnings = warnings.toList(),
                    fatalError = "Local database initialization failed. Retry startup or clear app data if the issue persists."
                )
            }

            prepareAppStorage(warnings)
            warmAiEngine(warnings)
            scheduleSecurityMaintenance(warnings)

            if (!hasInternetConnection()) {
                warnings += "Internet relay is unavailable. MeshVerse will continue in offline-first mode."
            }

            val meshActive = startForegroundServices(permissionState, warnings)
            StartupInitializationResult(
                ready = true,
                meshActive = meshActive,
                warnings = warnings.toList()
            )
        }

    private fun warmDatabase(warnings: MutableSet<String>): Boolean =
        runCatching {
            databaseProvider.get().openHelper.writableDatabase.query("SELECT 1").use { }
        }.onFailure { throwable ->
            Log.e(TAG, "Database warmup failed", throwable)
            warnings += "Local data access is degraded until the database can be reopened safely."
        }.isSuccess

    private fun prepareAppStorage(warnings: MutableSet<String>) {
        runCatching {
            File(appContext.filesDir, "meshverse/startup").mkdirs()
            File(appContext.cacheDir, "meshverse").mkdirs()
        }.onFailure { throwable ->
            Log.e(TAG, "Unable to prepare app storage", throwable)
            warnings += "Some offline file features are temporarily unavailable."
        }
    }

    private fun warmAiEngine(warnings: MutableSet<String>) {
        runCatching {
            LocalAiAssistant.routeCommand("startup")
        }.onFailure { throwable ->
            Log.e(TAG, "Local AI warmup failed", throwable)
            warnings += "Local AI assistant is unavailable right now. The rest of the app can still run."
        }
    }

    private fun scheduleSecurityMaintenance(warnings: MutableSet<String>) {
        if (securityWorkScheduled) return

        runCatching {
            val request = PeriodicWorkRequestBuilder<SecurityMaintenanceWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                "security-maintenance",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            securityWorkScheduled = true
        }.onFailure { throwable ->
            Log.e(TAG, "Unable to schedule security maintenance", throwable)
            warnings += "Background maintenance is paused until WorkManager becomes available."
        }
    }

    private fun startForegroundServices(
        permissionState: StartupPermissionState,
        warnings: MutableSet<String>
    ): Boolean {
        if (!permissionState.canStartMeshServices) {
            warnings += "Bluetooth, nearby, location, or notification access is missing. Mesh services will stay paused until those permissions are granted."
            return false
        }

        val meshStartedNow = if (meshServiceStarted) {
            true
        } else {
            startForegroundService(
                Intent(appContext, MeshService::class.java).apply {
                    action = MeshService.ACTION_START
                },
                "mesh network",
                warnings
            ).also { started -> meshServiceStarted = started }
        }

        val syncStartedNow = if (syncServiceStarted) {
            true
        } else {
            startForegroundService(
                Intent(appContext, SyncService::class.java),
                "background sync",
                warnings
            ).also { started -> syncServiceStarted = started }
        }

        return meshStartedNow && syncStartedNow
    }

    private fun startForegroundService(
        intent: Intent,
        label: String,
        warnings: MutableSet<String>
    ): Boolean = runCatching {
        ContextCompat.startForegroundService(appContext, intent)
        true
    }.getOrElse { throwable ->
        Log.e(TAG, "Unable to start $label service", throwable)
        warnings += "Unable to start $label services. MeshVerse will continue in local-only mode."
        false
    }

    private fun hasInternetConnection(): Boolean = runCatching {
        val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
        connectivityManager?.activeNetwork != null
    }.getOrDefault(false)

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
}
