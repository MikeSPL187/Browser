package dev.sk2andy.materialbrowser.browser.credentials

import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthReport
import dev.sk2andy.materialbrowser.shared.credentials.PasswordHealthRules
import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * [PasswordCheck] on the phone's own rules and Pwned Passwords. Each distinct password is hashed
 * once and each hash prefix is asked once, however many logins share it; a few requests run at
 * once, so hundreds of passwords take seconds. Passwords never leave this class, only prefixes.
 */
internal class PwnedPasswordCheck(
    private val fetchRange: (prefix: String) -> String? = PwnedPasswordsClient::range,
    private val parallelRequests: Int = PARALLEL_REQUESTS,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PasswordCheck {

    override fun report(logins: List<VaultLogin>, leaks: LeakCheckResult?): PasswordHealthReport =
        PasswordHealthRules.report(logins, leaks?.breaches.orEmpty())

    override suspend fun checkLeaks(
        logins: List<VaultLogin>,
        onProgress: (LeakCheckProgress) -> Unit,
    ): LeakCheckResult = withContext(dispatcher) {
        // An empty password is weak on the phone already; there is nothing to ask about it.
        val hashByPassword = logins.asSequence()
            .map(VaultLogin::password)
            .filter(String::isNotEmpty)
            .distinct()
            .associateWith(PwnedPasswordsClient::sha1Hex)
        val prefixes = hashByPassword.values.map(::prefixOf).distinct()
        val answered = AtomicInteger()
        onProgress(LeakCheckProgress(0, prefixes.size))

        val gate = Semaphore(parallelRequests)
        val ranges = prefixes.map { prefix ->
            async {
                val body = gate.withPermit { runInterruptible { fetchRange(prefix) } }
                onProgress(LeakCheckProgress(answered.incrementAndGet(), prefixes.size))
                prefix to body
            }
        }.awaitAll().toMap()

        // Null: the prefix got no answer, so the password is unknown, not clean.
        val seenByHash = hashByPassword.values.distinct().associateWith { hash ->
            ranges[prefixOf(hash)]?.let { body ->
                PasswordHealthRules.timesSeen(body, hash.drop(PasswordHealthRules.HASH_PREFIX_LENGTH))
            }
        }
        val breaches = mutableMapOf<String, Int>()
        val unchecked = mutableSetOf<String>()
        for (login in logins) {
            val hash = hashByPassword[login.password] ?: continue
            when (val seen = seenByHash[hash]) {
                null -> unchecked += login.id
                0 -> Unit
                else -> breaches[login.id] = seen
            }
        }
        LeakCheckResult(breaches, unchecked)
    }

    private fun prefixOf(hash: String): String = hash.take(PasswordHealthRules.HASH_PREFIX_LENGTH)

    private companion object {
        const val PARALLEL_REQUESTS = 4
    }
}
