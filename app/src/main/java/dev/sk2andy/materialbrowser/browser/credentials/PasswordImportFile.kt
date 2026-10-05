package dev.sk2andy.materialbrowser.browser.credentials

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.annotation.WorkerThread
import dev.sk2andy.materialbrowser.shared.credentials.PasswordImportRules
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** A picked export file: its text, or why it could not be read. */
internal sealed interface PasswordImportFile {
    data class Loaded(val text: String, val name: String?, val sizeBytes: Long) : PasswordImportFile {
        override fun toString(): String = "Loaded(name=$name, sizeBytes=$sizeBytes)"
    }

    data object TooLarge : PasswordImportFile

    data object NotText : PasswordImportFile

    data object Unreadable : PasswordImportFile
}

/**
 * Reads the export the user picked (Q22a) and offers to delete it afterwards: the file holds every
 * password in plain text. The bytes stay in memory only while they are decoded.
 */
internal object PasswordImportFiles {
    private const val BUFFER_BYTES = 8 * 1_024

    @WorkerThread
    fun read(resolver: ContentResolver, uri: Uri): PasswordImportFile {
        val name = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        val stream = runCatching { resolver.openInputStream(uri) }.getOrNull() ?: return PasswordImportFile.Unreadable
        return read(stream, name)
    }

    fun read(input: InputStream, name: String?): PasswordImportFile = runCatching {
        input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (output.size() + count > PasswordImportRules.MAX_FILE_BYTES) return PasswordImportFile.TooLarge
                output.write(buffer, 0, count)
            }
            buffer.fill(0)
            val bytes = output.toByteArray()
            try {
                val text = runCatching {
                    Charsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes))
                        .toString()
                }.getOrElse { return PasswordImportFile.NotText }
                PasswordImportFile.Loaded(text, name, bytes.size.toLong())
            } finally {
                bytes.fill(0)
            }
        }
    }.getOrDefault(PasswordImportFile.Unreadable)

    /** Deletes the picked file through its document provider; false when the provider refuses. */
    @WorkerThread
    fun delete(resolver: ContentResolver, uri: Uri): Boolean =
        runCatching { DocumentsContract.deleteDocument(resolver, uri) }.getOrDefault(false)
}
