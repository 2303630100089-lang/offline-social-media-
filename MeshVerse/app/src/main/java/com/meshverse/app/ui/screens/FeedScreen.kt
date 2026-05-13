package com.meshverse.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshverse.app.domain.model.Comment
import com.meshverse.app.ui.viewmodel.MainViewModel

@Composable
fun FeedScreen(viewModel: MainViewModel) {
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    var postText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.ensureSeedData() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Local Social Feed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = postText,
                onValueChange = { postText = it },
                label = { Text("What's happening offline?") },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    viewModel.createPost(postText)
                    postText = ""
                },
                enabled = postText.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Post")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(feed, key = { it.postId }) { post ->
                PostCard(post = post, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun PostCard(post: com.meshverse.app.domain.model.Post, viewModel: MainViewModel) {
    var showComments by remember { mutableStateOf(false) }
    val comments by viewModel.getCommentsForPost(post.postId)
        .collectAsState(initial = emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            post.title?.let { title ->
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
            }
            Text(post.content, style = MaterialTheme.typography.bodyMedium)
            if (post.tags.isNotEmpty()) {
                Text(
                    post.tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            // Vote + Comment row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.upvotePost(post.postId) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Upvote",
                        tint = MaterialTheme.colorScheme.primary)
                }
                Text("${post.upvotes}", style = MaterialTheme.typography.labelLarge)

                IconButton(
                    onClick = { viewModel.downvotePost(post.postId) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Downvote",
                        tint = MaterialTheme.colorScheme.error)
                }
                Text("${post.downvotes}", style = MaterialTheme.typography.labelLarge)

                Spacer(Modifier.weight(1f))

                TextButton(onClick = { showComments = !showComments }) {
                    Icon(Icons.Default.Comment, contentDescription = "Comments",
                        modifier = Modifier.size(18.dp))
                    Text("  ${comments.size} comments")
                }
            }

            // Comments section
            AnimatedVisibility(visible = showComments) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Divider()
                    CommentSection(postId = post.postId, comments = comments, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun CommentSection(
    postId: String,
    comments: List<Comment>,
    viewModel: MainViewModel
) {
    var commentText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Top-level comments
        val topLevel = comments.filter { it.parentCommentId == null }
        topLevel.forEach { comment ->
            CommentItem(comment = comment, comments = comments, viewModel = viewModel, postId = postId)
        }

        // New comment input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                label = { Text("Add comment…") },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    viewModel.addComment(postId, commentText)
                    commentText = ""
                },
                enabled = commentText.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Submit comment")
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    comments: List<Comment>,
    viewModel: MainViewModel,
    postId: String,
    depth: Int = 0
) {
    var showReplyField by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    val replies = comments.filter { it.parentCommentId == comment.commentId }
    val indent = (depth * 16).dp

    Column(modifier = Modifier.padding(start = indent)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(comment.authorName, style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold)
                Text(comment.content, style = MaterialTheme.typography.bodySmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.upvoteComment(comment.commentId) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Upvote comment",
                            modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("${comment.upvotes}", style = MaterialTheme.typography.labelSmall)
                    IconButton(
                        onClick = { viewModel.downvoteComment(comment.commentId) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Downvote comment",
                            modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                    }
                    if (depth < 2) {
                        TextButton(
                            onClick = { showReplyField = !showReplyField },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = "Reply",
                                modifier = Modifier.size(14.dp))
                            Text(" Reply", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Reply input field
        AnimatedVisibility(visible = showReplyField) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Reply to ${comment.authorName}") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        viewModel.addComment(postId, replyText, comment.commentId)
                        replyText = ""
                        showReplyField = false
                    },
                    enabled = replyText.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send reply")
                }
            }
        }

        // Nested replies (up to depth 2)
        replies.forEach { reply ->
            CommentItem(
                comment = reply,
                comments = comments,
                viewModel = viewModel,
                postId = postId,
                depth = depth + 1
            )
        }
    }
}

