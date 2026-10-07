package dev.sk2andy.materialbrowser.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunPresentationRulesTest {
    @Test
    fun `a due first run shows at once on a launcher start`() {
        assertTrue(FirstRunPresentationRules.showNow(onboardingRequired = true, isColdExternalLinkLaunch = false))
        assertFalse(FirstRunPresentationRules.showNow(onboardingRequired = false, isColdExternalLinkLaunch = false))
    }

    @Test
    fun `a cold start from a link shows the page and keeps the first run waiting`() {
        assertFalse(FirstRunPresentationRules.showNow(onboardingRequired = true, isColdExternalLinkLaunch = true))
    }

    @Test
    fun `a waiting first run appears on the next launcher intent only`() {
        assertTrue(FirstRunPresentationRules.showOnNewIntent(isLauncherLaunch = true, isOnboardingCompleted = false))
        // Another link keeps it waiting; a finished first run never comes back.
        assertFalse(FirstRunPresentationRules.showOnNewIntent(isLauncherLaunch = false, isOnboardingCompleted = false))
        assertFalse(FirstRunPresentationRules.showOnNewIntent(isLauncherLaunch = true, isOnboardingCompleted = true))
    }
}
