package dev.sk2andy.materialbrowser.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import dev.sk2andy.materialbrowser.data.writeSafely
import dev.sk2andy.materialbrowser.sync.SyncBase64
import dev.sk2andy.materialbrowser.sync.SyncCache
import dev.sk2andy.materialbrowser.sync.SyncCacheStore
import dev.sk2andy.materialbrowser.sync.SyncConnectionSettings
import dev.sk2andy.materialbrowser.sync.SyncSettingsStore
import dev.sk2andy.materialbrowser.sync.SyncVaultSecrets
import dev.sk2andy.materialbrowser.sync.SyncVaultStore
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

class AndroidSyncSettingsStore internal constructor(
    private val preferences: SharedPreferences,
) : SyncSettingsStore {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
    )

    override fun load(): SyncConnectionSettings? = preferences.getString(KEY_SETTINGS, null)?.let { raw ->
        runCatching { SyncStateCodec.decodeSettings(raw) }.getOrNull()
    }

    override fun save(value: SyncConnectionSettings): Boolean = preferences.edit()
        .putString(KEY_SETTINGS, SyncStateCodec.encodeSettings(value))
        .commit()

    override fun clear(): Boolean = preferences.edit().remove(KEY_SETTINGS).commit()

    private companion object {
        const val PREFERENCES_NAME = "candy_sync_settings"
        const val KEY_SETTINGS = "connection"
    }
}

class AndroidSyncVaultStore(
    context: Context,
    private val protector: AndroidSyncSecretProtector = AndroidSyncSecretProtector(),
) : SyncVaultStore {
    private val file = AtomicFile(File(context.applicationContext.noBackupFilesDir, "candy_sync_vault_v1"))

    @Synchronized
    override fun load(): SyncVaultSecrets? = runCatching {
        val encrypted = file.openRead().use {
            it.readBytesLimited(AndroidSyncSecretProtector.maxEnvelopeBytes(MAX_VAULT_BYTES))
        }
        val plaintext = protector.decrypt(encrypted, VAULT_AAD)
        try {
            SyncStateCodec.decodeVault(plaintext)
        } finally {
            plaintext.fill(0)
        }
    }.getOrNull()

    @Synchronized
    override fun save(value: SyncVaultSecrets): Boolean {
        val plaintext = SyncStateCodec.encodeVault(value)
        return try {
            require(plaintext.size <= MAX_VAULT_BYTES)
            val encrypted = protector.encrypt(plaintext, VAULT_AAD)
            file.writeSafely { it.write(encrypted) }
        } finally {
            plaintext.fill(0)
        }
    }

    @Synchronized
    override fun clear() = file.delete()

    private companion object {
        val VAULT_AAD = "candy-sync/android-vault/v1".toByteArray()
        const val MAX_VAULT_BYTES = 16 * 1_024
    }
}

class AndroidSyncCacheStore(
    context: Context,
    private val protector: AndroidSyncSecretProtector = AndroidSyncSecretProtector(),
) : SyncCacheStore {
    private val file = AtomicFile(File(context.applicationContext.noBackupFilesDir, "candy_sync_cache_v1"))

    @Synchronized
    override fun load(): SyncCache = runCatching {
        val encrypted = file.openRead().use {
            it.readBytesLimited(AndroidSyncSecretProtector.maxEnvelopeBytes(MAX_PLAINTEXT_CACHE_BYTES))
        }
        val plaintext = protector.decrypt(encrypted, CACHE_AAD)
        try {
            SyncStateCodec.decodeCache(plaintext)
        } finally {
            plaintext.fill(0)
        }
    }.getOrDefault(SyncCache(cursor = "", profiles = emptyMap()))

    @Synchronized
    override fun save(value: SyncCache): Boolean {
        val plaintext = SyncStateCodec.encodeCache(value)
        return try {
            require(plaintext.size <= MAX_PLAINTEXT_CACHE_BYTES)
            val encrypted = protector.encrypt(plaintext, CACHE_AAD)
            file.writeSafely { it.write(encrypted) }
        } finally {
            plaintext.fill(0)
        }
    }

    @Synchronized
    override fun clear() = file.delete()

    private companion object {
        val CACHE_AAD = "candy-sync/android-cache/v1".toByteArray()
        const val MAX_PLAINTEXT_CACHE_BYTES = 8 * 1_024 * 1_024
    }
}

class AndroidSyncSecretProtector(
    private val alias: String = "candy_sync_local_v1",
) {
    fun encrypt(plaintext: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(aad)
        return JSONObject()
            .put("cryptoVersion", 1)
            .put("nonce", SyncBase64.encode(cipher.iv))
            .put("ciphertext", SyncBase64.encode(cipher.doFinal(plaintext)))
            .toString()
            .toByteArray(Charsets.UTF_8)
    }

    fun decrypt(envelope: ByteArray, aad: ByteArray): ByteArray {
        val value = JSONObject(envelope.toString(Charsets.UTF_8))
        require(value.keys().asSequence().toSet() == setOf("cryptoVersion", "nonce", "ciphertext"))
        require(value.getInt("cryptoVersion") == 1)
        val nonce = SyncBase64.decode(value.getString("nonce"), expectedBytes = 12)
        val ciphertext = SyncBase64.decode(
            value.getString("ciphertext"),
            maxBytes = MAX_PLAINTEXT_BYTES + GCM_TAG_BYTES,
        )
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BYTES = 16
        private const val NONCE_BYTES = 12

        /** The largest plaintext any store saves (the cache's); a vault is much smaller. */
        private const val MAX_PLAINTEXT_BYTES = 8 * 1_024 * 1_024

        /** Room for the JSON keys, quotes and the version around the two Base64 values. */
        private const val ENVELOPE_JSON_BYTES = 128

        /**
         * The size of the envelope [encrypt] writes for at most [maxPlaintextBytes]: Base64 grows
         * the ciphertext and its tag by a third, so a store must not read with its plaintext limit.
         */
        fun maxEnvelopeBytes(maxPlaintextBytes: Int): Int =
            base64Length(maxPlaintextBytes + GCM_TAG_BYTES) + base64Length(NONCE_BYTES) +
                ENVELOPE_JSON_BYTES

        private fun base64Length(bytes: Int): Int = (bytes + 2) / 3 * 4
    }
}

private fun java.io.InputStream.readBytesLimited(maximum: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8_192)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= maximum)
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}
