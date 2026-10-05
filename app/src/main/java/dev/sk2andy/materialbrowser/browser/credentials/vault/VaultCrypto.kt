package dev.sk2andy.materialbrowser.browser.credentials.vault

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * The vault's symmetric crypto: AES-256-GCM with a fresh random 96-bit nonce per seal, and
 * PBKDF2-HMAC-SHA256 for keys that come from the recovery phrase.
 *
 * A sealed blob is `nonce ‖ ciphertext ‖ tag`. The additional data binds a blob to its place, so a
 * blob cut from one part of the vault file does not open in another.
 */
internal object VaultCrypto {
    const val KEY_BYTES = 32
    const val NONCE_BYTES = 12
    const val TAG_BYTES = 16
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KDF = "PBKDF2WithHmacSHA256"

    fun newKey(random: SecureRandom): ByteArray = ByteArray(KEY_BYTES).also(random::nextBytes)

    fun seal(key: ByteArray, plaintext: ByteArray, aad: ByteArray, random: SecureRandom): ByteArray {
        require(key.size == KEY_BYTES)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BYTES * 8, nonce))
        cipher.updateAAD(aad)
        return nonce + cipher.doFinal(plaintext)
    }

    /** The plaintext, or null when [sealed] was not sealed with [key] and [aad] or was changed since. */
    fun open(key: ByteArray, sealed: ByteArray, aad: ByteArray): ByteArray? {
        if (key.size != KEY_BYTES || sealed.size < NONCE_BYTES + TAG_BYTES) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(TAG_BYTES * 8, sealed, 0, NONCE_BYTES),
            )
            cipher.updateAAD(aad)
            cipher.doFinal(sealed, NONCE_BYTES, sealed.size - NONCE_BYTES)
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    fun deriveKey(secret: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(secret, salt, iterations, KEY_BYTES * 8)
        return try {
            SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
