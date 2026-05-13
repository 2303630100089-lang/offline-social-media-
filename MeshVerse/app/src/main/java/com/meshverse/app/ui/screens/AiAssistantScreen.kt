package com.meshverse.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshverse.app.ai.AiModelManager
import com.meshverse.app.ai.FileContextLoader
import com.meshverse.app.ai.LocalAiAssistant
import com.meshverse.app.services.VoiceCallService
import com.meshverse.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AiAssistantScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiModelManager = remember { AiModelManager() }
    val modelStates by aiModelManager.modelStates.collectAsState()

    var prompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("Select a model below and type a command or question.") }
    var selectedModelId by remember { mutableStateOf<String?>(null) }
    var loadedFileName by remember { mutableStateOf<String?>(null) }
    var fileContext by remember { mutableStateOf<String?>(null) }
    var activeDownloadId by remember { mutableStateOf<Long?>(null) }
    var activeDownloadModelId by remember { mutableStateOf<String?>(null) }
    var showModels by remember { mutableStateOf(true) }

    // File picker
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val text = FileContextLoader.load(context, it)
            if (text != null) {
                fileContext = text
                loadedFileName = it.lastPathSegment ?: "document"
                response = "📄 Loaded: $loadedFileName\n\nFile contains ${text.length} characters. " +
                    "Ask me anything about this document, or type 'summarise' to get a summary."
            } else {
                response = "❌ Could not read file. Supported formats: TXT, CSV, JSON, MD."
            }
        }
    }

    // Poll download progress
    LaunchedEffect(activeDownloadId) {
        val dlId = activeDownloadId ?: return@LaunchedEffect
        val modelId = activeDownloadModelId ?: return@LaunchedEffect
        while (true) {
            aiModelManager.queryProgress(context, dlId, modelId)
            val state = aiModelManager.modelStates.value.find { it.info.id == modelId }
            if (state?.status == AiModelManager.ModelStatus.READY) {
                activeDownloadId = null
                activeDownloadModelId = null
                break
            }
            delay(2_000)
        }
    }

    LaunchedEffect(Unit) { aiModelManager.refreshModelStatus(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Offline AI Assistant", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)

        // Selected model indicator
        val selectedState = modelStates.find { it.info.id == selectedModelId }
        if (selectedState?.status == AiModelManager.ModelStatus.READY) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    "✅ Active model: ${selectedState.info.displayName}",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // File context indicator
        fileContext?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📄 $loadedFileName", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { fileContext = null; loadedFileName = null }) {
                        Text("Clear")
                    }
                }
            }
        }

        // Prompt input
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Ask anything or type a command") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Attach file button
            Button(
                onClick = { filePicker.launch("*/*") },
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach file",
                    modifier = Modifier.padding(end = 4.dp))
                Text("Load File")
            }

            // Run button
            Button(
                onClick = {
                    val inputPrompt = prompt.trim()
                    if (inputPrompt.isBlank()) return@Button

                    scope.launch {
                        // Build prompt with optional file context
                        val fullPrompt = if (fileContext != null) {
                            if (inputPrompt.lowercase() == "summarise" || inputPrompt.lowercase() == "summarize") {
                                FileContextLoader.buildSummaryPrompt(fileContext!!)
                            } else {
                                FileContextLoader.buildContextPrompt(fileContext!!, inputPrompt)
                            }
                        } else inputPrompt

                        // Route command or simulate AI response
                        val command = LocalAiAssistant.routeCommand(inputPrompt)
                        response = when (command.action) {
                            LocalAiAssistant.Action.SEND_SOS -> {
                                viewModel.sendSos(command.message)
                                "🆘 SOS broadcast queued in mesh network."
                            }
                            LocalAiAssistant.Action.FIND_HOSPITAL -> {
                                "🏥 Nearest offline hospital POIs are shown on the Map screen."
                            }
                            LocalAiAssistant.Action.START_WALKIE_TALKIE -> {
                                context.startForegroundService(
                                    Intent(context, VoiceCallService::class.java).apply {
                                        action = VoiceCallService.ACTION_START_CALL
                                    }
                                )
                                "📻 Walkie-talkie service started."
                            }
                            LocalAiAssistant.Action.OPEN_MARKETPLACE -> {
                                "🛒 Marketplace is available in the MiniApps tab."
                            }
                            LocalAiAssistant.Action.UNKNOWN -> {
                                val activeModel = selectedModelId?.let { id ->
                                    modelStates.find { it.info.id == id && it.status == AiModelManager.ModelStatus.READY }
                                }
                                if (activeModel != null) {
                                    // In a real app, run llama.cpp JNI or TFLite inference here
                                    // For now return a contextual placeholder
                                    withContext(Dispatchers.IO) {
                                        simulateInference(fullPrompt, activeModel.info.displayName)
                                    }
                                } else {
                                    "⚠️ No model loaded. Download a model below and select it to enable AI responses."
                                }
                            }
                        }
                        prompt = ""
                    }
                },
                enabled = prompt.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Run",
                    modifier = Modifier.padding(end = 4.dp))
                Text("Ask AI")
            }
        }

        // Response area
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                response,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Divider()

        // Model management section
        TextButton(onClick = { showModels = !showModels }) {
            Text(if (showModels) "▼ Available Models" else "▶ Available Models",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        AnimatedVisibility(visible = showModels) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Models are stored in the app's external storage under /models/. " +
                    "You can also manually place .gguf files there.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                modelStates.forEach { state ->
                    ModelCard(
                        state = state,
                        isSelected = selectedModelId == state.info.id,
                        onSelect = { if (state.status == AiModelManager.ModelStatus.READY) selectedModelId = state.info.id },
                        onDownload = {
                            val id = aiModelManager.startDownload(context, state.info.id)
                            if (id > 0) {
                                activeDownloadId = id
                                activeDownloadModelId = state.info.id
                            }
                        },
                        onDelete = { aiModelManager.deleteModel(context, state.info.id) }
                    )
                }

                // Setup instructions
                Spacer(Modifier.height(8.dp))
                Text("Manual Model Setup", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("1. Download a .gguf model file from HuggingFace (TheBloke / bartowski repos)",
                            style = MaterialTheme.typography.bodySmall)
                        Text("2. Connect your phone via USB and copy the file to:",
                            style = MaterialTheme.typography.bodySmall)
                        Text("   Android/data/com.meshverse.app/files/models/",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold)
                        Text("3. Return to this screen — the model will appear as Ready",
                            style = MaterialTheme.typography.bodySmall)
                        Text("4. Tap the model card to select it, then ask questions",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Text("For file-based AI context:", style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold)
                        Text("• Tap 'Load File' to open a TXT, CSV, JSON, or Markdown file",
                            style = MaterialTheme.typography.bodySmall)
                        Text("• Type 'summarise' to get a document summary",
                            style = MaterialTheme.typography.bodySmall)
                        Text("• Ask any question to get an answer grounded in the document",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelCard(
    state: AiModelManager.ModelState,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val info = state.info
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(info.displayName, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    Text(info.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Size: ${info.fileSizeMb} MB  •  RAM: ${info.recommendedRamGb}GB+  •  ${info.format.uppercase()}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Row {
                    when (state.status) {
                        AiModelManager.ModelStatus.NOT_DOWNLOADED -> {
                            IconButton(onClick = onDownload) {
                                Icon(Icons.Default.Download, contentDescription = "Download",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        AiModelManager.ModelStatus.DOWNLOADING -> {
                            // Show progress below
                        }
                        AiModelManager.ModelStatus.READY -> {
                            if (!isSelected) {
                                Button(onClick = onSelect) { Text("Use") }
                            }
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Download progress bar
            if (state.status == AiModelManager.ModelStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = state.downloadProgress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Downloading… ${state.downloadProgress}%",
                    style = MaterialTheme.typography.labelSmall)
            }

            if (isSelected) {
                Text("✅ Selected", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Placeholder inference function.
 * In a production build, replace with a llama.cpp JNI call or TFLite interpreter:
 *
 *   // llama.cpp (via android-llama.cpp or similar JNI wrapper):
 *   val ctx = LlamaContext.create(modelPath)
 *   return ctx.completion(prompt, maxTokens = 256)
 *
 *   // TFLite:
 *   val interpreter = Interpreter(modelFile)
 *   // Tokenise prompt → run inference → detokenise output
 */
private fun simulateInference(prompt: String, modelName: String): String {
    return "[$modelName is loaded and ready]\n\n" +
        "To enable live AI inference, integrate the llama.cpp Android JNI bindings " +
        "(github.com/ggerganov/llama.cpp) or the TFLite interpreter for .tflite models.\n\n" +
        "Your prompt:\n\"$prompt\"\n\n" +
        "Once the inference engine is wired up, responses will appear here."
}

