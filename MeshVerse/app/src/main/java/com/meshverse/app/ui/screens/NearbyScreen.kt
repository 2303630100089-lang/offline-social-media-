package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshverse.app.ui.theme.MeshGlassCard
import com.meshverse.app.ui.theme.MeshPrimaryButton
import com.meshverse.app.ui.viewmodel.MainViewModel

@Composable
fun NearbyScreen(viewModel: MainViewModel) {
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    val localOnly by viewModel.isLocalOnlyMode.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Home Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        MeshGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (localOnly) "You are Offline" else "Mesh Active",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (localOnly) "Using local-only mode with no peers nearby." else "Nearby peers are available for relay and chat.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MeshPrimaryButton(
                        text = "Broadcast SOS",
                        onClick = { viewModel.sendSos("Emergency ping from home dashboard") }
                    )
                }
            }
        }

        Text("Nearby Devices", fontWeight = FontWeight.SemiBold)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(peers) { peer ->
                MeshGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(peer.deviceName, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${peer.connectionType} • cost ${peer.routingCost}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (peer.isConnected) "Online" else "Seen",
                            color = if (peer.isConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
