package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshverse.app.ui.viewmodel.MainViewModel

@Composable
fun NearbyScreen(viewModel: MainViewModel) {
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    val localOnly by viewModel.isLocalOnlyMode.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (localOnly) "No nearby peers. Running in local-only mode." else "Nearby mesh peers")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(peers) { peer ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(peer.deviceName)
                        Text("${peer.connectionType} • cost ${peer.routingCost} • rep ${peer.reputation}")
                        Text(if (peer.isConnected) "Connected" else "Seen ${peer.lastSeen}")
                    }
                }
            }
        }
    }
}
