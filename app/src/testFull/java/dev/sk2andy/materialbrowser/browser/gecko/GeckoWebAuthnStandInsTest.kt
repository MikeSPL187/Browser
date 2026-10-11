package dev.sk2andy.materialbrowser.browser.gecko

import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.Fido2ApiClient
import com.google.android.gms.fido.fido2.Fido2PrivilegedApiClient
import com.google.android.gms.fido.fido2.api.common.AuthenticationExtensions
import com.google.android.gms.fido.fido2.api.common.EC2Algorithm
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import com.google.android.gms.fido.fido2.api.common.RSAAlgorithm
import com.google.android.gms.tasks.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The stand-ins answer GeckoView the way a device without Google Play services FIDO would. */
class GeckoWebAuthnStandInsTest {
    @Test
    fun platformAuthenticatorQueryFailsAtOnceAndNeverSucceeds() {
        listOf(
            Fido2ApiClient().isUserVerifyingPlatformAuthenticatorAvailable(),
            Fido2PrivilegedApiClient().isUserVerifyingPlatformAuthenticatorAvailable(),
        ).forEach { task ->
            var succeeded = false
            var failure: Exception? = null
            // GeckoView adds the success listener first, then the failure one, and answers false.
            task.addOnSuccessListener { succeeded = true }
            task.addOnFailureListener { failure = it }
            assertFalse(succeeded)
            assertTrue(failure is UnsupportedOperationException)
        }
    }

    @Test
    fun unavailableTaskReportsEveryFailureListener() {
        val task: Task<String> = Task.unavailable()
        var failures = 0
        task.addOnFailureListener { failures++ }.addOnFailureListener { failures++ }
        assertEquals(2, failures)
    }

    @Test
    fun everyPasskeyRequestIsRefusedWithAnException() {
        // GeckoView catches Exception around these calls and rejects the page's promise.
        assertThrows(UnsupportedOperationException::class.java) {
            Fido2PrivilegedApiClient().getCredentialList("example.com")
        }
        assertThrows(UnsupportedOperationException::class.java) {
            PublicKeyCredentialCreationOptions.Builder()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            AuthenticationExtensions.Builder()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            PublicKeyCredentialDescriptor(
                PublicKeyCredentialType.PUBLIC_KEY.toString(),
                byteArrayOf(1),
                listOf(Transport.INTERNAL),
            )
        }
    }

    @Test
    fun algorithmsCarryTheirCoseIdentifiers() {
        assertEquals(
            mapOf("ED256" to -260, "ED512" to -261, "ES256" to -7, "ES384" to -35, "ES512" to -36),
            EC2Algorithm.values().associate { it.name to it.algoValue },
        )
        assertEquals(
            mapOf(
                "RS256" to -257, "RS384" to -258, "RS512" to -259,
                "PS256" to -37, "PS384" to -38, "PS512" to -39,
            ),
            RSAAlgorithm.values().associate { it.name to it.algoValue },
        )
        assertEquals("public-key", PublicKeyCredentialType.PUBLIC_KEY.toString())
    }
}
