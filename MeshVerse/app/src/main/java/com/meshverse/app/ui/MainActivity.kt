package com.meshverse.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.meshverse.app.startup.AppStartupManager
import com.meshverse.app.startup.StartupPermissionState
import com.meshverse.app.ui.navigation.MeshVerseNavGraph
import com.meshverse.app.ui.theme.MeshVerseTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class MainActivityUiState(
    val onboardingCompleted: Boolean = false,
    val permissionState: StartupPermissionState = StartupPermissionState(
        missingStartupPermissions = emptyList(),
        missingOptionalPermissions = emptyList(),
        backgroundLocationGranted = false,
        overlayGranted = false
    ),
    val isInitializing: Boolean = false,
    val isReady: Boolean = false,
    val meshActive: Boolean = false,
    val startupWarnings: List<String> = emptyList(),
    val startupError: String? = null
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    @Inject
    lateinit var appStartupManager: AppStartupManager

    private var uiState by mutableStateOf(MainActivityUiState())
    private var startupJob: Job? = null

    private val runtimePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshStartupState(force = true)
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshStartupState(force = true)
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshStartupState(force = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        uiState = uiState.copy(permissionState = appStartupManager.createPermissionState())

        lifecycleScope.launch {
            appStartupManager.onboardingCompleted.collectLatest { completed ->
                uiState = uiState.copy(onboardingCompleted = completed)
                refreshStartupState(force = true)
            }
        }

        setContent {
            MeshVerseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainActivityContent(
                        state = uiState,
                        onRequestStartupPermissions = ::requestStartupPermissions,
                        onRequestOptionalPermissions = ::requestOptionalPermissions,
                        onRequestBackgroundLocation = ::requestBackgroundLocation,
                        onRequestOverlayPermission = ::requestOverlayPermission,
                        onContinueAfterOnboarding = ::completeOnboarding,
                        onRetryStartup = { refreshStartupState(force = true) }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshStartupState(force = true)
    }

    private fun requestStartupPermissions() {
        val permissions = uiState.permissionState.missingStartupPermissions
        if (permissions.isNotEmpty()) {
            runtimePermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun requestOptionalPermissions() {
        val permissions = uiState.permissionState.missingOptionalPermissions
        if (permissions.isNotEmpty()) {
            runtimePermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            Manifest.permission.ACCESS_FINE_LOCATION !in uiState.permissionState.missingStartupPermissions &&
            !uiState.permissionState.backgroundLocationGranted
        ) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !uiState.permissionState.overlayGranted
        ) {
            runCatching {
                overlayPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }.onFailure { throwable ->
                Log.e(TAG, "Unable to request overlay permission", throwable)
            }
        }
    }

    private fun completeOnboarding() {
        lifecycleScope.launch {
            appStartupManager.markOnboardingComplete()
        }
    }

    private fun refreshStartupState(force: Boolean = false) {
        val permissionState = appStartupManager.createPermissionState()
        uiState = uiState.copy(permissionState = permissionState)

        if (!uiState.onboardingCompleted) {
            uiState = uiState.copy(
                isInitializing = false,
                isReady = false,
                startupError = null
            )
            return
        }

        if (startupJob?.isActive == true && !force) return

        startupJob?.cancel()
        startupJob = lifecycleScope.launch {
            uiState = uiState.copy(isInitializing = true, startupError = null)
            val startupResult = appStartupManager.initialize(permissionState)
            uiState = uiState.copy(
                isInitializing = false,
                isReady = startupResult.ready,
                meshActive = startupResult.meshActive,
                startupWarnings = startupResult.warnings,
                startupError = startupResult.fatalError
            )
        }
    }
}

@Composable
private fun MainActivityContent(
    state: MainActivityUiState,
    onRequestStartupPermissions: () -> Unit,
    onRequestOptionalPermissions: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onContinueAfterOnboarding: () -> Unit,
    onRetryStartup: () -> Unit
) {
    when {
        !state.onboardingCompleted -> StartupOnboardingScreen(
            state = state,
            onRequestStartupPermissions = onRequestStartupPermissions,
            onRequestOptionalPermissions = onRequestOptionalPermissions,
            onRequestBackgroundLocation = onRequestBackgroundLocation,
            onRequestOverlayPermission = onRequestOverlayPermission,
            onContinue = onContinueAfterOnboarding
        )

        state.startupError != null -> StartupErrorScreen(
            error = state.startupError,
            warnings = state.startupWarnings,
            onRetryStartup = onRetryStartup
        )

        state.isReady -> Column(modifier = Modifier.fillMaxSize()) {
            if (state.startupWarnings.isNotEmpty() || !state.meshActive || state.permissionState.hasOptionalFeaturesDisabled) {
                StartupWarningBanner(
                    state = state,
                    onRequestStartupPermissions = onRequestStartupPermissions,
                    onRequestOptionalPermissions = onRequestOptionalPermissions,
                    onRequestBackgroundLocation = onRequestBackgroundLocation,
                    onRequestOverlayPermission = onRequestOverlayPermission
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                MeshVerseNavGraph()
            }
        }

        else -> StartupLoadingScreen()
    }
}

@Composable
private fun StartupOnboardingScreen(
    state: MainActivityUiState,
    onRequestStartupPermissions: () -> Unit,
    onRequestOptionalPermissions: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome to MeshVerse", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Review permissions before startup. You can continue even if you deny optional access; the app will fall back to local-only mode where needed.")

        PermissionSection(
            title = "Core startup permissions",
            description = "Needed before mesh scanning, nearby discovery, and foreground sync start.",
            missingPermissions = state.permissionState.missingStartupPermissions,
            onGrant = onRequestStartupPermissions,
            buttonLabel = "Grant core permissions"
        )

        PermissionSection(
            title = "Optional media permissions",
            description = "Camera, microphone, and media access stay disabled until granted.",
            missingPermissions = state.permissionState.missingOptionalPermissions,
            onGrant = onRequestOptionalPermissions,
            buttonLabel = "Grant optional permissions"
        )

        PermissionToggleCard(
            title = "Background location",
            granted = state.permissionState.backgroundLocationGranted,
            description = "Needed for location-aware mesh behavior while the app is not visible on Android 10+.",
            actionLabel = "Grant background location",
            onGrant = onRequestBackgroundLocation
        )

        PermissionToggleCard(
            title = "Overlay permission",
            granted = state.permissionState.overlayGranted,
            description = "Required before any bubble or overlay UI is shown.",
            actionLabel = "Grant overlay access",
            onGrant = onRequestOverlayPermission
        )

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue to app")
        }
    }
}

@Composable
private fun StartupLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("Preparing MeshVerse safely…")
        }
    }
}

