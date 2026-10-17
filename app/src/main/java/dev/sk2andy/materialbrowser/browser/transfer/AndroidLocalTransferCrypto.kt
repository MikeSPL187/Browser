package dev.sk2andy.materialbrowser.browser.transfer

import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferCrypto
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** [LocalTransferCrypto] on the platform's providers: `SecureRandom`, HMAC-SHA256, AES-256-GCM. */
internal object AndroidLocalTransferCrypto : LocalTransferCrypto {
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    override fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    override fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key, "HmacSHA256"))
            doFinal(data)
        }

    override fun aesGcmSeal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray =
        cipher(Cipher.ENCRYPT_MODE, key, nonce, aad).doFinal(plaintext)

    override fun aesGcmOpen(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, aad: ByteArray): ByteArray? =
        try {
            cipher(Cipher.DECRYPT_MODE, key, nonce, aad).doFinal(ciphertext)
        } catch (_: GeneralSecurityException) {
            // AEADBadTagException: wrong key, nonce, associated data or changed bytes.
            null
        }

    private fun cipher(mode: Int, key: ByteArray, nonce: ByteArray, aad: ByteArray): Cipher {
        require(key.size == 32 && nonce.size == 12)
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(aad)
        }
    }
}
