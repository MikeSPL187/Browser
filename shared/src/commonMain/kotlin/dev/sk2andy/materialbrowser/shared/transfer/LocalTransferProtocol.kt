package dev.sk2andy.materialbrowser.shared.transfer

import dev.sk2andy.materialbrowser.sync.SyncBase64
import dev.sk2andy.materialbrowser.sync.parseStrictJsonObject
import dev.sk2andy.materialbrowser.sync.strictInt
import dev.sk2andy.materialbrowser.sync.strictString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The primitives the transfer needs, supplied by the platform (javax.crypto on Android), so the
 * protocol itself stays in shared code and its tests can use a fake.
 */
interface LocalTransferCrypto {
    fun randomBytes(size: Int): ByteArray

    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray

    /** AES-256-GCM with a 128-bit tag; [nonce] is 12 bytes. */
    fun aesGcmSeal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray

    /** The plaintext, or null when the tag does not match (wrong key, changed bytes or AAD). */
    fun aesGcmOpen(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, aad: ByteArray): ByteArray?
}

/**
 * The local transfer protocol, version 1 (V26). The sending device shows a [LocalTransferPairing]
 * as a QR code and listens on the Wi-Fi; the receiving device scans it and makes one request.
 *
 * - **Request** (`POST /v1/<session>`, [request]): a fresh 16-byte nonce and a proof — an HMAC of
 *   the session and that nonce under the pairing's authentication key. Only the phone that read
 *   the code can make it, so another device on the Wi-Fi can neither fetch the payload nor use up
 *   the one-shot session ([checkRequest]).
 * - **Response** ([seal], [open]): the payload, AES-256-GCM under the pairing's encryption key,
 *   with the session and the request's nonce as associated data, so an old response cannot be
 *   replayed to a new request.
 * - The two keys are derived from the code's key by HMAC with distinct labels; the code's key is
 *   never used directly and never leaves the phones' screens.
 */
object LocalTransferProtocol {
    const val VERSION = 1
    const val NONCE_BYTES = 16
    const val GCM_NONCE_BYTES = 12
    private const val PROOF_BYTES = 32
    private const val TAG_BYTES = 16

    /** The path the receiving device posts to. */
    fun path(pairing: LocalTransferPairing): String = "/v1/${SyncBase64.encode(pairing.sessionId)}"

    /** The receiving device's request body and the nonce it must keep to [open] the answer. */
    fun request(pairing: LocalTransferPairing, crypto: LocalTransferCrypto): LocalTransferRequest {
        val nonce = crypto.randomBytes(NONCE_BYTES)
        val body = buildJsonObject {
            put("v", VERSION)
            put("nonce", SyncBase64.encode(nonce))
            put("proof", SyncBase64.encode(proof(pairing, nonce, crypto)))
        }.toString()
        return LocalTransferRequest(body, nonce)
    }

    /**
     * The nonce of a request [body] that proves the code's key for [pairing]; null for anything
     * else. The sending device answers only such a request, and only once.
     */
    fun checkRequest(pairing: LocalTransferPairing, body: String, crypto: LocalTransferCrypto): ByteArray? = runCatching {
        require(body.length <= MAX_REQUEST_CHARS)
        val root = parseStrictJsonObject(body)
        require(root.keys == setOf("v", "nonce", "proof"))
        require(root.strictInt("v") == VERSION)
        val nonce = SyncBase64.decode(root.strictString("nonce"), expectedBytes = NONCE_BYTES)
        val claimed = SyncBase64.decode(root.strictString("proof"), expectedBytes = PROOF_BYTES)
        nonce.takeIf { constantTimeEquals(claimed, proof(pairing, it, crypto)) }
    }.getOrNull()

