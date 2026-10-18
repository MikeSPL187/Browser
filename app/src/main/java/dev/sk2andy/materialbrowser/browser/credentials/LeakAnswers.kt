package dev.sk2andy.materialbrowser.browser.credentials

import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin

/**
 * The leak check's answers kept while the screen lives, by the password's SHA-1 hash rather than
 * by login: a password changed after the check is not the one that was checked, so it reads as
 * unchecked instead of carrying the old answer, and an answer that did not come in a later check
 * keeps the earlier one instead of erasing a known leak.
 */
internal object LeakAnswers {
    /** [answers] with what [result] learned about [logins]; a login it left unchecked keeps its old answer. */
    fun record(answers: Map<String, Int>, logins: List<VaultLogin>, result: LeakCheckResult): Map<String, Int> {
        val learned = logins.asSequence()
            .filter { it.password.isNotEmpty() && it.id !in result.unchecked }
            .associate { PwnedPasswordsClient.sha1Hex(it.password) to (result.breaches[it.id] ?: 0) }
        return answers + learned
    }

    /** How often each login's current password was seen in leaks, leaked logins only. */
    fun breaches(logins: List<VaultLogin>, answers: Map<String, Int>): Map<String, Int> {
        if (answers.isEmpty()) return emptyMap()
        return logins.mapNotNull { login ->
            if (login.password.isEmpty()) return@mapNotNull null
            answers[PwnedPasswordsClient.sha1Hex(login.password)]?.takeIf { it > 0 }?.let { login.id to it }
        }.toMap()
    }

    /** Logins whose current password no check has answered for. */
    fun unchecked(logins: List<VaultLogin>, answers: Map<String, Int>): Int =
        logins.count { it.password.isNotEmpty() && PwnedPasswordsClient.sha1Hex(it.password) !in answers }
}
