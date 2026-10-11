package dev.sk2andy.materialbrowser.browser.credentials

import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthReport
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin

/**
 * The password check (v5 V24, after Chrome's): which saved passwords need changing.
 *
 * Two steps with different privacy. [report] finds repeated and weak passwords on the phone and
 * never touches the network. [checkLeaks] is the only step that goes out, and only when the user
 * asks for it: it sends the first five characters of each password's SHA-1 hash (k-anonymity) and
 * nothing about the login, the site or the user name.
 */
internal interface PasswordCheck {
    /** Repeated and weak passwords, plus the leaks of an earlier [checkLeaks], if any. Offline. */
    fun report(logins: List<VaultLogin>, leaks: LeakCheckResult? = null): PasswordHealthReport

    /**
     * Asks which passwords of [logins] were seen in leaks. Call it from a user action only. It
     * reports progress as [LeakCheckProgress] from a worker thread and can be cancelled.
     */
    suspend fun checkLeaks(
        logins: List<VaultLogin>,
        onProgress: (LeakCheckProgress) -> Unit = {},
    ): LeakCheckResult
}

/** How far a leak check is: [answered] of [total] requests, one per distinct hash prefix. */
internal data class LeakCheckProgress(val answered: Int, val total: Int) {
    /** Share done, from 0 to 1; 1 when there was nothing to ask. */
    val fraction: Float get() = if (total == 0) 1f else answered.toFloat() / total
}

/**
 * What a leak check found. [breaches] maps a login id to how often its password was seen in leaks,
 * leaked logins only; [unchecked] lists the logins whose answer did not come, so a failed request
 * never reads as a clean password.
 */
internal data class LeakCheckResult(
    val breaches: Map<String, Int>,
    val unchecked: Set<String>,
) {
    /** Whether every login got an answer. */
    val complete: Boolean get() = unchecked.isEmpty()
}
