package com.meshverse.app.ui.screens

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.meshverse.app.ai.LocalAiAssistant
import com.meshverse.app.services.VoiceCallService
import com.meshverse.app.ui.viewmodel.MainViewModel

@Composable
fun AiAssistantScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("Try: Send SOS | Find nearby hospital | Start walkie-talkie") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Offline AI Assistant")
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Command") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            val command = LocalAiAssistant.routeCommand(prompt)
            when (command.action) {
                LocalAiAssistant.Action.SEND_SOS -> {
                    viewModel.sendSos(command.message)
                    response = "SOS broadcast queued in mesh"
                }
                LocalAiAssistant.Action.FIND_HOSPITAL -> {
                    response = "Nearest offline hospital POIs shown on map screen"
                }
                LocalAiAssistant.Action.START_WALKIE_TALKIE -> {
                    context.startForegroundService(
                        Intent(context, VoiceCallService::class.java).apply {
                            action = VoiceCallService.ACTION_START_CALL
                        }
                    )
                    response = "Walkie-talkie service started"
                }
                LocalAiAssistant.Action.OPEN_MARKETPLACE -> response = "Marketplace mini app is available in MiniApps tab"
                LocalAiAssistant.Action.UNKNOWN -> response = "Offline assistant can execute safety, map and mesh commands"
            }
            prompt = ""
        }) { Text("Run command") }
        Text(response)
    }
}
