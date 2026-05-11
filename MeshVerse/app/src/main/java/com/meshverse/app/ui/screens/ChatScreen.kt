package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshverse.app.ui.viewmodel.MainViewModel

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.ensureSeedData() }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Offline Messaging")
        if (conversations.isEmpty()) {
            Text("No conversations yet")
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(conversations) { conversation ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(conversation.name ?: conversation.conversationId)
                        Text(conversation.lastMessagePreview ?: "No messages")
                    }
                }
            }
        }

        val recipientId = peers.firstOrNull { it.isConnected }?.peerId ?: "BROADCAST"
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                viewModel.sendMessage("local-general", recipientId, message)
                message = ""
            }) {
                Text("Send")
            }
        }
    }
}
