package dev.sk2andy.materialbrowser.browser.userscript

import dev.sk2andy.materialbrowser.data.ToppingCatalogRefreshResult
import dev.sk2andy.materialbrowser.data.ToppingDownloadResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToppingCatalogInstallerTest {
    private val entry = ToppingCatalogEntry(
        id = "calm-reader",
        name = "Calm Reader",
        description = "Calmer articles.",
        author = "Candy Browser",
        license = "MIT",
        version = "1.0.0",
        source = "toppings/calm-reader.user.js",
        matches = listOf("https://*.example.com/*"),
        sha256 = "0".repeat(64),
    )
    private val catalog = ToppingCatalog(schemaVersion = 1, toppings = listOf(entry))
    private val scriptId = ToppingCatalogRules.stableScriptId(entry.id)

    private var userScripts = listOf<UserScript>()
    private val pendingRefreshes = mutableListOf<(ToppingCatalogRefreshResult) -> Unit>()
    private val pendingDownloads = mutableListOf<(ToppingDownloadResult) -> Unit>()
    private var resolves = true
    private val enabledChanges = mutableListOf<Pair<String, Boolean>>()

    private val installer = ToppingCatalogInstaller(
        fetchCatalog = { pendingRefreshes += it },
        downloadTopping = { _, onComplete -> pendingDownloads += onComplete },
        host = object : ToppingCatalogInstaller.Host {
            override val isDestroyed = false
            override val userScripts get() = this@ToppingCatalogInstallerTest.userScripts
            override fun setUserScriptEnabled(id: String, enabled: Boolean, onComplete: (Boolean) -> Unit) {
                enabledChanges += id to enabled
                onComplete(true)
            }
            override fun resolveDependencies(
                script: UserScript,
                onComplete: (UserScriptDependencyResolution) -> Unit,
            ) = onComplete(
                if (resolves) {
                    UserScriptDependencyResolution.Resolved(script)
                } else {
                    UserScriptDependencyResolution.Failed(UserScriptDependencyFailureReason.entries.first())
                },
            )
            override fun commitUserScripts(scripts: List<UserScript>, onComplete: (Boolean) -> Unit) {
                this@ToppingCatalogInstallerTest.userScripts = scripts
                onComplete(true)
            }
        },
    )

    private fun downloaded(enabled: Boolean = true): UserScript = (
        UserScriptParser.parse(
            id = scriptId,
            source = """
                // ==UserScript==
                // @name Calm Reader
                // @match https://*.example.com/*
                // @grant none
                // ==/UserScript==
                document.documentElement.dataset.candy = "calm";
            """.trimIndent(),
            enabled = enabled,
        ) as UserScriptParseResult.Accepted
        ).script

    private fun loadCatalog() {
        installer.refresh()
        pendingRefreshes.removeAt(0)(ToppingCatalogRefreshResult.Fresh(catalog))
    }

    @Test
    fun onlyTheLatestRefreshLands() {
        installer.refresh()
        installer.refresh()
        assertTrue(installer.isLoading)

        pendingRefreshes[0](ToppingCatalogRefreshResult.Fresh(catalog))
        assertTrue(installer.isLoading)
        assertNull(installer.result)

        pendingRefreshes[1](ToppingCatalogRefreshResult.Fresh(catalog))
        assertFalse(installer.isLoading)
        assertEquals(ToppingCatalogRefreshResult.Fresh(catalog), installer.result)
    }

    @Test
    fun turningOnANewToppingDownloadsAndSavesIt() {
        loadCatalog()
        var answer: Boolean? = null

        installer.setEnabled(entry.id, enabled = true) { answer = it }
        assertEquals(listOf(entry.id), installer.busyIds.toList())
        pendingDownloads.single()(ToppingDownloadResult.Accepted(downloaded()))

        assertEquals(true, answer)
        assertEquals(listOf(scriptId), userScripts.map(UserScript::id))
        assertTrue(installer.busyIds.isEmpty())
    }

    @Test
    fun anInstalledToppingIsSwitchedWithoutADownload() {
        loadCatalog()
        userScripts = listOf(downloaded())

        installer.setEnabled(entry.id, enabled = false)

        assertEquals(listOf(scriptId to false), enabledChanges)
        assertTrue(pendingDownloads.isEmpty())
    }

    @Test
    fun turningOffAToppingThatIsNotInstalledIsAlreadyDone() {
        loadCatalog()
        var answer: Boolean? = null

        installer.setEnabled(entry.id, enabled = false) { answer = it }

        assertEquals(true, answer)
        assertTrue(pendingDownloads.isEmpty())
    }

    @Test
    fun withoutACatalogOrWhileBusyNothingStarts() {
        var answer: Boolean? = null
        installer.setEnabled(entry.id, enabled = true) { answer = it }
        assertEquals(false, answer)

        loadCatalog()
        installer.setEnabled(entry.id, enabled = true)
        installer.setEnabled(entry.id, enabled = true) { answer = it }
        assertEquals(false, answer)
        assertEquals(1, pendingDownloads.size)
    }

    @Test
    fun updatingKeepsTheToppingOffWhenTheOwnerTurnedItOff() {
        loadCatalog()
        userScripts = listOf(downloaded(enabled = false))

        installer.update(entry.id)
        pendingDownloads.single()(ToppingDownloadResult.Accepted(downloaded(enabled = true)))

        assertFalse(userScripts.single().enabled)
    }

    @Test
    fun updatingAToppingThatIsNotInstalledFails() {
        loadCatalog()
        var answer: Boolean? = null

        installer.update(entry.id) { answer = it }

        assertEquals(false, answer)
    }

    @Test
    fun aRejectedDownloadOrMissingDependencySavesNothing() {
        loadCatalog()
        var answer: Boolean? = null
        installer.setEnabled(entry.id, enabled = true) { answer = it }
        pendingDownloads.removeAt(0)(ToppingDownloadResult.IntegrityMismatch)
        assertEquals(false, answer)

        resolves = false
        installer.setEnabled(entry.id, enabled = true) { answer = it }
        pendingDownloads.removeAt(0)(ToppingDownloadResult.Accepted(downloaded()))

        assertEquals(false, answer)
        assertTrue(userScripts.isEmpty())
        assertTrue(installer.busyIds.isEmpty())
    }
}
