package dev.sk2andy.materialbrowser.ui

import dev.sk2andy.materialbrowser.browser.actions.ExternalDownloadManagerApp
import dev.sk2andy.materialbrowser.browser.actions.ExternalDownloadProtocol
import dev.sk2andy.materialbrowser.data.BrowserDownloadSettings
import dev.sk2andy.materialbrowser.data.DownloadManagerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadSettingsRulesTest {
    private fun manager(id: String, isOneDm: Boolean = false) = ExternalDownloadManagerApp(
        id = id,
        packageName = "com.example.$id",
        activityName = "com.example.$id.Download",
        label = id,
        protocol = ExternalDownloadProtocol.entries.first(),
        isOneDm = isOneDm,
    )

    @Test
    fun `built-in and ask come first, then every installed manager`() {
        assertEquals(
            listOf(
                DownloadManagerChoice(DownloadManagerMode.BuiltIn),
                DownloadManagerChoice(DownloadManagerMode.AskEveryTime),
                DownloadManagerChoice(DownloadManagerMode.External, "adm"),
                DownloadManagerChoice(DownloadManagerMode.External, "1dm"),
            ),
            DownloadSettingsRules.choices(listOf(manager("adm"), manager("1dm"))),
        )
    }

    @Test
    fun `a stale manager id does not count outside the external mode`() {
        val selected = DownloadSettingsRules.selected(
            BrowserDownloadSettings(
                managerMode = DownloadManagerMode.AskEveryTime,
                externalManagerId = "adm",
            ),
        )

        assertEquals(DownloadManagerChoice(DownloadManagerMode.AskEveryTime), selected)
    }

    @Test
    fun `picking the built-in downloader drops the manager id`() {
        val applied = DownloadSettingsRules.apply(
            BrowserDownloadSettings(
                managerMode = DownloadManagerMode.External,
                externalManagerId = "adm",
            ),
            DownloadManagerChoice(DownloadManagerMode.BuiltIn),
        )

        assertEquals(DownloadManagerMode.BuiltIn, applied.managerMode)
        assertNull(applied.externalManagerId)
    }

    @Test
    fun `1DM session sharing matters only where 1DM may download`() {
        val managers = listOf(manager("adm"), manager("1dm", isOneDm = true))

        assertFalse(DownloadSettingsRules.oneDmRelevant(BrowserDownloadSettings(), managers))
        assertTrue(
            DownloadSettingsRules.oneDmRelevant(
                BrowserDownloadSettings(managerMode = DownloadManagerMode.AskEveryTime),
                managers,
            ),
        )
        assertFalse(
            DownloadSettingsRules.oneDmRelevant(
                BrowserDownloadSettings(
                    managerMode = DownloadManagerMode.External,
                    externalManagerId = "adm",
                ),
                managers,
            ),
        )
    }
}
