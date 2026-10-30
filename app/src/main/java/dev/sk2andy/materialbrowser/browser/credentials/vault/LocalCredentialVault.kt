package dev.sk2andy.materialbrowser.browser.credentials.vault

import dev.sk2andy.materialbrowser.shared.credentials.CredentialVault
import dev.sk2andy.materialbrowser.shared.credentials.CredentialVaultRules
import dev.sk2andy.materialbrowser.shared.credentials.ImportConflict
import dev.sk2andy.materialbrowser.shared.credentials.ImportedLogin
import dev.sk2andy.materialbrowser.shared.credentials.VaultChange
import dev.sk2andy.materialbrowser.shared.credentials.VaultImportResult
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginDraft
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginHint
import dev.sk2andy.materialbrowser.shared.credentials.VaultSaveResult
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.Executor

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
 *
 * [index] mirrors the sites and user names, never the passwords, so a locked vault can still say
 * which logins a page has ([loginHints]). It is rewritten after every change and every opening, on
 * [indexWriter] and outside the vault's lock: its key lives in Keystore (StrongBox on many phones),
 * where one operation can take seconds.
 *
 * Reads never wait: the open state is one immutable snapshot, so the main thread can ask
 * [isUnlocked] or [allLogins] while a write or a Keystore unwrap runs elsewhere (an unlock froze the
 * Passwords screen for ~10 s and a second opening hit an ANR when reads shared the writers' lock).
 */
internal class LocalCredentialVault(
    private val storage: VaultStorage,
    private val random: SecureRandom = SecureRandom(),
    private val index: LoginIndex? = null,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val indexWriter: Executor = Executor(Runnable::run),
) : CredentialVault {
    /** The open vault; replaced whole, never changed in place, so a reader sees one consistent state. */
    private class Open(val key: ByteArray, val slots: List<VaultSlot>, val logins: List<VaultLogin>)

    @Volatile
    private var open: Open? = null

    val exists: Boolean get() = storage.exists()

    override val isUnlocked: Boolean get() = open != null

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
        publish(Open(newKey, newSlots, emptyList()))
        return VaultOpenResult.Opened
    }

    /**
     * Opens the vault with [way]. The slow part (reading the file, the Keystore unwrap, decryption)
     * runs before the lock is taken; only the switch to the open state holds it.
     */
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
        synchronized(this) { publish(Open(openedKey, envelope.slots, opened)) }
        return VaultOpenResult.Opened
    }

    override fun loginsFor(origin: String): List<VaultLogin> =
        open?.let { CredentialVaultRules.loginsFor(it.logins, origin) }.orEmpty()

    override fun allLogins(): List<VaultLogin> =
        open?.let { CredentialVaultRules.sortedForList(it.logins) }.orEmpty()

    override fun loginHints(): List<VaultLoginHint> {
        open?.let { return CredentialVaultRules.hints(it.logins) }
        return if (storage.exists()) index?.hints().orEmpty() else emptyList()
    }

    @Synchronized
    override fun save(draft: VaultLoginDraft, nowMillis: Long): VaultSaveResult {
        val current = open ?: return VaultSaveResult.Locked
        val change: VaultChange = CredentialVaultRules.save(current.logins, draft, nowMillis, newId)
        if (change.logins === current.logins) return change.result
        if (!commit(current, change.logins)) return VaultSaveResult.Failed
        return change.result
    }

    @Synchronized
    override fun update(id: String, draft: VaultLoginDraft, nowMillis: Long): VaultSaveResult {
        val current = open ?: return VaultSaveResult.Locked
        val change = CredentialVaultRules.update(current.logins, id, draft, nowMillis)
        if (change.logins === current.logins) return change.result
        if (!commit(current, change.logins)) return VaultSaveResult.Failed
        return change.result
    }

    /**
     * Gives the open vault a new slot for [way], replacing any slot of that kind: after recovering
     * with the phrase, the device gets a fresh key; after showing a new phrase, the old one stops
     * working. Needs the vault open, and [way] may first need the user (device key).
     */
    @Synchronized
    fun replaceSlot(way: VaultKeyWrapper): VaultOpenResult {
        val current = open ?: return VaultOpenResult.Refused
        val payload = when (val wrapped = way.wrap(current.key)) {
            is VaultKeyOutcome.Done -> wrapped.value
            VaultKeyOutcome.NeedsAuthentication -> return VaultOpenResult.NeedsAuthentication
            VaultKeyOutcome.Refused -> return VaultOpenResult.Failed
        }
        val newSlots = current.slots.filterNot { it.type == way.slotType } + VaultSlot(way.slotType, payload)
        if (!persist(current.key, newSlots, current.logins)) return VaultOpenResult.Failed
        open = Open(current.key, newSlots, current.logins)
        return VaultOpenResult.Opened
    }

    /** Which ways in the vault file offers, or empty when there is none or it cannot be read. */
    fun slotTypes(): Set<Int> =
        storage.read()?.let(VaultEnvelope::decode)?.slots?.mapTo(mutableSetOf(), VaultSlot::type).orEmpty()

    @Synchronized
    override fun markUsed(id: String, nowMillis: Long): Boolean {
        val current = open ?: return false
        val changed = CredentialVaultRules.markUsed(current.logins, id, nowMillis) ?: return false
        return commit(current, changed)
    }

    @Synchronized
    override fun setTotp(id: String, totp: String?, nowMillis: Long): Boolean {
        val current = open ?: return false
        val changed = CredentialVaultRules.setTotp(current.logins, id, totp, nowMillis) ?: return false
        return commit(current, changed, reindex = false)
    }

    @Synchronized
    override fun importLogins(logins: List<ImportedLogin>, nowMillis: Long): VaultImportResult {
        val current = open ?: return VaultImportResult.Locked
        val (changed, summary) = CredentialVaultRules.import(current.logins, logins, nowMillis, newId)
        if (changed != current.logins && !commit(current, changed)) return VaultImportResult.Failed
        return VaultImportResult.Imported(summary)
    }

    @Synchronized
    override fun replacePasswords(conflicts: List<ImportConflict>, nowMillis: Long): Boolean {
        val current = open ?: return false
        val changed = CredentialVaultRules.replacePasswords(current.logins, conflicts, nowMillis) ?: return true
        return commit(current, changed, reindex = false)
    }

    @Synchronized
    override fun delete(id: String): Boolean {
        val current = open ?: return false
        val changed = CredentialVaultRules.delete(current.logins, id) ?: return false
        return commit(current, changed)
    }

    @Synchronized
    override fun lock() {
        val current = open ?: return
        open = null
        current.key.fill(0)
    }

    /** Erases the vault from this device: the way out when every way in is lost. */
    @Synchronized
    fun destroy(): Boolean {
        lock()
        // After any index write still queued, so an erased vault leaves no user names behind.
        index?.let { indexWriter.execute(it::clear) }
        return storage.delete()
    }

    /** Replaces the open state with [next] (the old key is wiped) and brings the index in step. */
    private fun publish(next: Open) {
        val previous = open
        open = next
        if (previous != null && previous.key !== next.key) previous.key.fill(0)
        syncIndex(next.logins)
    }

    /** Seals [logins] into the file, then shows them; false leaves the vault as it was. */
    private fun commit(current: Open, logins: List<VaultLogin>, reindex: Boolean = true): Boolean {
        if (!persist(current.key, current.slots, logins)) return false
        open = Open(current.key, current.slots, logins)
        if (reindex) syncIndex(logins)
        return true
    }

    /** The index follows the open vault; a failed index write leaves the vault as it is. */
    private fun syncIndex(logins: List<VaultLogin>) {
        val index = index ?: return
        val hints = CredentialVaultRules.hints(logins)
        indexWriter.execute { index.replace(hints) }
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
