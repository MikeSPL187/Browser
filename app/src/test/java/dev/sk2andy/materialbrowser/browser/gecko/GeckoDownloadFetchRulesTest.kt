package dev.sk2andy.materialbrowser.browser.gecko

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.geckoview.GeckoWebExecutor

class GeckoDownloadFetchRulesTest {
    @Test
    fun `a tab in the shared jar fetches with its cookies`() {
        val owner = GeckoDownloadOwner("default", isPrivate = false, sessionKey = Any(), contextId = null)

        assertEquals(
            GeckoWebExecutor.FETCH_FLAGS_NONE,
            GeckoDownloadFetchRules.flags(GeckoWebExecutor.FETCH_FLAGS_NONE, owner),
        )
    }

    @Test
    fun `an isolated profile's tab fetches without the shared jar's cookies`() {
        val owner = GeckoDownloadOwner("work", isPrivate = false, sessionKey = Any(), contextId = "work")

        assertEquals(
            GeckoWebExecutor.FETCH_FLAGS_ANONYMOUS,
            GeckoDownloadFetchRules.flags(GeckoWebExecutor.FETCH_FLAGS_NONE, owner),
        )
    }

    @Test
    fun `a private tab fetches privately and without cookies`() {
        val owner = GeckoDownloadOwner(
            "default",
            isPrivate = true,
            sessionKey = Any(),
            contextId = "private:default",
        )

        assertEquals(
            GeckoWebExecutor.FETCH_FLAGS_PRIVATE or GeckoWebExecutor.FETCH_FLAGS_ANONYMOUS,
            GeckoDownloadFetchRules.flags(GeckoWebExecutor.FETCH_FLAGS_NONE, owner),
        )
    }

    @Test
    fun `flags the caller asked for are kept`() {
        val owner = GeckoDownloadOwner("work", isPrivate = false, sessionKey = Any(), contextId = "work")

        assertEquals(
            GeckoWebExecutor.FETCH_FLAGS_NO_REDIRECTS or GeckoWebExecutor.FETCH_FLAGS_ANONYMOUS,
            GeckoDownloadFetchRules.flags(GeckoWebExecutor.FETCH_FLAGS_NO_REDIRECTS, owner),
        )
    }
}
