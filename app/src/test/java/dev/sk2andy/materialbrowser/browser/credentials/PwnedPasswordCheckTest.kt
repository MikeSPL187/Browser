package dev.sk2andy.materialbrowser.browser.credentials

import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PwnedPasswordCheckTest {
    @Test
    fun `only five characters of each hash leave, once per prefix however many logins share it`() = runBlocking {
        val asked = Collections.synchronizedList(mutableListOf<String>())
        val check = PwnedPasswordCheck(fetchRange = { prefix ->
            asked += prefix
            "1E4C9B93F3F0682250B6CF8331B7EE68FD8:3861493\r\n0018A45C4D1DEF81644B54AB7F969B88D65:0"
        })
        val logins = listOf(
            login("1", "https://a.example", "password"),
            login("2", "https://b.example", "password"),
            login("3", "https://c.example", "password"),
        )

        val result = check.checkLeaks(logins)

        assertEquals(listOf("5BAA6"), asked)
        assertEquals(mapOf("1" to 3_861_493, "2" to 3_861_493, "3" to 3_861_493), result.breaches)
        assertTrue(result.complete)
    }

    @Test
    fun `a prefix without an answer leaves its logins unchecked, never clean`() = runBlocking {
        val leakedHash = PwnedPasswordsClient.sha1Hex("password")
        val check = PwnedPasswordCheck(fetchRange = { prefix ->
            if (prefix == leakedHash.take(5)) "${leakedHash.drop(5)}:7" else null
        })

        val result = check.checkLeaks(
            listOf(login("1", "https://a.example", "password"), login("2", "https://b.example", "kV7#qe2Lm!Tz9pWf")),
        )

        assertEquals(mapOf("1" to 7), result.breaches)
        assertEquals(setOf("2"), result.unchecked)
        assertFalse(result.complete)
    }

    @Test
    fun `a password missing from the range or padded with a zero count is clean`() = runBlocking {
        val paddedHash = PwnedPasswordsClient.sha1Hex("kV7#qe2Lm!Tz9pWf")
        val check = PwnedPasswordCheck(fetchRange = { prefix ->
            if (prefix == paddedHash.take(5)) "${paddedHash.drop(5)}:0\r\nFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF:4" else ""
        })

        val result = check.checkLeaks(
            listOf(login("1", "https://a.example", "kV7#qe2Lm!Tz9pWf"), login("2", "https://b.example", "Tr0ub4dor&3-horse!")),
        )

        assertEquals(emptyMap<String, Int>(), result.breaches)
        assertTrue(result.complete)
    }

    @Test
    fun `an empty password is not asked about`() = runBlocking {
        val check = PwnedPasswordCheck(fetchRange = { error("nothing to ask") })

        val result = check.checkLeaks(listOf(login("1", "https://a.example", "")))

        assertEquals(LeakCheckResult(emptyMap(), emptySet()), result)
    }

    @Test
    fun `progress counts answered prefixes from zero to all`() = runBlocking {
        val logins = (1..6).map { index -> login("$index", "https://site$index.example", "password-$index") }
        val total = logins.map { PwnedPasswordsClient.sha1Hex(it.password).take(5) }.distinct().size
        val seen = Collections.synchronizedList(mutableListOf<LeakCheckProgress>())

        PwnedPasswordCheck(fetchRange = { "" }).checkLeaks(logins) { seen += it }

        assertEquals(LeakCheckProgress(0, total), seen.first())
        assertEquals((0..total).toList(), seen.map { it.answered }.sorted())
        assertEquals(1f, seen.maxBy { it.answered }.fraction, 0f)
    }

    @Test
    fun `no more requests run at once than allowed`() = runBlocking {
        val running = AtomicInteger()
        val most = AtomicInteger()
        val check = PwnedPasswordCheck(
            fetchRange = {
                most.accumulateAndGet(running.incrementAndGet()) { a, b -> maxOf(a, b) }
                Thread.sleep(20)
                running.decrementAndGet()
                ""
            },
            parallelRequests = 2,
        )

        check.checkLeaks((1..12).map { index -> login("$index", "https://site$index.example", "secret-$index") })

        assertTrue("at most 2 at once, saw ${most.get()}", most.get() in 1..2)
    }

    @Test
    fun `without a network the check stops asking after two failures in a row`() = runBlocking {
        val asked = AtomicInteger()
        val check = PwnedPasswordCheck(fetchRange = { asked.incrementAndGet(); null }, parallelRequests = 1)
        val logins = (1..20).map { index -> login("$index", "https://site$index.example", "secret-$index") }

        val result = check.checkLeaks(logins)

        assertEquals(2, asked.get())
        assertEquals(logins.map { it.id }.toSet(), result.unchecked)
    }

    @Test
    fun `one failure between answers does not stop the check`() = runBlocking {
        val asked = AtomicInteger()
        val check = PwnedPasswordCheck(
            fetchRange = { if (asked.incrementAndGet() % 2 == 0) null else "" },
            parallelRequests = 1,
        )
        val logins = (1..10).map { index -> login("$index", "https://site$index.example", "secret-$index") }
        val prefixes = logins.map { PwnedPasswordsClient.sha1Hex(it.password).take(5) }.distinct().size

        check.checkLeaks(logins)

        assertEquals(prefixes, asked.get())
    }

    @Test
    fun `prefixes left after the deadline stay unchecked`() = runBlocking {
        val clock = AtomicInteger()
        val asked = AtomicInteger()
        val check = PwnedPasswordCheck(
            fetchRange = { asked.incrementAndGet(); "" },
            parallelRequests = 1,
            deadlineMillis = 3,
            nowMillis = { clock.getAndIncrement().toLong() },
        )
        val logins = (1..10).map { index -> login("$index", "https://site$index.example", "secret-$index") }

        val result = check.checkLeaks(logins)

        assertTrue("asked ${asked.get()}", asked.get() in 1..4)
        assertFalse(result.complete)
    }

    @Test
    fun `the report finds repeats and weak passwords offline and adds the leaks of a check`() {
        val check = PwnedPasswordCheck(fetchRange = { error("the report never goes online") })
        val logins = listOf(
            login("1", "https://a.example", "Same-Strong-Pass-42!"),
            login("2", "https://b.example", "Same-Strong-Pass-42!"),
            login("3", "https://c.example", "sunshine"),
            login("4", "https://d.example", "kV7#qe2Lm!Tz9pWf"),
        )

        val offline = check.report(logins)
        val withLeaks = check.report(logins, LeakCheckResult(breaches = mapOf("4" to 2), unchecked = emptySet()))

        assertEquals(listOf("1", "2"), offline.reused.map { it.login.id })
        assertEquals(listOf("3"), offline.weak.map { it.login.id })
        assertEquals(emptyList<String>(), offline.breached.map { it.login.id })
        assertEquals(listOf("4" to 2), withLeaks.breached.map { it.login.id to it.timesSeen })
    }

    private fun login(id: String, origin: String, password: String) = VaultLogin(
        id = id,
        origin = origin,
        formActionOrigin = null,
        httpRealm = null,
        username = "user$id",
        password = password,
        createdAtMillis = 0,
        updatedAtMillis = 0,
        lastUsedAtMillis = null,
        timesUsed = 0,
    )
}
