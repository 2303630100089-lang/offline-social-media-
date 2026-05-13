package com.meshverse.app.ai

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loads text content from local files (TXT, CSV, JSON, MD) so the user can
 * start an AI conversation with a document as context.
 *
 * Usage:
 *   val text = FileContextLoader.load(context, uri)
 *   // prepend to AI prompt: "Context:\n$text\n\nQuestion: ..."
 *
 * For PDF support, a PDF parsing library (e.g. iText or PdfBox) would be
 * needed; this implementation gracefully falls back to raw byte reading.
 */
object FileContextLoader {

    // Conservative cap to fit low-memory mobile contexts across supported models.
    // 8,000 chars is roughly ~2,000 tokens in English, leaving room for system/user prompt parts.
    private const val MAX_CHARS = 8_000

    /**
     * Read up to [MAX_CHARS] characters from the given URI and return them
     * as a trimmed string, or null on failure.
     */
    fun load(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null && sb.length < MAX_CHARS) {
                    sb.appendLine(line)
                }
                sb.toString().take(MAX_CHARS).trim()
            }
        }.getOrNull()
    }

    /**
     * Build a prompt that includes the file content as context.
     * The AI should read and answer based on the provided document.
     */
    fun buildContextPrompt(fileContent: String, userQuestion: String): String {
        return buildString {
            appendLine("=== Document Context ===")
            appendLine(fileContent)
            appendLine("=== End of Document ===")
            appendLine()
            appendLine("Based on the document above, please answer the following:")
            append(userQuestion)
        }
    }

    /**
     * Summarize a document by constructing a summary prompt.
     */
    fun buildSummaryPrompt(fileContent: String): String {
        return buildString {
            appendLine("Please summarize the following document concisely:")
            appendLine()
            appendLine(fileContent)
        }
    }
}
