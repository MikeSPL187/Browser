package dev.sk2andy.materialbrowser.shared.settings

import dev.sk2andy.materialbrowser.ui.SettingsDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsSearchRulesTest {
    private val darkSites = SettingSearchCandidate(
        key = "force_dark",
        destination = SettingsDestination.Appearance,
        title = "Тёмные сайты",
        summary = "Затемнять страницы без тёмной темы",
        page = "Внешний вид",
    )
    private val theme = SettingSearchCandidate(
        key = "theme",
        destination = SettingsDestination.Appearance,
        title = "Тема",
        summary = "Светлая, тёмная или как в системе",
        page = "Внешний вид",
    )
    private val appearancePage = SettingSearchCandidate(
        key = "page:appearance",
        destination = SettingsDestination.Appearance,
        title = "Внешний вид",
        summary = "Рама, тема, цвета",
        page = "",
    )
    private val httpsOnly = SettingSearchCandidate(
        key = "https_only",
        destination = SettingsDestination.ProtectionAndData,
        title = "Только HTTPS",
        summary = null,
        page = "Защита и данные",
    )
    private val all = listOf(darkSites, theme, appearancePage, httpsOnly)

    @Test
    fun everyWordMustAppearAndTitlesComeFirst() {
        // «тема» is the title of one and only in the description of the page.
        assertEquals(listOf(theme, appearancePage), SettingsSearchRules.search("тема", all))
        // «Тёмные» matches «темн» without the dots.
        assertEquals(listOf(darkSites, theme), SettingsSearchRules.search("ТЕМН", all))
        assertEquals(listOf(httpsOnly), SettingsSearchRules.search("https", all))
        // Every word: «https» and «тема» are never together.
        assertTrue(SettingsSearchRules.search("https тема", all).isEmpty())
    }

    @Test
    fun pageNamesFindTheirSettingsAndThePageItself() {
        assertEquals(
            listOf(appearancePage, darkSites, theme),
            SettingsSearchRules.search("  внешний   ВИД ", all),
        )
    }

    @Test
    fun blankQueryFindsNothing() {
        assertTrue(SettingsSearchRules.search("   ", all).isEmpty())
        assertEquals("темные сайты", SettingsSearchRules.normalize(" Тёмные  сайты "))
    }

    @Test
    fun pastedUnicodeSpacesSeparateWordsLikeOrdinarySpaces() {
        for (space in listOf('\u00a0', '\u202f', '\u2009')) {
            assertEquals(
                listOf(appearancePage),
                SettingsSearchRules.search("Внешний${space}вид", listOf(appearancePage)),
            )
        }
        assertEquals("внешний вид", SettingsSearchRules.normalize("\u00a0Внешний\u202f\u2009вид "))
    }
}
