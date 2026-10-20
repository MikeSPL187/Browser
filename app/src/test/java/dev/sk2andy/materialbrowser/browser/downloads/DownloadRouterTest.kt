package dev.sk2andy.materialbrowser.browser.downloads

import dev.sk2andy.materialbrowser.browser.actions.DownloadActionResult
import dev.sk2andy.materialbrowser.browser.actions.ExternalDownloadLaunchResult
import dev.sk2andy.materialbrowser.browser.actions.ExternalDownloadManagerApp
import dev.sk2andy.materialbrowser.browser.actions.ExternalDownloadProtocol
import dev.sk2andy.materialbrowser.data.BrowserDownloadRequest
import dev.sk2andy.materialbrowser.data.BrowserDownloadSettings
import dev.sk2andy.materialbrowser.data.DownloadManagerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadRouterTest {
    private val document = BrowserDownloadRequest("https://files.example/report.pdf", "report.pdf", "application/pdf")
    private val app = ExternalDownloadManagerApp(
        id = "adm",
        packageName = "com.example.adm",
        activityName = "Main",
        label = "ADM",
        protocol = ExternalDownloadProtocol.View,
        isOneDm = false,
    )
    private var settings = BrowserDownloadSettings()
    private var installed = listOf(app)
    private var externalAvailable = true
    private val privateTabs = mutableSetOf<String>()
    private val builtIn = mutableListOf<BrowserDownloadRequest>()
    private val launched = mutableListOf<Pair<String, Boolean>>()
    private val reports = mutableListOf<DownloadActionResult>()
    private var released = 0
    private var notificationAsks = 0

    private val router = DownloadRouter(
        host = object : DownloadRouter.Host {
            override val settings get() = this@DownloadRouterTest.settings
            override fun isIncognito(tabId: String) = tabId in privateTabs
            override fun requestNotificationPermission() {
                notificationAsks++
            }
            override fun report(result: DownloadActionResult) {
                reports += result
            }
        },
        enqueueBuiltIn = { request -> builtIn += request; DownloadActionResult.Enqueued(1, request.fileName) },
        launchExternal = { _, target, _, allowSessionData ->
            launched += target.id to allowSessionData
            if (externalAvailable) ExternalDownloadLaunchResult.Launched(target.label)
            else ExternalDownloadLaunchResult.Unavailable
        },
        discover = { installed },
    )

    @Test
    fun theBuiltInModeDownloadsAndAsksForTheProgressNotification() {
        val result = router.route(document, "tab")

        assertTrue(result is DownloadActionResult.Enqueued)
        assertEquals(listOf(document), builtIn)
        assertEquals(1, notificationAsks)
    }

    @Test
    fun theExternalModeHandsOffAndReleasesTheEnginesResponse() {
        settings = BrowserDownloadSettings(managerMode = DownloadManagerMode.External, externalManagerId = "adm")

        val result = router.route(document, "tab", releaseResponse = { released++ })

        assertEquals(DownloadActionResult.HandedOff("report.pdf", "ADM"), result)
        assertEquals(listOf("adm" to true), launched)
        assertEquals(1, released)
        assertTrue(builtIn.isEmpty())
    }

    @Test
    fun aManagerThatCannotOpenFallsBackToTheBuiltInDownload() {
        settings = BrowserDownloadSettings(managerMode = DownloadManagerMode.External, externalManagerId = "adm")
        externalAvailable = false

        router.route(document, "tab")

        assertEquals(listOf(document), builtIn)
    }

    @Test
    fun aPrivateTabAsksInsteadOfHandingOffOnItsOwn() {
        settings = BrowserDownloadSettings(managerMode = DownloadManagerMode.External, externalManagerId = "adm")
        privateTabs += "private"

        assertNull(router.route(document, "private"))
        assertNotNull(router.choices.pending)
        assertTrue(router.choices.pending!!.isIncognito)

        router.confirmChoice("adm")
        assertEquals(listOf("adm" to false), launched)
        assertTrue(reports.single() is DownloadActionResult.HandedOff)
        assertNull(router.choices.pending)
    }

    @Test
    fun askEveryTimeWithoutManagersDownloadsAtOnce() {
        settings = BrowserDownloadSettings(managerMode = DownloadManagerMode.AskEveryTime)
        installed = emptyList()

        router.route(document, "tab")

        assertEquals(listOf(document), builtIn)
        assertNull(router.choices.pending)
    }

    @Test
    fun aChoiceForAPageThatMovedOnReleasesAndDownloadsNothing() {
        settings = BrowserDownloadSettings(managerMode = DownloadManagerMode.AskEveryTime)
        var current = true
        router.route(document, "tab", releaseResponse = { released++ }, isSourceCurrent = { current })
        current = false

        router.confirmChoice(null)

        assertEquals(1, released)
        assertTrue(builtIn.isEmpty())
    }

    @Test
    fun dismissingTheChoiceReleasesTheResponse() {
        settings = BrowserDownloadSettings(managerMode = DownloadManagerMode.AskEveryTime)
        router.route(document, "tab", releaseResponse = { released++ })

        router.dismissChoice()

        assertEquals(1, released)
        assertNull(router.choices.pending)
    }

    @Test
    fun anAndroidPackageWaitsForTheFileCheckAndSavingRoutesIt() {
        val apk = BrowserDownloadRequest("https://files.example/app.apk", "app.apk", "application/vnd.android.package-archive")

        assertNull(router.route(apk, "tab"))
        assertTrue(builtIn.isEmpty())

        router.safety.save()
        assertEquals(listOf(apk), builtIn)
        assertTrue(reports.single() is DownloadActionResult.Enqueued)
    }

    @Test
    fun closingReleasesEveryHeldChoice() {
        settings = BrowserDownloadSettings(managerMode = DownloadManagerMode.AskEveryTime)
        router.route(document, "tab", releaseResponse = { released++ })
        router.route(document, "tab", releaseResponse = { released++ })

        router.close()

        assertEquals(2, released)
    }

    @Test
    fun theManagersListFollowsWhatIsInstalled() {
        router.refreshManagers()
        assertEquals(listOf(app), router.managers.toList())

        installed = emptyList()
        router.refreshManagers()
        assertTrue(router.managers.isEmpty())
    }
}
