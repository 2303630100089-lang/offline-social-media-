package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshverse.app.domain.model.ConversationType
import com.meshverse.app.ui.theme.MeshGlassCard
import com.meshverse.app.ui.viewmodel.MainViewModel

@Composable
fun GroupsScreen(viewModel: MainViewModel) {
    val groups = viewModel.conversations.collectAsStateWithLifecycle().value.filter {
        it.type == ConversationType.GROUP || it.type == ConversationType.COMMUNITY || it.type == ConversationType.CHANNEL
    }
    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mesh Rooms", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(groups) { group ->
                MeshGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(group.name ?: group.conversationId)
                        Text(
                            group.description ?: "Local community",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
