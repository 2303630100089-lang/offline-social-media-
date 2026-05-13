package com.meshverse.app.ai

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages offline AI model downloads and tracks which models are available on-device.
 *
 * Models are stored in: getExternalFilesDir(null)/models/<modelId>.gguf
 * The app detects them automatically on startup and when the user refreshes.
 *
 * Supported format: GGUF (llama.cpp compatible).
 * Integration: TFLite models (.tflite) are also supported for smaller on-device use.
 */
@Singleton
class AiModelManager @Inject constructor() {

    data class AiModelInfo(
        val id: String,
        val displayName: String,
        val description: String,
        val fileSizeMb: Int,
        val recommendedRamGb: Int,
        val downloadUrl: String,
        val fileName: String,
        val format: String = "gguf"  // gguf or tflite
    )

    enum class ModelStatus { NOT_DOWNLOADED, DOWNLOADING, READY }

    data class ModelState(
        val info: AiModelInfo,
        val status: ModelStatus = ModelStatus.NOT_DOWNLOADED,
        val downloadProgress: Int = 0
    )

    companion object {
        /** Well-known quantized models suitable for mobile devices. */
        val AVAILABLE_MODELS = listOf(
            AiModelInfo(
                id = "tinyllama-1.1b",
                displayName = "TinyLlama 1.1B",
                description = "Fast, small model. Great for basic Q&A and commands on any device.",
                fileSizeMb = 637,
                recommendedRamGb = 2,
                downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
                fileName = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"
            ),
            AiModelInfo(
                id = "phi-2",
                displayName = "Phi-2 (2.7B)",
                description = "Microsoft Phi-2. Excellent reasoning and code in a small package.",
                fileSizeMb = 1580,
                recommendedRamGb = 4,
                downloadUrl = "https://huggingface.co/TheBloke/phi-2-GGUF/resolve/main/phi-2.Q4_K_M.gguf",
                fileName = "phi-2.Q4_K_M.gguf"
            ),
            AiModelInfo(
                id = "gemma-2b",
                displayName = "Gemma 2B",
                description = "Google Gemma 2B. Good balance of quality and speed for mobile.",
                fileSizeMb = 1350,
                recommendedRamGb = 3,
                downloadUrl = "https://huggingface.co/google/gemma-2b-it-cpu-int4/resolve/main/gemma-2b-it-cpu-int4.bin",
                fileName = "gemma-2b-it-cpu-int4.bin",
                format = "tflite"
            ),
            AiModelInfo(
                id = "qwen-1.5-1.8b",
                displayName = "Qwen 1.5 (1.8B)",
                description = "Alibaba Qwen 1.5 1.8B chat model. Multilingual, good for general chat.",
                fileSizeMb = 1100,
                recommendedRamGb = 3,
                downloadUrl = "https://huggingface.co/Qwen/Qwen1.5-1.8B-Chat-GGUF/resolve/main/qwen1_5-1_8b-chat-q4_k_m.gguf",
                fileName = "qwen1_5-1_8b-chat-q4_k_m.gguf"
            ),
            AiModelInfo(
                id = "llama3-8b",
                displayName = "Llama 3 8B",
                description = "Meta Llama 3 8B. High-quality responses, requires 8GB+ RAM.",
                fileSizeMb = 4_800,
                recommendedRamGb = 8,
                downloadUrl = "https://huggingface.co/bartowski/Meta-Llama-3-8B-Instruct-GGUF/resolve/main/Meta-Llama-3-8B-Instruct-Q4_K_M.gguf",
                fileName = "Meta-Llama-3-8B-Instruct-Q4_K_M.gguf"
            )
        )
    }

    private val _modelStates = MutableStateFlow<List<ModelState>>(
        AVAILABLE_MODELS.map { ModelState(it) }
    )
    val modelStates: StateFlow<List<ModelState>> = _modelStates.asStateFlow()

    /** Refresh download status by checking if model files exist on disk. */
    fun refreshModelStatus(context: Context) {
        val modelsDir = getModelsDir(context)
        _modelStates.value = _modelStates.value.map { state ->
            val file = File(modelsDir, state.info.fileName)
            state.copy(
                status = if (file.exists() && file.length() > 0) ModelStatus.READY
                         else ModelStatus.NOT_DOWNLOADED,
                downloadProgress = if (file.exists()) 100 else 0
            )
        }
    }

    /**
     * Start downloading a model via Android DownloadManager.
     * The file is saved to the app's external files dir under /models/.
     */
    fun startDownload(context: Context, modelId: String): Long {
        val model = AVAILABLE_MODELS.find { it.id == modelId } ?: return -1L
        val modelsDir = getModelsDir(context)
        val destFile = File(modelsDir, model.fileName)

        if (destFile.exists()) return -1L  // already downloaded

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(model.downloadUrl)).apply {
            setTitle("Downloading ${model.displayName}")
            setDescription("AI model for offline use (${model.fileSizeMb} MB)")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, null, "models/${model.fileName}")
            setAllowedOverMetered(false)
            setAllowedOverRoaming(false)
        }

        updateStatus(modelId, ModelStatus.DOWNLOADING, 0)
        return dm.enqueue(request)
    }

    /** Query download progress (0-100) from DownloadManager for a given download ID. */
    fun queryProgress(context: Context, downloadId: Long, modelId: String) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        dm.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else 0L
                val downloaded = if (downloadedIdx >= 0) cursor.getLong(downloadedIdx) else 0L
                val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else 0
                val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        updateStatus(modelId, ModelStatus.READY, 100)
                    }
                    DownloadManager.STATUS_FAILED -> {
                        updateStatus(modelId, ModelStatus.NOT_DOWNLOADED, 0)
                    }
                    else -> updateStatus(modelId, ModelStatus.DOWNLOADING, progress)
                }
            }
        }
    }

    /** Delete a downloaded model file to free storage. */
    fun deleteModel(context: Context, modelId: String) {
        val model = AVAILABLE_MODELS.find { it.id == modelId } ?: return
        File(getModelsDir(context), model.fileName).delete()
        updateStatus(modelId, ModelStatus.NOT_DOWNLOADED, 0)
    }

    fun getModelFile(context: Context, modelId: String): File? {
        val model = AVAILABLE_MODELS.find { it.id == modelId } ?: return null
        val file = File(getModelsDir(context), model.fileName)
        return if (file.exists()) file else null
    }

    private fun updateStatus(modelId: String, status: ModelStatus, progress: Int) {
        _modelStates.value = _modelStates.value.map { state ->
            if (state.info.id == modelId) state.copy(status = status, downloadProgress = progress)
            else state
        }
    }

    private fun getModelsDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
