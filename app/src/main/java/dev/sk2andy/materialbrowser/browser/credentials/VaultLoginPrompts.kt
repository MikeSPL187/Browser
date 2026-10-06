package dev.sk2andy.materialbrowser.browser.credentials

import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** A saved account the sign-in sheet offers: the user name, never the password. */
internal data class VaultLoginAccount(
    val username: String,
    val lastUsedAtMillis: Long?,
) {
    override fun toString(): String = "VaultLoginAccount(username=<redacted>)"
}

/** One sheet the Vola password host asks the browser window to show. */
internal sealed interface VaultLoginRequest {
    val id: Long

    /** The window that asked; another browser window does not show it. */
    val windowId: Int

    /** The site as people read it, for the title. */
    val site: String

    /** «Save password?» or, when the user name is already saved here, «Update password?». */
    data class Save(
        override val id: Long,
        override val windowId: Int,
        override val site: String,
        val username: String,
        val update: Boolean,
    ) : VaultLoginRequest {
        override fun toString(): String = "VaultLoginRequest.Save(id=$id, update=$update)"
    }

    /** «Strong password» for a new-password field (board W-Generator). */
    data class Generate(
        override val id: Long,
        override val windowId: Int,
        override val site: String,
        /** The vault is set up, so the password is saved as it is used. */
        val canSave: Boolean,
    ) : VaultLoginRequest

    /** «Keep passwords in Vola» when a sign-in field is touched and the vault is not set up. */
    data class Offer(
        override val id: Long,
        override val windowId: Int,
        override val site: String,
    ) : VaultLoginRequest

    /** «Sign in to …» with the accounts saved for exactly this site (board W-Autofill). */
    data class Select(
        override val id: Long,
        override val windowId: Int,
        override val site: String,
        val accounts: List<VaultLoginAccount>,
    ) : VaultLoginRequest
}

internal sealed interface VaultLoginAnswer {
    /** «Save» or «Update». */
    data object Save : VaultLoginAnswer

    /** An account was picked to fill. */
    data class Pick(val username: String) : VaultLoginAnswer {
        override fun toString(): String = "VaultLoginAnswer.Pick(username=<redacted>)"
    }

    /** A generated password to put in the field. */
    class Use(val password: String) : VaultLoginAnswer {
        override fun toString(): String = "VaultLoginAnswer.Use(password=<redacted>)"
    }

    /** «Set up» on the vault offer. */
    data object SetUp : VaultLoginAnswer

    /** «Not now», «Don't fill», back, or the page went away. */
    data object Dismiss : VaultLoginAnswer
}

/**
 * Where the Vola password host and the browser window meet: the host posts a request, the window
 * shows it as a sheet and answers. One sheet at a time; a new request dismisses the one before, and
 * every request is answered exactly once.
 */
internal object VaultLoginPrompts {
    var current by mutableStateOf<VaultLoginRequest?>(null)
        private set

    private var onAnswer: ((VaultLoginAnswer) -> Unit)? = null
    private var nextId = 1L

    fun nextRequestId(): Long = nextId++

    @MainThread
    fun show(request: VaultLoginRequest, callback: (VaultLoginAnswer) -> Unit) {
        current?.let { previous -> answer(previous.id, VaultLoginAnswer.Dismiss) }
        current = request
        onAnswer = callback
    }

    @MainThread
    fun answer(id: Long, answer: VaultLoginAnswer) {
        if (current?.id != id) return
        val callback = onAnswer
        current = null
        onAnswer = null
        callback?.invoke(answer)
    }
}
