package dev.sk2andy.materialbrowser.browser.credentials

import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A FedCM question the v5 interface asks in its own sheet instead of a system dialog. */
internal sealed interface IdentityPromptRequest {
    val id: Long

    /** The window that asked: only it shows the sheet. */
    val windowId: Int

    /** Pick one: an identity provider or an account. The answer is the option's index. */
    data class Choice(
        override val id: Long,
        override val windowId: Int,
        val title: String,
        val options: List<String>,
    ) : IdentityPromptRequest

    /** The provider's policies before the first sign-in. The answer is [ACCEPT] or nothing. */
    data class Privacy(
        override val id: Long,
        override val windowId: Int,
        val providerName: String,
        val providerDomain: String,
        val privacyPolicyUrl: String,
        val termsOfServiceUrl: String?,
    ) : IdentityPromptRequest

    companion object {
        const val ACCEPT = 0
    }
}

/**
 * The FedCM questions of [AndroidCredentialPromptHost] while a window draws them in v5 sheets
 * (the phone wholly on v5): the window [attach]es while its sheets are on screen, and the host
 * asks here instead of in an AlertDialog. A window that does not draw them (the tablet, v4) keeps
 * the system dialog. A newer question dismisses the one before it, as the dialogs did.
 */
internal object IdentityCredentialPrompts {
    var current by mutableStateOf<IdentityPromptRequest?>(null)
        private set

    private val windows = mutableSetOf<Int>()
    private var onAnswer: ((Int?) -> Unit)? = null
    private var nextId = 1L

    fun nextRequestId(): Long = nextId++

    @MainThread
    fun attach(windowId: Int) {
        windows += windowId
    }

    @MainThread
    fun detach(windowId: Int) {
        windows -= windowId
        current?.takeIf { it.windowId == windowId }?.let { answer(it.id, null) }
    }

    fun drawsFor(windowId: Int): Boolean = windowId in windows

    @MainThread
    fun show(request: IdentityPromptRequest, callback: (Int?) -> Unit) {
        current?.let { previous -> answer(previous.id, null) }
        current = request
        onAnswer = callback
    }

    /** [answer] is the chosen index, [IdentityPromptRequest.ACCEPT], or null for «Close». */
    @MainThread
    fun answer(id: Long, answer: Int?) {
        if (current?.id != id) return
        val callback = onAnswer
        current = null
        onAnswer = null
        callback?.invoke(answer)
    }
}