    /**
     * The sending device's answer to a request with [requestNonce]: the sealed payload as JSON.
     * Throws [IllegalArgumentException] for a payload above [LocalTransferPayloadCodec.MAX_PAYLOAD_BYTES].
     */
    fun seal(
        pairing: LocalTransferPairing,
        requestNonce: ByteArray,
        payload: LocalTransferPayload,
        crypto: LocalTransferCrypto,
    ): String {
        val nonce = crypto.randomBytes(GCM_NONCE_BYTES)
        val plaintext = LocalTransferPayloadCodec.encode(payload).encodeToByteArray()
        require(plaintext.size <= LocalTransferPayloadCodec.MAX_PAYLOAD_BYTES) { "Payload too large" }
        val ciphertext = crypto.aesGcmSeal(encryptionKey(pairing, crypto), nonce, plaintext, aad(pairing, requestNonce))
        return buildJsonObject {
            put("v", VERSION)
            put("iv", SyncBase64.encode(nonce))
            put("ct", SyncBase64.encode(ciphertext))
        }.toString()
    }

    /** The payload in the answer [body] to the request with [requestNonce]; null when it does not open. */
    fun open(
        pairing: LocalTransferPairing,
        requestNonce: ByteArray,
        body: String,
        crypto: LocalTransferCrypto,
    ): LocalTransferPayload? = runCatching {
        require(body.length <= MAX_RESPONSE_CHARS)
        val root = parseStrictJsonObject(body)
        require(root.keys == setOf("v", "iv", "ct"))
        require(root.strictInt("v") == VERSION)
        val nonce = SyncBase64.decode(root.strictString("iv"), expectedBytes = GCM_NONCE_BYTES)
        val ciphertext = SyncBase64.decode(root.strictString("ct", maximum = MAX_CIPHERTEXT_CHARS), maxBytes = MAX_CIPHERTEXT_BYTES)
        val plaintext = crypto.aesGcmOpen(encryptionKey(pairing, crypto), nonce, ciphertext, aad(pairing, requestNonce))
            ?: return@runCatching null
        require(plaintext.size <= LocalTransferPayloadCodec.MAX_PAYLOAD_BYTES)
        LocalTransferPayloadCodec.decode(plaintext.decodeToString(throwOnInvalidSequence = true))
    }.getOrNull()

    /** Compares secrets in time that does not depend on where they differ. */
    fun constantTimeEquals(first: ByteArray, second: ByteArray): Boolean {
        if (first.size != second.size) return false
        var difference = 0
        for (index in first.indices) difference = difference or (first[index].toInt() xor second[index].toInt())
        return difference == 0
    }

    private fun proof(pairing: LocalTransferPairing, nonce: ByteArray, crypto: LocalTransferCrypto): ByteArray =
        crypto.hmacSha256(
            crypto.hmacSha256(pairing.key, AUTH_LABEL),
            REQUEST_LABEL + pairing.sessionId + nonce,
        )

    private fun encryptionKey(pairing: LocalTransferPairing, crypto: LocalTransferCrypto): ByteArray =
        crypto.hmacSha256(pairing.key, ENCRYPTION_LABEL)

    private fun aad(pairing: LocalTransferPairing, requestNonce: ByteArray): ByteArray =
        PAYLOAD_LABEL + pairing.sessionId + requestNonce

    private val AUTH_LABEL = "vola-pair/v1/auth-key".encodeToByteArray()
    private val ENCRYPTION_LABEL = "vola-pair/v1/encryption-key".encodeToByteArray()
    private val REQUEST_LABEL = "vola-pair/v1/request".encodeToByteArray()
    private val PAYLOAD_LABEL = "vola-pair/v1/payload".encodeToByteArray()
    const val MAX_REQUEST_CHARS = 1_024

    private const val MAX_CIPHERTEXT_BYTES = LocalTransferPayloadCodec.MAX_PAYLOAD_BYTES + TAG_BYTES

    /** Unpadded base64 of [MAX_CIPHERTEXT_BYTES]. */
    private const val MAX_CIPHERTEXT_CHARS = (MAX_CIPHERTEXT_BYTES * 4 + 2) / 3

    /** The sealed payload with room for the envelope's other fields. */
    const val MAX_RESPONSE_CHARS = MAX_CIPHERTEXT_CHARS + 1_024
}

/** A receiving device's request: the [body] to post and the [nonce] that opens the answer. */
class LocalTransferRequest(val body: String, val nonce: ByteArray)
