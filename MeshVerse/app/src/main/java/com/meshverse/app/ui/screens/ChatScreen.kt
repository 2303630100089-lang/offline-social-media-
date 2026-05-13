package com.meshverse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshverse.app.ui.theme.MeshGlassCard
import com.meshverse.app.ui.viewmodel.MainViewModel

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.ensureSeedData() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Chats", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conversations) { conversation ->
                MeshGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = (conversation.name ?: "M").take(1).uppercase(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(conversation.name ?: conversation.conversationId, fontWeight = FontWeight.SemiBold)
                            Text(
                                conversation.lastMessagePreview ?: "No messages yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        val recipientId = peers.firstOrNull { it.isConnected }?.peerId ?: "BROADCAST"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = { Text("Type a message") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            FloatingActionButton(
                onClick = {
                    viewModel.sendMessage("local-general", recipientId, message)
                    message = ""
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}
