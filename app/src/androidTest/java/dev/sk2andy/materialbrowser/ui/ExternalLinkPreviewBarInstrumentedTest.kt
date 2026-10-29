package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.browser.BrowserProfile
import dev.sk2andy.materialbrowser.browser.ExternalLinkPreviewState
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalLinkPreviewBarInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactBarHidesProfileSelectionWhenOnlyOneProfileExists() {
        composeRule.setContent {
            MaterialBrowserTheme {
                ExternalLinkPreviewBar(
                    state = ExternalLinkPreviewState(
                        sessionId = 1,
                        generation = 0,
                        currentUrl = "https://example.com/path",
                        targetProfileId = "default",
                    ),
                    profiles = listOf(BrowserProfile(id = "default", emoji = "🍬")),
                    isDesktopView = false,
                    backdropSource = null,
                    rootBottomInWindowPx = 0,
                    onDismissPreview = {},
                    onOpenInCandy = {},
                    onSelectProfile = {},
                    onShare = {},
                    onCopyLink = {},
                    onFindInPage = {},
                    onDesktopViewChange = {},
                )
            }
        }

        composeRule.onNodeWithTag(ExternalLinkPreviewTestTags.Open).assertIsEnabled()
        composeRule.onNodeWithTag(ExternalLinkPreviewTestTags.Profile).assertDoesNotExist()
    }
}
