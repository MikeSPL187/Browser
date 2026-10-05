package dev.sk2andy.materialbrowser.browser.credentials.vault

import dev.sk2andy.materialbrowser.shared.credentials.CredentialVault
import dev.sk2andy.materialbrowser.shared.credentials.CredentialVaultRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultChange
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginDraft
import dev.sk2andy.materialbrowser.shared.credentials.VaultSaveResult
import java.security.SecureRandom
import java.util.UUID

/** Where the sealed vault file lives; writes replace the whole file atomically. */
internal interface VaultStorage {
    fun exists(): Boolean

    fun read(): ByteArray?

    fun write(bytes: ByteArray): Boolean

    fun delete(): Boolean
}

internal enum class VaultOpenResult {
    Opened,

    /** There is no vault on this device yet. */
    Missing,

    /** Creating would overwrite the vault that is already here. */
    AlreadyExists,

    /** The file has no slot for this way in. */
    NoSlot,
    NeedsAuthentication,

    /** The way in did not open the vault: wrong phrase, lost device key, or a changed file. */
    Refused,

    /** The file is not a vault this version can read. */
    Unreadable,

    /** The vault could not be written. */
    Failed,
}

/**
 * The vault on this device. One random 256-bit key seals all logins; each slot in the file holds
 * that key wrapped by one way in ([VaultKeyWrapper]). The key and the logins exist in memory only
 * while the vault is open, and every change rewrites the sealed file before it is shown.
 */
internal class LocalCredentialVault(
    private val storage: VaultStorage,
    private val random: SecureRandom = SecureRandom(),
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : CredentialVault {
    private var key: ByteArray? = null
    private var slots: List<VaultSlot> = emptyList()
    private var logins: List<VaultLogin> = emptyList()

    val exists: Boolean get() = storage.exists()

    override val isUnlocked: Boolean
        @Synchronized get() = key != null

    /** Creates an empty vault that each of [ways] can open, and leaves it open. */
    @Synchronized
    fun create(ways: List<VaultKeyWrapper>): VaultOpenResult {
        if (storage.exists()) return VaultOpenResult.AlreadyExists
        require(ways.isNotEmpty() && ways.map(VaultKeyWrapper::slotType).distinct().size == ways.size)
        val newKey = VaultCrypto.newKey(random)
        val newSlots = ways.map { way ->
            when (val wrapped = way.wrap(newKey)) {
                is VaultKeyOutcome.Done -> VaultSlot(way.slotType, wrapped.value)
                VaultKeyOutcome.NeedsAuthentication -> return VaultOpenResult.NeedsAuthentication.also { newKey.fill(0) }
                VaultKeyOutcome.Refused -> return VaultOpenResult.Failed.also { newKey.fill(0) }
            }
        }
        if (!persist(newKey, newSlots, emptyList())) {
            newKey.fill(0)
            return VaultOpenResult.Failed
        }
        lock()
        key = newKey
        slots = newSlots
        return VaultOpenResult.Opened
    }

    @Synchronized
    fun unlock(way: VaultKeyWrapper): VaultOpenResult {
        val envelope = (storage.read() ?: return VaultOpenResult.Missing)
            .let(VaultEnvelope::decode) ?: return VaultOpenResult.Unreadable
        val slot = envelope.slots.firstOrNull { it.type == way.slotType } ?: return VaultOpenResult.NoSlot
        val openedKey = when (val unwrapped = way.unwrap(slot.payload)) {
            is VaultKeyOutcome.Done -> unwrapped.value
            VaultKeyOutcome.NeedsAuthentication -> return VaultOpenResult.NeedsAuthentication
            VaultKeyOutcome.Refused -> return VaultOpenResult.Refused
        }
        val plaintext = VaultCrypto.open(openedKey, envelope.body, VaultEnvelope.header(envelope.slots))
        if (plaintext == null || openedKey.size != VaultCrypto.KEY_BYTES) {
            openedKey.fill(0)
            return VaultOpenResult.Refused
        }
        val opened = try {
            VaultDocumentCodec.decode(plaintext)
        } finally {
            plaintext.fill(0)
        }
        if (opened == null) {
            openedKey.fill(0)
            return VaultOpenResult.Unreadable
        }
        lock()
        key = openedKey
        slots = envelope.slots
        logins = opened
        return VaultOpenResult.Opened
    }

    @Synchronized
    override fun loginsFor(origin: String): List<VaultLogin> =
        if (key == null) emptyList() else CredentialVaultRules.loginsFor(logins, origin)

    @Synchronized
    override fun allLogins(): List<VaultLogin> =
        if (key == null) emptyList() else CredentialVaultRules.sortedForList(logins)

    @Synchronized
    override fun save(draft: VaultLoginDraft, nowMillis: Long): VaultSaveResult {
        val openKey = key ?: return VaultSaveResult.Locked
        val change: VaultChange = CredentialVaultRules.save(logins, draft, nowMillis, newId)
        if (change.logins === logins) return change.result
        if (!persist(openKey, slots, change.logins)) return VaultSaveResult.Failed
        logins = change.logins
        return change.result
    }

    @Synchronized
    override fun markUsed(id: String, nowMillis: Long): Boolean {
        val openKey = key ?: return false
        val changed = CredentialVaultRules.markUsed(logins, id, nowMillis) ?: return false
        return persist(openKey, slots, changed).also { written -> if (written) logins = changed }
    }

    @Synchronized
    override fun delete(id: String): Boolean {
        val openKey = key ?: return false
        val changed = CredentialVaultRules.delete(logins, id) ?: return false
        return persist(openKey, slots, changed).also { written -> if (written) logins = changed }
    }

    @Synchronized
    override fun lock() {
        key?.fill(0)
        key = null
        slots = emptyList()
        logins = emptyList()
    }

    /** Erases the vault from this device: the way out when every way in is lost. */
    @Synchronized
    fun destroy(): Boolean {
        lock()
        return storage.delete()
    }

    private fun persist(vaultKey: ByteArray, vaultSlots: List<VaultSlot>, content: List<VaultLogin>): Boolean {
        val header = VaultEnvelope.header(vaultSlots)
        val plaintext = VaultDocumentCodec.encode(content)
        val body = try {
            VaultCrypto.seal(vaultKey, plaintext, header, random)
        } finally {
            plaintext.fill(0)
        }
        return storage.write(VaultEnvelope(vaultSlots, body).encode())
    }
}
