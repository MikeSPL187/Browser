package dev.sk2andy.materialbrowser.data.sync.webdav

import android.content.Context
import android.util.AtomicFile
import dev.sk2andy.materialbrowser.data.sync.AndroidSyncSecretProtector
import dev.sk2andy.materialbrowser.data.writeSafely
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavStoredAccount
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncMemory
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncStateCodec
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * The WebDAV sync's files (V27): the account (app password and the key from the phrase) and the
 * memory between syncs (the other devices' tabs and bookmarks). Both are AES-GCM under a key in
 * the Android Keystore and live in `noBackupFilesDir`, so neither leaves the phone in a backup or
 * a device transfer; a restored phone connects again with its phrase.
 */
internal class AndroidWebDavSyncStore(
    context: Context,
    private val protector: AndroidSyncSecretProtector = AndroidSyncSecretProtector(KEY_ALIAS),
) : WebDavSyncStore {
    private val context = context.applicationContext

    /** Resolved on first use, on the sync's thread: `noBackupFilesDir` touches the disk. */
    private val accountFile by lazy { AtomicFile(File(this.context.noBackupFilesDir, "vola_webdav_account_v1")) }
    private val memoryFile by lazy { AtomicFile(File(this.context.noBackupFilesDir, "vola_webdav_memory_v1")) }

    @Synchronized
    override fun loadAccount(): WebDavStoredAccount? =
        read(accountFile, ACCOUNT_AAD, MAX_ACCOUNT_BYTES)?.let(WebDavSyncStateCodec::decodeAccount)

    @Synchronized
    override fun saveAccount(stored: WebDavStoredAccount): Boolean =
        write(accountFile, ACCOUNT_AAD, WebDavSyncStateCodec.encodeAccount(stored), MAX_ACCOUNT_BYTES)

    @Synchronized
    override fun loadMemory(): WebDavSyncMemory? =
        read(memoryFile, MEMORY_AAD, MAX_MEMORY_BYTES)?.let(WebDavSyncStateCodec::decodeMemory)

    @Synchronized
    override fun saveMemory(memory: WebDavSyncMemory): Boolean =
        write(memoryFile, MEMORY_AAD, WebDavSyncStateCodec.encodeMemory(memory), MAX_MEMORY_BYTES)

    @Synchronized
    override fun clear() {
        accountFile.delete()
        memoryFile.delete()
    }

    private fun read(file: AtomicFile, aad: ByteArray, maximum: Int): String? = runCatching {
        val encrypted = file.openRead().use { it.readLimited(maximum * 2) }
        val plaintext = protector.decrypt(encrypted, aad)
        try {
            plaintext.toString(Charsets.UTF_8)
        } finally {
            plaintext.fill(0)
        }
    }.getOrNull()

    private fun write(file: AtomicFile, aad: ByteArray, json: String, maximum: Int): Boolean = runCatching {
        val plaintext = json.toByteArray(Charsets.UTF_8)
        try {
            require(plaintext.size <= maximum)
            val encrypted = protector.encrypt(plaintext, aad)
            file.writeSafely { it.write(encrypted) }
        } finally {
            plaintext.fill(0)
        }
    }.getOrDefault(false)

    private fun InputStream.readLimited(maximum: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            require(output.size() + read <= maximum)
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private companion object {
        const val KEY_ALIAS = "vola_webdav_local_v1"
        val ACCOUNT_AAD = "vola-webdav/android-account/v1".toByteArray()
        val MEMORY_AAD = "vola-webdav/android-memory/v1".toByteArray()
        const val MAX_ACCOUNT_BYTES = 16 * 1_024

        /** The protector's envelope takes at most 9 MiB of ciphertext. */
        const val MAX_MEMORY_BYTES = 8 * 1_024 * 1_024
    }
}
