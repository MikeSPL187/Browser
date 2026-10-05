package dev.sk2andy.materialbrowser.browser.credentials.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import dev.sk2andy.materialbrowser.shared.credentials.CredentialVaultRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginHint
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Seals and opens the login index with a device key; null when the key refuses. */
internal interface LoginIndexKey {
    fun seal(plaintext: ByteArray): ByteArray?

    fun open(sealed: ByteArray): ByteArray?

    fun delete()
}

/**
 * The login index: site and user name of every saved login, never a password. It lets a page offer
 * «Sign in as …» while the vault is locked; the fingerprint is asked only when a login is picked.
 * The vault keeps it in step with every change ([LocalCredentialVault]).
 */
internal class LoginIndex(
    private val storage: VaultStorage,
    private val key: LoginIndexKey,
) {
    private var cache: List<VaultLoginHint>? = null

    /** The saved hints; empty when there is no index or the device key refuses (screen locked). */
    @Synchronized
    fun hints(): List<VaultLoginHint> {
        cache?.let { return it }
        if (!storage.exists()) return emptyList<VaultLoginHint>().also { cache = it }
        val sealed = storage.read() ?: return emptyList()
        val plaintext = key.open(sealed) ?: return emptyList()
        val hints = try {
            LoginIndexCodec.decode(plaintext)
        } finally {
            plaintext.fill(0)
        }
        return hints?.also { cache = it }.orEmpty()
    }

    /** Writes [hints] when they differ from what is saved; false when the write failed. */
    @Synchronized
    fun replace(hints: List<VaultLoginHint>): Boolean {
        if (cache == hints && storage.exists()) return true
        val plaintext = LoginIndexCodec.encode(hints)
        val sealed = try {
            key.seal(plaintext)
        } finally {
            plaintext.fill(0)
        } ?: return false
        if (!storage.write(sealed)) return false
        cache = hints
        return true
    }

    /** Forgets the index and its key: the vault is gone. */
    @Synchronized
    fun clear() {
        cache = null
        storage.delete()
        key.delete()
    }
}

/** The plaintext of the index: `VOLAINDEX` `v1` · hints, each checked again on the way in. */
internal object LoginIndexCodec {
    private val MAGIC = "VOLAINDEX".toByteArray(Charsets.US_ASCII)
    private const val VERSION = 1
    private const val NEVER = Long.MIN_VALUE

    fun encode(hints: List<VaultLoginHint>): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.write(MAGIC)
            output.writeByte(VERSION)
            output.writeInt(hints.size)
            hints.forEach { hint ->
                output.writeUTF(hint.id)
                output.writeUTF(hint.origin)
                output.writeOptional(hint.formActionOrigin)
                output.writeOptional(hint.httpRealm)
                output.writeUTF(hint.username)
                output.writeLong(hint.lastUsedAtMillis ?: NEVER)
            }
        }
        return bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): List<VaultLoginHint>? = try {
        val input = DataInputStream(ByteArrayInputStream(bytes))
        val magic = ByteArray(MAGIC.size).also(input::readFully)
        if (!magic.contentEquals(MAGIC) || input.readUnsignedByte() != VERSION) {
            null
        } else {
            read(input)
        }
    } catch (_: IOException) {
        null
    }

    private fun read(input: DataInputStream): List<VaultLoginHint>? {
        val count = input.readInt()
        if (count !in 0..CredentialVaultRules.MAX_LOGINS) return null
        val hints = List(count) {
            val hint = VaultLoginHint(
                id = input.readUTF(),
                origin = input.readUTF(),
                formActionOrigin = input.readOptional(),
                httpRealm = input.readOptional(),
                username = input.readUTF(),
                lastUsedAtMillis = input.readLong().takeIf { it != NEVER },
            )
            if (!CredentialVaultRules.accepts(hint)) return null
            hint
        }
        return if (input.read() != -1 || hints.map(VaultLoginHint::id).distinct().size != hints.size) null else hints
    }

    private fun DataOutputStream.writeOptional(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeUTF(value)
    }

    private fun DataInputStream.readOptional(): String? = if (readBoolean()) readUTF() else null
}

/**
 * The index's device key: AES-256-GCM in Android Keystore (StrongBox when there is one). It needs
 * no fingerprint, so the sign-in sheet opens at once, but it works only while the phone is
 * unlocked, and it never leaves the hardware.
 */
internal class KeystoreLoginIndexKey(
    private val alias: String = ALIAS,
) : LoginIndexKey {
    override fun seal(plaintext: ByteArray): ByteArray? = guarded {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key(createIfMissing = true) ?: return@guarded null)
        cipher.updateAAD(AAD)
        cipher.iv + cipher.doFinal(plaintext)
    }

    override fun open(sealed: ByteArray): ByteArray? = guarded {
        if (sealed.size < VaultCrypto.NONCE_BYTES + VaultCrypto.TAG_BYTES) return@guarded null
        val key = key(createIfMissing = false) ?: return@guarded null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(VaultCrypto.TAG_BYTES * 8, sealed, 0, VaultCrypto.NONCE_BYTES),
        )
        cipher.updateAAD(AAD)
        cipher.doFinal(sealed, VaultCrypto.NONCE_BYTES, sealed.size - VaultCrypto.NONCE_BYTES)
    }

    override fun delete() {
        runCatching { keyStore().deleteEntry(alias) }
    }

    private inline fun guarded(block: () -> ByteArray?): ByteArray? = try {
        block()
    } catch (_: GeneralSecurityException) {
        null
    } catch (_: ProviderException) {
        null
    } catch (_: IllegalStateException) {
        null
    }

    private fun key(createIfMissing: Boolean): SecretKey? {
        (keyStore().getKey(alias, null) as? SecretKey)?.let { return it }
        if (!createIfMissing) return null
        return try {
            generate(strongBox = true)
        } catch (_: StrongBoxUnavailableException) {
            generate(strongBox = false)
        }
    }

    private fun generate(strongBox: Boolean): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(VaultCrypto.KEY_BYTES * 8)
                .setRandomizedEncryptionRequired(true)
                .setUnlockedDeviceRequired(true)
                .setIsStrongBoxBacked(strongBox)
                .build(),
        )
        return generator.generateKey()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    internal companion object {
        const val ALIAS = "vola_credential_index_v1"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val AAD = "vola-vault/login-index/v1".toByteArray(Charsets.US_ASCII)
    }
}
