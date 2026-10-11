package dev.sk2andy.materialbrowser.browser.gecko

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Passkeys option A against a GeckoView update, for every class of it: each Play services class,
 * field or method GeckoView names exists in the stand-ins (`src/full/java/com/google`) or is on
 * [KNOWN_UNREACHABLE], code GeckoView runs only after a stand-in has refused. One test per reference,
 * named by its key, so the CI log names what a new GeckoView started to call. Because every absent
 * reference has to pass here first, the broad `-dontwarn` in `proguard-gecko-rules.pro` cannot hide one.
 */
@RunWith(Parameterized::class)
class GeckoPlayServicesReferenceTest(private val key: String) {
    @Test
    fun existsInAStandInOrRunsOnlyAfterARefusal() {
        val reference = referencesByKey[key]
            ?: return fail("GeckoView no longer names $key; drop it from KNOWN_UNREACHABLE")
        if (GeckoPlayServicesReferences.resolves(reference)) {
            assertTrue("$key has a stand-in; drop it from KNOWN_UNREACHABLE", key !in KNOWN_UNREACHABLE)
        } else {
            assertTrue(
                "GeckoView names $key, which no stand-in has. Read the code that names it at the GeckoView " +
                    "tag: add a stand-in when it can run before a stand-in refuses, otherwise add it to " +
                    "KNOWN_UNREACHABLE",
                key in KNOWN_UNREACHABLE,
            )
        }
    }

    companion object {
        private val referencesByKey by lazy { GeckoPlayServicesReferences.inGeckoView().associateBy { it.key } }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun keys(): List<String> = (referencesByKey.keys + KNOWN_UNREACHABLE).sorted()

        /**
         * Play services GeckoView names only after a stand-in has refused, with the reasons:
         * `gecko_play_services_unreachable.txt` in the test resources.
         */
        val KNOWN_UNREACHABLE: Set<String> by lazy {
            val stream = checkNotNull(GeckoPlayServicesReferenceTest::class.java.getResourceAsStream(UNREACHABLE_LIST))
            stream.bufferedReader().useLines { lines ->
                lines.map(String::trim).filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()
            }
        }

        private const val UNREACHABLE_LIST = "/gecko_play_services_unreachable.txt"
    }
}
