package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshverse.app.config.FeatureFlags
import com.meshverse.app.ui.theme.MeshGlassCard

@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        MeshGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Payments: Prototype only. Not real banking infrastructure.")
        }
        MeshGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Internet gateway mode: ${if (FeatureFlags.gatewayMode) "enabled" else "disabled"}")
        }
        MeshGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Gaming module: ${if (FeatureFlags.gamingModule) "enabled" else "disabled"}")
        }
    }
}
