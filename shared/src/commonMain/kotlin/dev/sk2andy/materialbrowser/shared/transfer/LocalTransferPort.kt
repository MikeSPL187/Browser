package dev.sk2andy.materialbrowser.shared.transfer

/**
 * Moving tabs and bookmarks to another phone on the same Wi-Fi (V26): one tap, no server, nothing
 * in the background. The platform adapter runs the one-shot local server, the QR code and the
 * camera; the screens see only this port. Callbacks arrive on the main thread.
 */
interface LocalTransferPort {
    /**
     * Offers [payload] to the next phone that scans the code: [onState] hears [LocalTransferOfferState.Waiting]
     * with the code to show, then how the offer ended. [LocalTransferHandle.cancel] closes it early.
     */
    fun offer(payload: LocalTransferPayload, onState: (LocalTransferOfferState) -> Unit): LocalTransferHandle

    /** Fetches the payload a scanned or typed [code] offers. */
    fun receive(code: String, onResult: (LocalTransferReceiveResult) -> Unit): LocalTransferHandle

    /** A platform without the transfer (no Wi-Fi address, no camera): every call fails at once. */
    object Unavailable : LocalTransferPort {
        override fun offer(payload: LocalTransferPayload, onState: (LocalTransferOfferState) -> Unit): LocalTransferHandle {
            onState(LocalTransferOfferState.Failed(LocalTransferFailure.Unavailable))
            return LocalTransferHandle.Done
        }

        override fun receive(code: String, onResult: (LocalTransferReceiveResult) -> Unit): LocalTransferHandle {
            onResult(LocalTransferReceiveResult.Failed(LocalTransferFailure.Unavailable))
            return LocalTransferHandle.Done
        }
    }
}

/** A running offer or receive; [cancel] ends it and frees the port or the connection. */
fun interface LocalTransferHandle {
    fun cancel()

    companion object {
        val Done = LocalTransferHandle {}
    }
}

sealed interface LocalTransferOfferState {
    /** Show [code] as a QR code until [expiresAtEpochSeconds]. */
    data class Waiting(val code: String, val expiresAtEpochSeconds: Long) : LocalTransferOfferState

    /** The other phone took the payload; the server is closed. */
    data object Sent : LocalTransferOfferState

    data class Failed(val reason: LocalTransferFailure) : LocalTransferOfferState
}

sealed interface LocalTransferReceiveResult {
    data class Received(val payload: LocalTransferPayload) : LocalTransferReceiveResult

    data class Failed(val reason: LocalTransferFailure) : LocalTransferReceiveResult
}

enum class LocalTransferFailure {
    /** The phone cannot transfer: no Wi-Fi address or no camera. */
    Unavailable,

    /** The code is not a Vola transfer code, or it was cut. */
    InvalidCode,

    /** The two minutes ran out before the other phone asked. */
    Expired,

    /** The other phone is not reachable: another network, or it closed the offer. */
    Unreachable,

    /** Too many wrong requests reached the offer; it closed to stay safe. */
    TooManyAttempts,

    /** The answer did not open with the code's key, or was not a payload. */
    Corrupt,

    /** The payload is above the size limit. */
    TooLarge,
}
