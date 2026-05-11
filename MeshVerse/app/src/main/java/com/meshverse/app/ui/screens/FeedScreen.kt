package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshverse.app.ui.viewmodel.MainViewModel

@Composable
fun FeedScreen(viewModel: MainViewModel) {
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    var postText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Local Social Feed")
        OutlinedTextField(
            value = postText,
            onValueChange = { postText = it },
            label = { Text("Create post") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            viewModel.createPost(postText)
            postText = ""
        }) { Text("Post offline") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(feed) { post ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(post.title ?: "Post")
                        Text(post.content)
                        Text("▲${post.upvotes} ▼${post.downvotes} ${post.tags.joinToString(" ")}")
                    }
                }
            }
        }
    }
}
