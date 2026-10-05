package dev.sk2andy.materialbrowser.browser.credentials.vault

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.security.keystore.UserNotAuthenticatedException
import android.util.AtomicFile
import dev.sk2andy.materialbrowser.data.writeSafely
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The Android side of the vault: the sealed file in `noBackupFilesDir` and the device key in
 * Android Keystore. `no_backup` is outside cloud backup, device-to-device transfer and the app's
 * own data export, and the device key never leaves the hardware anyway, so the vault stays on
 * this phone; the recovery phrase is the way to bring it back.
 */
internal object AndroidCredentialVault {
    private const val FILE_NAME = "credential_vault_v1.bin"
    private const val INDEX_FILE_NAME = "credential_index_v1.bin"

    @Volatile
    private var instance: LocalCredentialVault? = null

    /** The one vault of this app process: the engine and the Passwords screen share its open state. */
    fun get(context: Context): LocalCredentialVault = instance ?: synchronized(this) {
        instance ?: run {
            val directory = context.applicationContext.noBackupFilesDir
            LocalCredentialVault(
                storage = AtomicFileVaultStorage(File(directory, FILE_NAME)),
                index = LoginIndex(AtomicFileVaultStorage(File(directory, INDEX_FILE_NAME)), KeystoreLoginIndexKey()),
            )
        }.also { instance = it }
    }

    fun wordlist(context: Context): RecoveryWordlist? = runCatching {
        context.assets.open(RecoveryWordlist.ASSET).use { it.readBytes() }
    }.getOrNull()?.let(RecoveryWordlist::from)
}

internal class AtomicFileVaultStorage(file: File) : VaultStorage {
    private val file = AtomicFile(file)

    override fun exists(): Boolean = file.baseFile.exists()

    override fun read(): ByteArray? = try {
        // Never read more than a vault can be: a planted huge file must not exhaust memory.
        file.openRead().use { input -> input.readAtMost(VaultEnvelope.MAX_FILE_BYTES) }
    } catch (_: IOException) {
        null
    }

    override fun write(bytes: ByteArray): Boolean = file.writeSafely { output -> output.write(bytes) }

    override fun delete(): Boolean {
        file.delete()
        return !file.baseFile.exists()
    }
}

private fun InputStream.readAtMost(limit: Int): ByteArray? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8_192)
    while (true) {
        val read = read(buffer)
        if (read < 0) return output.toByteArray()
        if (output.size() + read > limit) return null
        output.write(buffer, 0, read)
    }
}

/**
 * The device's way in: an AES-256-GCM key in Android Keystore (StrongBox when the phone has one)
 * that works only on an unlocked device and for [AUTH_WINDOW_SECONDS] after the user confirms
 * with strong biometrics or the screen lock. Enrolling a new fingerprint does not destroy it: the
 * screen lock opens it as well, so a new fingerprint grants nothing the PIN did not.
 */
internal class KeystoreVaultKeyWrapper(
    private val alias: String = ALIAS,
) : VaultKeyWrapper {
    override val slotType: Int = VaultSlotType.DEVICE

    override fun wrap(vaultKey: ByteArray): VaultKeyOutcome<ByteArray> = withCipher { cipher ->
        cipher.init(Cipher.ENCRYPT_MODE, key(createIfMissing = true) ?: return@withCipher VaultKeyOutcome.Refused)
        cipher.updateAAD(AAD)
        VaultKeyOutcome.Done(cipher.iv + cipher.doFinal(vaultKey))
    }

    override fun unwrap(payload: ByteArray): VaultKeyOutcome<ByteArray> = withCipher { cipher ->
        if (payload.size != VaultCrypto.NONCE_BYTES + VaultCrypto.KEY_BYTES + VaultCrypto.TAG_BYTES) {
            return@withCipher VaultKeyOutcome.Refused
        }
        val key = key(createIfMissing = false) ?: return@withCipher VaultKeyOutcome.Refused
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(VaultCrypto.TAG_BYTES * 8, payload, 0, VaultCrypto.NONCE_BYTES),
        )
        cipher.updateAAD(AAD)
        VaultKeyOutcome.Done(cipher.doFinal(payload, VaultCrypto.NONCE_BYTES, payload.size - VaultCrypto.NONCE_BYTES))
    }

    /** Drops the device key, for when the vault itself is erased. */
    fun deleteKey() {
        runCatching { keyStore().deleteEntry(alias) }
    }

    private inline fun withCipher(block: (Cipher) -> VaultKeyOutcome<ByteArray>): VaultKeyOutcome<ByteArray> = try {
        block(Cipher.getInstance(TRANSFORMATION))
    } catch (_: UserNotAuthenticatedException) {
        VaultKeyOutcome.NeedsAuthentication
    } catch (_: KeyPermanentlyInvalidatedException) {
        // The screen lock was removed: the key is gone for good and only the recovery phrase opens the vault.
        VaultKeyOutcome.Refused
    } catch (_: GeneralSecurityException) {
        VaultKeyOutcome.Refused
    } catch (_: ProviderException) {
        VaultKeyOutcome.Refused
    } catch (_: IllegalStateException) {
        // Keystore refuses auth-bound keys while the device has no secure screen lock.
        VaultKeyOutcome.Refused
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
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(
                    AUTH_WINDOW_SECONDS,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                )
                .setInvalidatedByBiometricEnrollment(false)
                .setUnlockedDeviceRequired(true)
                .setIsStrongBoxBacked(strongBox)
                .build(),
        )
        return generator.generateKey()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    internal companion object {
        const val ALIAS = "vola_credential_vault_v1"
        const val AUTH_WINDOW_SECONDS = 30
        private const val KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val AAD = "vola-vault/device-slot/v1".toByteArray(Charsets.US_ASCII)
    }
}
