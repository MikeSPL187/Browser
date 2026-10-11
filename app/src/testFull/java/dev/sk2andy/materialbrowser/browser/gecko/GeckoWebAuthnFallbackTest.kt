package dev.sk2andy.materialbrowser.browser.gecko

import dev.sk2andy.materialbrowser.browser.gecko.GeckoPlayServicesReferences.GMS
import dev.sk2andy.materialbrowser.browser.gecko.GeckoPlayServicesReferences.Kind
import dev.sk2andy.materialbrowser.browser.gecko.GeckoPlayServicesReferences.Reference
import dev.sk2andy.materialbrowser.browser.gecko.GeckoPlayServicesReferences.TOKEN_MANAGER
import dev.sk2andy.materialbrowser.browser.gecko.GeckoPlayServicesReferences.WEBAUTHN_UTILS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Guards passkeys option A against a GeckoView update. GeckoView's WebAuthn code calls Google Play
 * services FIDO, which Vola replaces with refusing stand-ins (`src/full/java/com/google`). Every
 * call GeckoView makes before a stand-in refuses must exist in them: a missing class or method is a
 * `NoClassDefFoundError` on the Gecko thread, which crashed Gecko on accounts.google.com (#123, H1).
 *
 * The references are read from GeckoView's own class files. If this fails after a GeckoView update,
 * re-read `WebAuthnTokenManager` at the new tag and update the stand-ins, not this list alone.
 * [GeckoPlayServicesReferenceTest] checks every other reference GeckoView makes.
 */
class GeckoWebAuthnFallbackTest {
    @Test
    fun algorithmsListedWhenGeckoLoadsWebAuthnExist() {
        val references = references(TOKEN_MANAGER)
        val algorithms = references.filter {
            it.kind == Kind.Field && (it.owner == EC2 || it.owner == RSA)
        }
        assertEquals(
            setOf("ED256", "ED512", "ES256", "ES384", "ES512", "PS256", "PS384", "PS512", "RS256", "RS384", "RS512"),
            algorithms.map { it.name }.toSet(),
        )
        algorithms.forEach(::assertResolves)
    }

    @Test
    fun platformAuthenticatorQueryReachesARefusingClient() {
        val references = references(TOKEN_MANAGER)
        val privileged = Reference(Kind.Method, FIDO, "getFido2PrivilegedApiClient", "(L$CONTEXT;)L$PRIVILEGED_CLIENT;")
        val regular = Reference(Kind.Method, FIDO, "getFido2ApiClient", "(L$CONTEXT;)L$CLIENT;")
        // GeckoView builds one of the two branches, chosen by its MOZILLA_OFFICIAL constant.
        assertTrue("GeckoView no longer asks Fido for a client", privileged in references || regular in references)
        val required = buildList {
            add(Reference(Kind.Method, TASK, "addOnSuccessListener", "(L$GMS/tasks/OnSuccessListener;)L$TASK;"))
            add(Reference(Kind.Method, TASK, "addOnFailureListener", "(L$GMS/tasks/OnFailureListener;)L$TASK;"))
            if (privileged in references) {
                add(privileged)
                add(Reference(Kind.Method, PRIVILEGED_CLIENT, UVPAA, "()L$TASK;"))
                add(Reference(Kind.Method, PRIVILEGED_CLIENT, "getCredentialList", "(Ljava/lang/String;)L$TASK;"))
            }
            if (regular in references) {
                add(regular)
                add(Reference(Kind.Method, CLIENT, UVPAA, "()L$TASK;"))
            }
        }
        required.forEach { assertReferencedAndResolves(references, it) }
    }

    @Test
    fun passkeyFallbacksStartWithAStandInThatRefuses() {
        val references = references(TOKEN_MANAGER)
        listOf(
            Reference(Kind.Method, "$COMMON/PublicKeyCredentialCreationOptions\$Builder", "<init>", "()V"),
            Reference(Kind.Method, "$COMMON/AuthenticationExtensions\$Builder", "<init>", "()V"),
            Reference(
                Kind.Method,
                "$COMMON/PublicKeyCredentialDescriptor",
                "<init>",
                "(Ljava/lang/String;[BLjava/util/List;)V",
            ),
            Reference(Kind.Field, "$COMMON/PublicKeyCredentialType", "PUBLIC_KEY", "L$COMMON/PublicKeyCredentialType;"),
            Reference(Kind.Method, "$COMMON/PublicKeyCredentialType", "toString", "()Ljava/lang/String;"),
        ).forEach { assertReferencedAndResolves(references, it) }
    }

    @Test
    fun transportsMappedBeforeTheSignInFallbackExist() {
        val references = references(WEBAUTHN_UTILS)
        listOf("USB", "NFC", "BLUETOOTH_LOW_ENERGY", "INTERNAL", "HYBRID").forEach { name ->
            assertReferencedAndResolves(
                references,
                Reference(Kind.Field, "$GMS/fido/common/Transport", name, "L$GMS/fido/common/Transport;"),
            )
        }
    }

    private fun references(className: String) = GeckoPlayServicesReferences.inClass(className)

    private fun assertReferencedAndResolves(references: Set<Reference>, reference: Reference) {
        assertTrue("GeckoView no longer calls $reference; re-audit the stand-ins", reference in references)
        assertResolves(reference)
    }

    private fun assertResolves(reference: Reference) {
        if (GeckoPlayServicesReferences.ownerClass(reference) == null) fail("Missing stand-in class for $reference")
        assertTrue("Stand-in lacks $reference", GeckoPlayServicesReferences.resolves(reference))
    }

    private companion object {
        const val COMMON = "$GMS/fido/fido2/api/common"
        const val EC2 = "$COMMON/EC2Algorithm"
        const val RSA = "$COMMON/RSAAlgorithm"
        const val FIDO = "$GMS/fido/Fido"
        const val CLIENT = "$GMS/fido/fido2/Fido2ApiClient"
        const val PRIVILEGED_CLIENT = "$GMS/fido/fido2/Fido2PrivilegedApiClient"
        const val TASK = "$GMS/tasks/Task"
        const val CONTEXT = "android/content/Context"
        const val UVPAA = "isUserVerifyingPlatformAuthenticatorAvailable"
    }
}
