package com.meshverse.app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.meshverse.app.services.MeshService
import com.meshverse.app.services.SyncService
import com.meshverse.app.ui.navigation.MeshVerseNavGraph
import com.meshverse.app.ui.theme.MeshVerseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start mesh and sync services
        safeStartForegroundService(
            Intent(this, MeshService::class.java).apply {
                action = MeshService.ACTION_START
            }
        )
        safeStartForegroundService(Intent(this, SyncService::class.java))

        setContent {
            MeshVerseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MeshVerseNavGraph()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Keep services running in background even when activity is destroyed
    }

    private fun safeStartForegroundService(intent: Intent) {
        runCatching {
            ContextCompat.startForegroundService(this, intent)
        }.onFailure { throwable ->
            Log.e(TAG, "Unable to start foreground service: ${intent.component?.className}", throwable)
        }
    }
}
