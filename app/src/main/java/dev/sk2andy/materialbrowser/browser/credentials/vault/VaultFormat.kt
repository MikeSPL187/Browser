package dev.sk2andy.materialbrowser.browser.credentials.vault

import dev.sk2andy.materialbrowser.shared.credentials.CredentialVaultRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import dev.sk2andy.materialbrowser.shared.credentials.VaultLoginDraft
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

/** Who can open the vault: each slot holds the vault key wrapped by one way in. */
internal object VaultSlotType {
    /** Wrapped by an Android Keystore key that needs the device's biometrics or screen lock. */
    const val DEVICE = 1

    /** Wrapped by a key derived from the 12-word recovery phrase. */
    const val RECOVERY_PHRASE = 2
}

internal class VaultSlot(val type: Int, val payload: ByteArray)

/**
 * The vault file: `VOLAVAULT` `v1` · slots · sealed logins.
 *
 * The header (everything before the sealed logins) is the additional data of the seal, so swapping,
 * dropping or editing a slot makes the whole file refuse to open instead of silently changing who
 * can open it.
 */
internal class VaultEnvelope(val slots: List<VaultSlot>, val body: ByteArray) {
    fun encode(): ByteArray = header(slots) + body

    companion object {
        private val MAGIC = "VOLAVAULT".toByteArray(Charsets.US_ASCII)
        private const val VERSION = 1
        private const val MAX_SLOTS = 4
        private const val MAX_SLOT_BYTES = 1_024
        const val MAX_FILE_BYTES = 64 * 1_024 * 1_024

        fun header(slots: List<VaultSlot>): ByteArray {
            require(slots.size in 1..MAX_SLOTS && slots.map(VaultSlot::type).distinct().size == slots.size)
            val bytes = ByteArrayOutputStream()
            DataOutputStream(bytes).use { output ->
                output.write(MAGIC)
                output.writeByte(VERSION)
                output.writeByte(slots.size)
                slots.forEach { slot ->
                    require(slot.type in 1..255 && slot.payload.size in 1..MAX_SLOT_BYTES)
                    output.writeByte(slot.type)
                    output.writeShort(slot.payload.size)
                    output.write(slot.payload)
                }
            }
            return bytes.toByteArray()
        }

        fun decode(bytes: ByteArray): VaultEnvelope? {
            if (bytes.size > MAX_FILE_BYTES) return null
            return try {
                val input = DataInputStream(ByteArrayInputStream(bytes))
                val magic = ByteArray(MAGIC.size).also(input::readFully)
                if (!magic.contentEquals(MAGIC) || input.readUnsignedByte() != VERSION) return null
                val count = input.readUnsignedByte()
                if (count !in 1..MAX_SLOTS) return null
                val slots = List(count) {
                    val type = input.readUnsignedByte()
                    val size = input.readUnsignedShort()
                    if (type == 0 || size !in 1..MAX_SLOT_BYTES) return null
                    VaultSlot(type, ByteArray(size).also(input::readFully))
                }
                if (slots.map(VaultSlot::type).distinct().size != slots.size) return null
                val body = input.readBytes()
                if (body.size < VaultCrypto.NONCE_BYTES + VaultCrypto.TAG_BYTES) return null
                VaultEnvelope(slots, body)
            } catch (_: IOException) {
                null
            }
        }
    }
}

/** The plaintext inside the seal: the saved logins, each checked again on the way in. */
internal object VaultDocumentCodec {
    private const val VERSION = 1

    fun encode(logins: List<VaultLogin>): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(VERSION)
            output.writeInt(logins.size)
            logins.forEach { login ->
                output.writeUTF(login.id)
                output.writeUTF(login.origin)
                output.writeOptional(login.formActionOrigin)
                output.writeOptional(login.httpRealm)
                output.writeUTF(login.username)
                output.writeUTF(login.password)
                output.writeLong(login.createdAtMillis)
                output.writeLong(login.updatedAtMillis)
                output.writeLong(login.lastUsedAtMillis ?: NEVER)
                output.writeInt(login.timesUsed)
            }
        }
        return bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): List<VaultLogin>? = try {
        read(DataInputStream(ByteArrayInputStream(bytes)))
    } catch (_: IOException) {
        null
    }

    private fun read(input: DataInputStream): List<VaultLogin>? {
        if (input.readInt() != VERSION) return null
        val count = input.readInt()
        if (count !in 0..CredentialVaultRules.MAX_LOGINS) return null
        val logins = List(count) {
            val login = VaultLogin(
                id = input.readUTF(),
                origin = input.readUTF(),
                formActionOrigin = input.readOptional(),
                httpRealm = input.readOptional(),
                username = input.readUTF(),
                password = input.readUTF(),
                createdAtMillis = input.readLong(),
                updatedAtMillis = input.readLong(),
                lastUsedAtMillis = input.readLong().takeIf { it != NEVER },
                timesUsed = input.readInt(),
            )
            val draft = VaultLoginDraft(login.origin, login.formActionOrigin, login.httpRealm, login.username, login.password)
            if (login.id.isBlank() || login.timesUsed < 0 || !CredentialVaultRules.accepts(draft)) return null
            login
        }
        return if (input.read() != -1 || logins.map(VaultLogin::id).distinct().size != logins.size) null else logins
    }

    private const val NEVER = Long.MIN_VALUE

    private fun DataOutputStream.writeOptional(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeUTF(value)
    }

    private fun DataInputStream.readOptional(): String? = if (readBoolean()) readUTF() else null
}
