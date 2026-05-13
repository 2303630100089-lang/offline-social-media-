package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshverse.app.ui.theme.MeshGlassCard
import com.meshverse.app.ui.theme.MeshPrimaryButton
import com.meshverse.app.ui.viewmodel.MainViewModel

@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val user = viewModel.localUser.collectAsStateWithLifecycle().value
    var username by remember { mutableStateOf(user?.username ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        MeshGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Node: ${user?.userId ?: "not initialized"}", style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        MeshPrimaryButton(text = "Save offline identity", onClick = { viewModel.saveLocalIdentity(username) }, modifier = Modifier.fillMaxWidth())
    }
}
