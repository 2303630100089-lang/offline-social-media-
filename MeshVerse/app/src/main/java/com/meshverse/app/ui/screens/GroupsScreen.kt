package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshverse.app.domain.model.ConversationType
import com.meshverse.app.ui.viewmodel.MainViewModel

@Composable
fun GroupsScreen(viewModel: MainViewModel) {
    val groups = viewModel.conversations.collectAsStateWithLifecycle().value.filter {
        it.type == ConversationType.GROUP || it.type == ConversationType.COMMUNITY || it.type == ConversationType.CHANNEL
    }
    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Groups & Communities")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(groups) { group ->
                Card {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(group.name ?: group.conversationId)
                        Text(group.description ?: "Local community")
                    }
                }
            }
        }
    }
}
