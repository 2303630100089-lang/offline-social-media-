package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshverse.app.media.MediaTransferManager
import com.meshverse.app.ui.theme.MeshGlassCard
import com.meshverse.app.ui.theme.MeshPrimaryButton
import com.meshverse.app.ui.viewmodel.MainViewModel
import com.meshverse.app.ui.viewmodel.MediaTransferManagerViewModel
import kotlinx.coroutines.launch

@Composable
fun MediaShareScreen(viewModel: MainViewModel) {
    val transferManager: MediaTransferManager = hiltViewModel<MediaTransferManagerViewModel>().manager
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Ready for chunked transfer") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("File Sharing", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        MeshPrimaryButton(text = "Send sample file", onClick = {
            coroutineScope.launch {
                val peerId = peers.firstOrNull { it.isConnected }?.peerId ?: "BROADCAST"
                val sampleData = "MeshVerse media payload".repeat(2048).toByteArray()
                val summary = transferManager.sendMedia(peerId, "sample.txt", "text/plain", sampleData)
                status = "Sent in ${summary.chunkCount} chunks with checksum ${summary.checksum.take(12)}..."
            }
        }, modifier = Modifier.fillMaxWidth())
        MeshGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(status)
        }
    }
}
