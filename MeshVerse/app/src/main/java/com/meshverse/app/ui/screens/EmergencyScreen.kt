package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshverse.app.ui.viewmodel.MainViewModel

@Composable
fun EmergencyScreen(viewModel: MainViewModel) {
    var message by remember { mutableStateOf("Need assistance") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Emergency Mesh Beacon")
        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("SOS message") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.sendSos(message) }) { Text("Broadcast SOS") }
        Text("SOS packets are encrypted, broadcast over mesh, and forwarded with TTL.")
    }
}
