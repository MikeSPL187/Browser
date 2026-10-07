package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.shared.browser.BrowserEngineFailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageErrorFeedbackRulesTest {
    @Test
    fun `HTTP 404 becomes not found after navigation finishes`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Hidden,
            error = null,
            httpStatusCode = 404,
            isLoading = false,
            isOnline = true,
        )

        assertEquals(PageErrorFeedbackState.NotFound, observation.state)
        assertFalse(observation.shouldReload)
    }

    @Test
    fun `offline state replaces transport error`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Hidden,
            error = "Network unavailable",
            httpStatusCode = null,
            isLoading = false,
            isOnline = false,
        )

        assertEquals(PageErrorFeedbackState.Offline(), observation.state)
    }

    @Test
    fun `engine offline error never becomes unknown host while connectivity catches up`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Hidden,
            error = "Gecko navigation failed",
            httpStatusCode = null,
            isLoading = false,
            isOnline = true,
            failureKind = BrowserEngineFailureKind.Offline,
        )

        assertEquals(PageErrorFeedbackState.Offline(isOnlineReady = true), observation.state)
    }

    @Test
    fun `engine HTTPS-only warning is never covered by the error page`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Hidden,
            error = "Gecko navigation failed",
            httpStatusCode = null,
            isLoading = false,
            isOnline = true,
            failureKind = BrowserEngineFailureKind.HttpsOnly,
        )

        assertEquals(PageErrorFeedbackState.Hidden, observation.state)
        assertFalse(observation.shouldReload)
    }

    @Test
    fun `a site stopped by safe browsing gets the dangerous-site page, not an error`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Hidden,
            error = "https://login-bank-help.top/",
            httpStatusCode = null,
            isLoading = false,
            isOnline = true,
            failureKind = BrowserEngineFailureKind.DangerousSite,
        )

        assertEquals(PageErrorFeedbackState.Hidden, observation.state)
        assertFalse(observation.shouldReload)
    }

    @Test
    fun `connection loss does not cover an already loaded page`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Hidden,
            error = null,
            httpStatusCode = null,
            isLoading = false,
            isOnline = false,
        )

        assertEquals(PageErrorFeedbackState.Hidden, observation.state)
        assertFalse(observation.shouldReload)
    }

    @Test
    fun `connection return reloads the page once by itself`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Offline(),
            error = "Network unavailable",
            httpStatusCode = null,
            isLoading = false,
            isOnline = true,
        )

        assertEquals(PageErrorFeedbackState.Retrying, observation.state)
        assertTrue(observation.shouldReload)
    }

    @Test
    fun `page that failed again after the automatic reload waits for the Retry button`() {
        val current = PageErrorFeedbackState.Offline(isOnlineReady = true)

        val observation = PageErrorFeedbackRules.observe(
            current = current,
            error = "Gecko navigation failed",
            httpStatusCode = null,
            isLoading = false,
            isOnline = true,
            failureKind = BrowserEngineFailureKind.Offline,
        )

        assertEquals(current, observation.state)
        assertFalse(observation.shouldReload)
    }

    @Test
    fun `a certificate failure is an insecure connection, not a missing site`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Hidden,
            error = "Gecko navigation failed",
            httpStatusCode = null,
            isLoading = false,
            isOnline = true,
            failureKind = BrowserEngineFailureKind.InsecureConnection,
        )

        assertEquals(PageErrorFeedbackState.InsecureConnection, observation.state)
        assertFalse(observation.shouldReload)
    }

    @Test
    fun `unknown host gets its own page while online`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Hidden,
            error = "Gecko navigation failed",
            httpStatusCode = null,
            isLoading = false,
            isOnline = true,
            failureKind = BrowserEngineFailureKind.UnknownHost,
        )

        assertEquals(PageErrorFeedbackState.UnknownHost, observation.state)
    }

    @Test
    fun `unknown host while offline is a missing connection, not a typo`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Hidden,
            error = "Gecko navigation failed",
            httpStatusCode = null,
            isLoading = false,
            isOnline = false,
            failureKind = BrowserEngineFailureKind.UnknownHost,
        )

        assertEquals(PageErrorFeedbackState.Offline(), observation.state)
    }

    @Test
    fun `page sentence names the host without www, or the address without a host`() {
        assertEquals(
            "north-guide.ru",
            PageErrorFeedbackRules.displayHost("https://www.north-guide.ru/a?b=1"),
        )
        assertEquals("example.com", PageErrorFeedbackRules.displayHost("http://example.com:8080/"))
        assertEquals("not a url", PageErrorFeedbackRules.displayHost("not a url"))
    }

    @Test
    fun `cleared transport error does not imply reconnect while network remains offline`() {
        val current = PageErrorFeedbackState.Offline(
            isOnlineReady = true,
        )

        val observation = PageErrorFeedbackRules.observe(
            current = current,
            error = null,
            httpStatusCode = null,
            isLoading = true,
            isOnline = false,
        )

        assertEquals(current.copy(isOnlineReady = false), observation.state)
        assertFalse(observation.shouldReload)
    }

    @Test
    fun `network loss removes the ready state again`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Offline(isOnlineReady = true),
            error = "Network unavailable",
            httpStatusCode = null,
            isLoading = false,
            isOnline = false,
        )

        assertEquals(PageErrorFeedbackState.Offline(), observation.state)
        assertFalse(observation.shouldReload)
    }

    @Test
    fun `online retry requests one explicit reload`() {
        val first = PageErrorFeedbackRules.requestRetry(
            PageErrorFeedbackState.Offline(isOnlineReady = true),
        )
        val duplicate = PageErrorFeedbackRules.requestRetry(first.state)

        assertEquals(PageErrorFeedbackState.Retrying, first.state)
        assertTrue(first.shouldReload)
        assertTrue(first.emitConfirmHaptic)
        assertFalse(duplicate.shouldReload)
    }

    @Test
    fun `offline retry still reloads, so the button never does nothing`() {
        val transition = PageErrorFeedbackRules.requestRetry(PageErrorFeedbackState.Offline())

        assertEquals(PageErrorFeedbackState.Retrying, transition.state)
        assertTrue(transition.shouldReload)
        assertTrue(transition.emitConfirmHaptic)
    }

    @Test
    fun `a page that loads after the offline retry hides the offline page`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Offline(isOnlineReady = true),
            error = null,
            httpStatusCode = 200,
            isLoading = false,
            isOnline = true,
        )

        assertEquals(PageErrorFeedbackState.Hidden, observation.state)
        assertFalse(observation.shouldReload)
    }

    @Test
    fun `a certificate failure after the offline retry shows the insecure connection page`() {
        val observation = PageErrorFeedbackRules.observe(
            current = PageErrorFeedbackState.Offline(isOnlineReady = true),
            error = "Gecko navigation failed",
            httpStatusCode = null,
            isLoading = false,
            isOnline = true,
            failureKind = BrowserEngineFailureKind.InsecureConnection,
        )

        assertEquals(PageErrorFeedbackState.InsecureConnection, observation.state)
        assertFalse(observation.shouldReload)
    }
}
