package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 page menu (PM-06): which rows a page, a new tab and a private tab get, «Downloads» once
 * (PM-02), why «Favorite» is off in a private tab (PM-04), and that a row closes the menu first.
 */
@RunWith(AndroidJUnit4::class)
class V5PageMenuInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private val page = V5PageMenuState(
        title = "Northern lights in March",
        subtitle = "polar-notes.example",
        favicon = null,
        hasPage = true,
        canGoForward = false,
        canFavorite = true,
        isFavorite = false,
        canRead = true,
        canTranslate = true,
        isPinned = false,
        desktopView = false,
        siteMuted = false,
    )

    private val actions = V5PageMenuActions(
        onForward = { calls += "forward" },
        onToggleFavorite = { calls += "favorite" },
        onShare = { calls += "share" },
        onPrint = { calls += "print" },
        onRead = { calls += "read" },
        onTranslate = { calls += "translate" },
        onFindInPage = { calls += "find" },
        onTogglePinned = { calls += "pin" },
        onDesktopViewChange = { calls += "desktop $it" },
        onSiteMutedChange = { calls += "mute $it" },
        onFavorites = { calls += "favorites" },
        onHistory = { calls += "history" },
        onDownloads = { calls += "downloads" },
        onSettings = { calls += "settings" },
        onDuplicateTab = { calls += "duplicate" },
        onOpenInApp = { calls += "open in app" },
        onHideDistractions = { calls += "hide distractions" },
        onShowHidden = { calls += "show hidden" },
        onPasswords = { calls += "passwords" },
    )

    private fun show(state: V5PageMenuState) {
        composeRule.setContent {
            VolaV5Theme {
                V5PageMenu(visible = true, state = state, actions = actions, onDismiss = { calls += "dismiss" })
            }
        }
        composeRule.waitForIdle()
    }

    private fun text(id: Int) = context.getString(id)

    @Test
    fun aPageGetsItsToolsAndARowClosesTheMenuBeforeItActs() {
        show(page.copy(canHideDistractions = true, hiddenCount = 2))

        listOf(
            R.string.action_translate_page,
            R.string.action_find_in_page,
            R.string.v5_distractions_hide,
            R.string.v5_distractions_show_all,
            R.string.v5_menu_downloads,
        ).forEach { composeRule.onNodeWithText(text(it)).assertExists() }
        // New and closed tabs live in the overview; the menu stays one layer (canvas «Menu»).
        listOf(R.string.cd_new_tab, R.string.v5_menu_new_private_tab, R.string.cd_close_tab)
            .forEach { composeRule.onNodeWithText(text(it)).assertDoesNotExist() }
        // The rarer actions wait on «More» (PM-05).
        composeRule.onNodeWithTag(V5PageMenuTestTags.OpenInApp).assertDoesNotExist()
        composeRule.onNodeWithTag(V5PageMenuTestTags.DuplicateTab).assertDoesNotExist()
        composeRule.onNodeWithTag(V5PageMenuTestTags.Forward).assertIsNotEnabled()

        composeRule.onNodeWithTag(V5PageMenuTestTags.History).performScrollTo().performClick()
        composeRule.onNodeWithTag(V5PageMenuTestTags.Passwords).performScrollTo().performClick()
        assertEquals(listOf("dismiss", "history", "dismiss", "passwords"), calls)
    }

    @Test
    fun moreHoldsTheRarerActionsAndBackReturnsToTheMenu() {
        show(page)

        composeRule.onNodeWithTag(V5PageMenuTestTags.More).performScrollTo().performClick()
        listOf(R.string.action_open_in_app, R.string.action_duplicate_tab, R.string.action_pin_tab, R.string.action_desktop_view)
            .forEach { composeRule.onNodeWithText(text(it)).assertExists() }
        composeRule.onNodeWithText(text(R.string.action_desktop_view)).performClick()
        composeRule.onNodeWithTag(V5PageMenuTestTags.Back).performClick()
        composeRule.onNodeWithTag(V5PageMenuTestTags.More).assertExists()
        composeRule.onNodeWithTag(V5PageMenuTestTags.More).performScrollTo().performClick()
        composeRule.onNodeWithTag(V5PageMenuTestTags.DuplicateTab).performClick()

        assertEquals(listOf("desktop true", "dismiss", "duplicate"), calls)
    }

    @Test
    fun aNewTabHasNoPageToolsAndShareAndPrintAreOff() {
        show(page.copy(title = "New tab", subtitle = null, hasPage = false, canRead = false, canTranslate = false))

        listOf(R.string.action_find_in_page, R.string.action_translate_page, R.string.v5_menu_more)
            .forEach { composeRule.onNodeWithText(text(it)).assertDoesNotExist() }
        composeRule.onNodeWithTag(V5PageMenuTestTags.Share).assertIsNotEnabled()
        composeRule.onNodeWithTag(V5PageMenuTestTags.Print).assertIsNotEnabled()
        composeRule.onNodeWithTag(V5PageMenuTestTags.Read).assertDoesNotExist()
    }

    @Test
    fun downloadsIsOneRowWithItsProgress() {
        show(page.copy(downloads = V5DownloadRingState.Progress(count = 1, fraction = 0.5f)))

        composeRule.onAllNodesWithText(text(R.string.v5_menu_downloads)).assertCountEquals(1)
        composeRule.onNodeWithText(text(R.string.v5_menu_downloads)).performScrollTo().performClick()
        assertEquals(listOf("dismiss", "downloads"), calls)
    }

    @Test
    fun aPrivateTabSaysWhyFavoriteIsOff() {
        show(page.copy(canFavorite = false, favoriteOffInPrivate = true))

        composeRule.onNodeWithTag(V5PageMenuTestTags.Favorite).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.v5_menu_favorite_private)).assertExists()
    }

    @Test
    fun favoriteTogglesInPlaceAndExtrasRun() {
        show(
            page.copy(
                extras = listOf(
                    V5PageMenuExtra("script", "Show comments", subtitle = "Forum tweaks", enabled = true) {
                        calls += "extra"
                    },
                ),
            ),
        )

        composeRule.onNodeWithTag(V5PageMenuTestTags.Favorite).performClick()
        // The extensions' commands wait in «More».
        composeRule.onNodeWithTag(V5PageMenuTestTags.extra("script")).assertDoesNotExist()
        composeRule.onNodeWithTag(V5PageMenuTestTags.More).performScrollTo().performClick()
        composeRule.onNodeWithTag(V5PageMenuTestTags.extra("script")).performScrollTo().performClick()
        assertEquals(listOf("favorite", "dismiss", "extra"), calls)
    }
}
