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
 * named by its key, so the CI log names what a new GeckoView started to call. [KNOWN_UNREACHABLE]
 * matches `proguard-gecko-rules.pro`, which lets R8 leave exactly these absent.
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
                    "KNOWN_UNREACHABLE and proguard-gecko-rules.pro",
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
         * Play services GeckoView names only on paths that start after a stand-in has refused: it
         * never gets a client result, a credential or a successful task, so no stand-in is needed.
         * Keys as [GeckoPlayServicesReferences.Reference.key].
         */
        val KNOWN_UNREACHABLE = setOf<String>()
    }
}