@Composable
private fun StartupErrorScreen(
    error: String,
    warnings: List<String>,
    onRetryStartup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Startup failed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(error)
        warnings.forEach { warning ->
            Text("• $warning")
        }
        Button(onClick = onRetryStartup, modifier = Modifier.fillMaxWidth()) {
            Text("Retry startup")
        }
    }
}

@Composable
private fun StartupWarningBanner(
    state: MainActivityUiState,
    onRequestStartupPermissions: () -> Unit,
    onRequestOptionalPermissions: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (state.meshActive) "MeshVerse started with limited features" else "MeshVerse is running in local-only fallback mode",
            fontWeight = FontWeight.SemiBold
        )

        state.startupWarnings.forEach { warning ->
            Text("• $warning", style = MaterialTheme.typography.bodySmall)
        }

        if (state.permissionState.missingStartupPermissions.isNotEmpty()) {
            OutlinedButton(onClick = onRequestStartupPermissions) {
                Text("Enable mesh permissions")
            }
        }

        if (state.permissionState.missingOptionalPermissions.isNotEmpty()) {
            OutlinedButton(onClick = onRequestOptionalPermissions) {
                Text("Enable media permissions")
            }
        }

        if (!state.permissionState.backgroundLocationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            OutlinedButton(onClick = onRequestBackgroundLocation) {
                Text("Enable background location")
            }
        }

        if (!state.permissionState.overlayGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            OutlinedButton(onClick = onRequestOverlayPermission) {
                Text("Enable overlay access")
            }
        }
    }
}

@Composable
private fun PermissionSection(
    title: String,
    description: String,
    missingPermissions: List<String>,
    onGrant: () -> Unit,
    buttonLabel: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall)
        if (missingPermissions.isEmpty()) {
            Text("Already granted")
        } else {
            missingPermissions.forEach { permission ->
                Text("• ${permissionLabel(permission)}")
            }
            OutlinedButton(onClick = onGrant) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun PermissionToggleCard(
    title: String,
    granted: Boolean,
    description: String,
    actionLabel: String,
    onGrant: () -> Unit
) {
    val currentGrantState by rememberUpdatedState(granted)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall)
        Text(if (currentGrantState) "Granted" else "Not granted")
        if (!currentGrantState) {
            OutlinedButton(onClick = onGrant) {
                Text(actionLabel)
            }
        }
    }
}

private fun permissionLabel(permission: String): String = when (permission) {
    Manifest.permission.ACCESS_FINE_LOCATION -> "Precise location"
    Manifest.permission.BLUETOOTH_SCAN -> "Bluetooth scan"
    Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth connect"
    Manifest.permission.NEARBY_WIFI_DEVICES -> "Nearby Wi-Fi devices"
    Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
    Manifest.permission.CAMERA -> "Camera"
    Manifest.permission.RECORD_AUDIO -> "Microphone"
    Manifest.permission.READ_MEDIA_IMAGES -> "Photos"
    Manifest.permission.READ_MEDIA_VIDEO -> "Videos"
    Manifest.permission.READ_MEDIA_AUDIO -> "Audio files"
    Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage"
    else -> permission
}
