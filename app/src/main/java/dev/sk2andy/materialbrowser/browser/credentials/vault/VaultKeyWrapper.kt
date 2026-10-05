package dev.sk2andy.materialbrowser.browser.credentials.vault

import java.nio.ByteBuffer
import java.security.SecureRandom

/** What a way into the vault answered. */
internal sealed interface VaultKeyOutcome<out T> {
    data class Done<T>(val value: T) : VaultKeyOutcome<T>

    /** The device key works only right after the user confirms with biometrics or the screen lock. */
    data object NeedsAuthentication : VaultKeyOutcome<Nothing>

    /** Wrong phrase, a key that no longer exists, or a slot that was changed. */
    data object Refused : VaultKeyOutcome<Nothing>
}

/** One way into the vault: wraps the vault key into a slot payload and unwraps it back. */
internal interface VaultKeyWrapper {
    val slotType: Int

    fun wrap(vaultKey: ByteArray): VaultKeyOutcome<ByteArray>

    fun unwrap(payload: ByteArray): VaultKeyOutcome<ByteArray>
}

/**
 * The recovery phrase's slot: `salt (16) ‖ iterations (4) ‖ seal(vault key)` under a key derived
 * with PBKDF2-HMAC-SHA256. The phrase already carries ~124 bits, so the iteration count only adds
 * cost for an attacker with the file; it is bounded on read so a doctored file cannot stall the app.
 */
internal class RecoveryPhraseKeyWrapper(
    phrase: String,
    private val random: SecureRandom,
    private val iterations: Int = DEFAULT_ITERATIONS,
) : VaultKeyWrapper {
    private val secret = phrase.toCharArray()

    override val slotType: Int = VaultSlotType.RECOVERY_PHRASE

    override fun wrap(vaultKey: ByteArray): VaultKeyOutcome<ByteArray> {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val key = VaultCrypto.deriveKey(secret, salt, iterations)
        return try {
            val sealed = VaultCrypto.seal(key, vaultKey, AAD, random)
            VaultKeyOutcome.Done(salt + ByteBuffer.allocate(Int.SIZE_BYTES).putInt(iterations).array() + sealed)
        } finally {
            key.fill(0)
        }
    }

    override fun unwrap(payload: ByteArray): VaultKeyOutcome<ByteArray> {
        if (payload.size != SALT_BYTES + Int.SIZE_BYTES + SEALED_KEY_BYTES) return VaultKeyOutcome.Refused
        val salt = payload.copyOfRange(0, SALT_BYTES)
        val storedIterations = ByteBuffer.wrap(payload, SALT_BYTES, Int.SIZE_BYTES).int
        if (storedIterations !in MIN_ITERATIONS..MAX_ITERATIONS) return VaultKeyOutcome.Refused
        val key = VaultCrypto.deriveKey(secret, salt, storedIterations)
        return try {
            VaultCrypto.open(key, payload.copyOfRange(SALT_BYTES + Int.SIZE_BYTES, payload.size), AAD)
                ?.let { VaultKeyOutcome.Done(it) }
                ?: VaultKeyOutcome.Refused
        } finally {
            key.fill(0)
        }
    }

    /** Clears the phrase this wrapper holds; call once the vault is created or opened. */
    fun forget() = secret.fill('\u0000')

    internal companion object {
        const val DEFAULT_ITERATIONS = 600_000
        const val MIN_ITERATIONS = 1_000
        const val MAX_ITERATIONS = 5_000_000
        private const val SALT_BYTES = 16
        private const val SEALED_KEY_BYTES = VaultCrypto.NONCE_BYTES + VaultCrypto.KEY_BYTES + VaultCrypto.TAG_BYTES
        private val AAD = "vola-vault/recovery-slot/v1".toByteArray(Charsets.US_ASCII)
    }
}
