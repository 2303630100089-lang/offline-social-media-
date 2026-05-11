package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshverse.app.config.FeatureFlags

@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Settings")
        Card { Text("Payments: Prototype only. Not real banking infrastructure.", modifier = Modifier.padding(12.dp)) }
        Card { Text("Internet gateway mode: ${if (FeatureFlags.gatewayMode) "enabled" else "disabled"}", modifier = Modifier.padding(12.dp)) }
        Card { Text("Gaming module: ${if (FeatureFlags.gamingModule) "enabled" else "disabled"}", modifier = Modifier.padding(12.dp)) }
    }
}
