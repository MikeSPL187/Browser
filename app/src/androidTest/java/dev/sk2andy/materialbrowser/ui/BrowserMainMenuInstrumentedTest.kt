package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtensionActionKey
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtensionActionKind
import dev.sk2andy.materialbrowser.browser.gecko.GeckoExtensionActionState
import dev.sk2andy.materialbrowser.browser.userscript.UserScriptMenuCommand
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserMainMenuInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shortMenuOpensMoreInPlace() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dismissals = AtomicInteger()
        val duplicateActions = AtomicInteger()
        val desktopViewChanges = AtomicInteger()
        val extensionActionKey = GeckoExtensionActionKey(
            extensionId = "site-addon@example.test",
            tabId = "tab",
            kind = GeckoExtensionActionKind.Browser,
        )
        val firefoxExtensionAction = AtomicReference<GeckoExtensionActionKey>()
        var setMenuExpanded: (Boolean) -> Unit = {}
        composeRule.setContent {
            MaterialBrowserTheme {
                var expanded by remember { mutableStateOf(true) }
                var desktopView by remember { mutableStateOf(false) }
                setMenuExpanded = { expanded = it }
                BrowserMainMenu(
                    expanded = expanded,
                    backdropSource = null,
                    onDismissRequest = {
                        if (expanded) dismissals.incrementAndGet()
                        expanded = false
                    },
                    pageSubtitle = "developer.android.com",
                    canGoBack = false,
                    canGoForward = false,
                    isLoading = false,
                    canToggleFavorite = true,
                    isFavorite = false,
                    isPinned = false,
                    canUsePageActions = true,
                    canOpenReader = true,
                    canTranslatePage = true,
                    canToggleDomainMute = true,
                    isDomainMuted = false,
                    canToggleDesktopView = true,
                    isDesktopView = desktopView,
                    canAddSiteCapsule = true,
                    canSnooze = true,
                    snoozedTabCount = 2,
                    onBack = {},
                    onForward = {},
                    onReloadOrStop = {},
                    onToggleFavorite = {},
                    onTogglePinned = {},
                    onShare = {},
                    onOpenExternal = {},
                    onPrint = {},
                    onOpenReader = {},
                    onTranslate = {},
                    onDomainMutedChange = {},
                    onDesktopViewChange = { enabled ->
                        desktopViewChanges.incrementAndGet()
                        desktopView = enabled
                    },
                    onOpenCandyTrail = {},
                    onAddSiteCapsule = {},
                    onSummarize = {},
                    onSnooze = {},
                    onSnoozedTabs = {},
                    onDuplicateTab = duplicateActions::incrementAndGet,
                    onDockAddressBar = {},
                    onHistory = {},
                    firefoxExtensionActions = listOf(
                        GeckoExtensionActionState(
                            key = extensionActionKey,
                            title = "Site add-on",
                            enabled = true,
                            badgeText = null,
                            badgeBackgroundColor = null,
                            badgeTextColor = null,
                        ),
                    ),
                    onFirefoxExtensionAction = firefoxExtensionAction::set,
                    onSettings = {},
                )
            }
        }

        // The first view: toolbar, page tiles ending in «More», the library.
        composeRule.onNode(
            hasTestTag(BrowserMainMenuTestTags.PageGroup) and
                hasAnyDescendant(hasText(context.getString(R.string.reader_open_action))) and
                hasAnyDescendant(hasText(context.getString(R.string.action_translate_page))) and
                hasAnyDescendant(hasText(context.getString(R.string.action_find_in_page))) and
                hasAnyDescendant(hasText(context.getString(R.string.action_print))) and
                hasAnyDescendant(hasTestTag(BrowserMainMenuTestTags.DesktopView)) and
                hasAnyDescendant(hasTestTag(BrowserMainMenuTestTags.More)),
        ).assertExists()
        composeRule.onNode(
            hasTestTag(BrowserMainMenuTestTags.BrowserGroup) and
                hasAnyDescendant(hasText(context.getString(R.string.downloads_title))) and
                hasAnyDescendant(hasText(context.getString(R.string.action_history))) and
                hasAnyDescendant(hasText(context.getString(R.string.favorites_title))) and
                hasAnyDescendant(hasText(context.getString(R.string.passwords_title))) and
                hasAnyDescendant(hasText(context.getString(R.string.action_settings))),
        ).assertExists()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.BrowserGroup)
            .onChildren()
            .assertCountEquals(5)
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.DuplicateTab).assertDoesNotExist()
        composeRule.onNodeWithTag(FirefoxExtensionChromeTestTags.SectionTitle).assertDoesNotExist()

        // A toggle tile switches in place.
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.DesktopView)
            .performScrollTo()
            .assertIsOff()
            .performClick()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.DesktopView).assertIsOn()
        assertEquals(1, desktopViewChanges.get())

        // «More» turns the page without closing the menu; its back row returns.
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.More).performScrollTo().performClick()
        assertEquals(0, dismissals.get())
        composeRule.onNode(
            hasTestTag(BrowserMainMenuTestTags.MoreGroup) and
                hasAnyDescendant(hasTestTag(BrowserMainMenuTestTags.DuplicateTab)) and
                hasAnyDescendant(hasTestTag(BrowserMainMenuTestTags.Pin)) and
                hasAnyDescendant(hasTestTag(BrowserMainMenuTestTags.CloseTab)) and
                hasAnyDescendant(hasTestTag(BrowserMainMenuTestTags.Snooze)) and
                hasAnyDescendant(hasTestTag(BrowserMainMenuTestTags.SnoozedTabs)) and
                hasAnyDescendant(hasTestTag(DomainMuteMenuTestTags.Item)) and
                hasAnyDescendant(hasText(context.getString(R.string.action_open_candy_trail))),
        ).assertExists()
        composeRule.onNodeWithTag(FirefoxExtensionChromeTestTags.SectionTitle).assertExists()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.MoreBack).performClick()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.MoreGroup).assertDoesNotExist()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.PageGroup).assertExists()

        composeRule.onNodeWithTag(BrowserMainMenuTestTags.More).performScrollTo().performClick()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.DuplicateTab).performScrollTo().performClick()
        assertEquals(1, dismissals.get())
        assertEquals(1, duplicateActions.get())

        // Opening again starts from the first view.
        composeRule.runOnIdle { setMenuExpanded(true) }
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.MoreGroup).assertDoesNotExist()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.More).performScrollTo().performClick()
        composeRule.onNodeWithTag(
            FirefoxExtensionChromeTestTags.action(extensionActionKey.saveableId),
        ).performScrollTo().performClick()
        assertEquals(2, dismissals.get())
        assertEquals(extensionActionKey, firefoxExtensionAction.get())
    }

    @Test
    fun disablesReaderForUnsupportedPage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val invocations = AtomicInteger()
        val favoriteInvocations = AtomicInteger()
        var setMenuExpanded: (Boolean) -> Unit = {}
        val command = UserScriptMenuCommand(
            tabId = "tab",
            scriptId = "script",
            scriptName = "Password helper",
            commandId = "reveal",
            caption = "Reveal passwords",
        )
        composeRule.setContent {
            MaterialBrowserTheme {
                var expanded by remember { mutableStateOf(true) }
                setMenuExpanded = { expanded = it }
                BrowserMainMenu(
                    expanded = expanded,
                    backdropSource = null,
                    onDismissRequest = { expanded = false },
                    pageSubtitle = "New tab",
                    canGoBack = false,
                    canGoForward = false,
                    isLoading = false,
                    canToggleFavorite = false,
                    isFavorite = false,
                    isPinned = false,
                    canUsePageActions = false,
                    canOpenReader = false,
                    canTranslatePage = false,
                    canToggleDomainMute = false,
                    isDomainMuted = false,
                    canToggleDesktopView = false,
                    isDesktopView = false,
                    canAddSiteCapsule = false,
                    canSnooze = false,
                    snoozedTabCount = 0,
                    userScriptMenuCommands = listOf(command),
                    onBack = {},
                    onForward = {},
                    onReloadOrStop = {},
                    onToggleFavorite = {},
                    onTogglePinned = {},
                    onShare = {},
                    onOpenExternal = {},
                    onPrint = {},
                    onOpenReader = {},
                    onTranslate = {},
                    onDomainMutedChange = {},
                    onDesktopViewChange = {},
                    onOpenCandyTrail = {},
                    onAddSiteCapsule = {},
                    onSummarize = {},
                    onSnooze = {},
                    onSnoozedTabs = {},
                    onUserScriptMenuCommand = { selected ->
                        if (selected == command) invocations.incrementAndGet()
                    },
                    onDockAddressBar = {},
                    onFavorites = favoriteInvocations::incrementAndGet,
                    onHistory = {},
                    onSettings = {},
                )
            }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.reader_open_action),
        ).assertIsNotEnabled()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.Translate).assertIsNotEnabled()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.DesktopView).assertIsNotEnabled()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.ToppingsGroup).assertDoesNotExist()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.More).performScrollTo().performClick()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.DuplicateTab).assertIsNotEnabled()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.ToppingsGroup).assertExists()
        composeRule.onNodeWithText("Password helper").assertExists()
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.userScriptCommand("reveal"))
            .performScrollTo()
            .performClick()
        assertEquals(1, invocations.get())
        composeRule.runOnIdle { setMenuExpanded(true) }
        composeRule.onNodeWithTag(BrowserMainMenuTestTags.Favorites)
            .performScrollTo()
            .performClick()
        assertEquals(1, favoriteInvocations.get())
    }
}
