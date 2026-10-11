package dev.sk2andy.materialbrowser.browser.credentials

import org.junit.Assert.assertEquals
import org.junit.Test

class PasskeySupportTest {
    @Test
    fun androidTwelveAndThirteenNeedAndroidFourteen() {
        listOf(31, 32, 33).forEach { sdk ->
            assertEquals(
                PasskeyAvailability.NeedsAndroid14,
                PasskeySupport.availability(sdkInt = sdk, hasCredentialManager = true),
            )
        }
    }

    @Test
    fun androidFourteenNeedsTheSystemCredentialManager() {
        assertEquals(
            PasskeyAvailability.NoCredentialManager,
            PasskeySupport.availability(sdkInt = 34, hasCredentialManager = false),
        )
        assertEquals(
            PasskeyAvailability.Available,
            PasskeySupport.availability(sdkInt = 34, hasCredentialManager = true),
        )
        assertEquals(
            PasskeyAvailability.Available,
            PasskeySupport.availability(sdkInt = 36, hasCredentialManager = true),
        )
    }

    @Test
    fun featureNameMatchesTheFramework() {
        assertEquals("android.software.credentials", PasskeySupport.CREDENTIALS_FEATURE)
    }
}
