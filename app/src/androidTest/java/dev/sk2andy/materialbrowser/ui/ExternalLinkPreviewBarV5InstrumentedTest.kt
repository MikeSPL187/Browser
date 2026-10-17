package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.browser.BrowserBackdropBlurRegion
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.ExternalLinkPreviewState
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.v5.V5ProfileSheetTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A link from another app under ui.v5: the bar routes the same actions as the v4 one. */
@RunWith(AndroidJUnit4::class)
class ExternalLinkPreviewBarV5InstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theBarRoutesOpenProfileAndMore() {
        var opened = false
        var closed = false
        var selectedProfileId: String? = null
        var shared = false
        var desktop: Boolean? = null
        var blur: BrowserBackdropBlurRegion? = BrowserBackdropBlurRegion(0f, 0f, 1f, 1f, 0f, 0f)
        composeRule.setContent {
            Bar(
                profiles = listOf(BrowserProfile(id = "default", emoji = "🍬"), BrowserProfile(id = "work", emoji = "💼")),
                onDismissPreview = { closed = true },
                onOpenInCandy = { opened = true },
                onSelectProfile = { selectedProfileId = it },
                onShare = { shared = true },
                onDesktopViewChange = { desktop = it },
                onBackdropBlurRegionChanged = { blur = it },
            )
        }

        composeRule.onNodeWithTag(ExternalLinkPreviewTestTags.Open).performClick()
        composeRule.onNodeWithTag(ExternalLinkPreviewTestTags.Profile).performClick()
        composeRule.onNodeWithTag(V5ProfileSheetTestTags.profile("work")).performClick()
        composeRule.onNodeWithTag(ExternalLinkPreviewTestTags.Overflow).performClick()
        composeRule.onNodeWithTag(ExternalLinkPreviewTestTags.Share).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ExternalLinkPreviewTestTags.Overflow).performClick()
        composeRule.onNodeWithTag(ExternalLinkPreviewTestTags.Desktop).performClick()
        composeRule.onNodeWithTag(ExternalLinkPreviewTestTags.Back).performClick()

        assertTrue(opened)
        assertEquals("work", selectedProfileId)
        assertTrue(shared)
        assertEquals(true, desktop)
        assertTrue(closed)
        assertNull("the opaque v5 bar asks for no blur behind it", blur)
    }

    @Test
    fun oneProfileHasNoProfileButton() {
        composeRule.setContent { Bar(profiles = listOf(BrowserProfile(id = "default", emoji = "🍬"))) }

        composeRule.onNodeWithTag(ExternalLinkPreviewTestTags.Open).assertExists()
        composeRule.onNodeWithTag(ExternalLinkPreviewTestTags.Profile).assertDoesNotExist()
    }

    @Composable
    private fun Bar(
        profiles: List<BrowserProfile>,
        onDismissPreview: () -> Unit = {},
        onOpenInCandy: () -> Unit = {},
        onSelectProfile: (String) -> Unit = {},
        onShare: () -> Unit = {},
        onDesktopViewChange: (Boolean) -> Unit = {},
        onBackdropBlurRegionChanged: (BrowserBackdropBlurRegion?) -> Unit = {},
    ) {
        MaterialBrowserTheme {
            ExternalLinkPreviewBarV5(
                state = ExternalLinkPreviewState(
                    sessionId = 1,
                    generation = 0,
                    currentUrl = "https://example.com/path",
                    targetProfileId = "default",
                ),
                profiles = profiles,
                isDesktopView = false,
                onDismissPreview = onDismissPreview,
                onOpenInCandy = onOpenInCandy,
                onSelectProfile = onSelectProfile,
                onShare = onShare,
                onCopyLink = {},
                onFindInPage = {},
                onDesktopViewChange = onDesktopViewChange,
                onBackdropBlurRegionChanged = onBackdropBlurRegionChanged,
            )
        }
    }
}
