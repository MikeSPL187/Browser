package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.reader.ReaderBlock
import dev.sk2andy.materialbrowser.reader.ReaderBlockKind
import dev.sk2andy.materialbrowser.reader.ReaderDocument
import dev.sk2andy.materialbrowser.reader.ReaderExtractionResult
import dev.sk2andy.materialbrowser.reader.ReaderLibraryStore
import dev.sk2andy.materialbrowser.reader.ReaderLibraryRepository
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderStudioScreenInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val store = ReaderLibraryStore(
        InstrumentationRegistry.getInstrumentation().targetContext,
    )
    private val repository = ReaderLibraryRepository.get(
        InstrumentationRegistry.getInstrumentation().targetContext,
    )

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun articleShowsSemanticsControlsAndPrivateBoundary() {
        composeRule.setContent {
            MaterialBrowserTheme {
                ReaderStudioScreen(
                    result = ReaderExtractionResult.Success(document()),
                    sourceUrl = "https://example.com/article",
                    isPrivate = true,
                    repository = repository,
                    onRetry = {},
                    onDismiss = {},
                    onOpenOriginal = {},
                    onOpenLink = {},
                )
            }
        }

        composeRule.onNodeWithTag(ReaderStudioTestTags.Screen).assertExists()
        composeRule.onNodeWithTag(ReaderStudioTestTags.Article).assertExists()
        composeRule.onAllNodesWithText("Reader test article").assertCountEquals(1)
        composeRule.onNodeWithTag(ReaderStudioTestTags.SettingsPanel).assertDoesNotExist()
        composeRule.onNodeWithTag(ReaderStudioTestTags.SettingsButton).performClick()
        composeRule.onNodeWithTag(ReaderStudioTestTags.PrivateNotice)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(ReaderStudioTestTags.Original).assertExists()
        composeRule.onNodeWithTag(ReaderStudioTestTags.Save).assertDoesNotExist()
        composeRule.onNodeWithTag(ReaderStudioTestTags.Library).assertDoesNotExist()
    }

    @Test
    fun controlsRemainDisplayedAtLargeFontScale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                MaterialBrowserTheme {
                    ReaderStudioScreen(
                        result = ReaderExtractionResult.Success(document()),
                        sourceUrl = "https://example.com/article",
                        isPrivate = false,
                        repository = repository,
                        onRetry = {},
                        onDismiss = {},
                        onOpenOriginal = {},
                        onOpenLink = {},
                    )
                }
            }
        }

        val minutes = context.resources.getQuantityString(R.plurals.reader_reading_minutes, 1, 1)
        composeRule.onNodeWithText("Example · $minutes").assertIsDisplayed()
        composeRule.onNodeWithTag(ReaderStudioTestTags.SettingsButton).performClick()
        composeRule.onNodeWithTag(ReaderStudioTestTags.SettingsPanel).assertIsDisplayed()
        listOf(
            ReaderStudioTestTags.SpeechTransport,
            ReaderStudioTestTags.SpeechPlay,
            ReaderStudioTestTags.ThemeSegmented,
            ReaderStudioTestTags.FontSegmented,
            ReaderStudioTestTags.FontFamily,
            ReaderStudioTestTags.WideMargins,
            ReaderStudioTestTags.Save,
            ReaderStudioTestTags.Library,
            ReaderStudioTestTags.Original,
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithTag(ReaderStudioTestTags.SpeechPause).assertDoesNotExist()
        composeRule.onNodeWithTag(ReaderStudioTestTags.SpeechStop).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.reader_theme_paper))
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        composeRule.onNodeWithText(context.getString(R.string.reader_theme_dark))
            .assertIsNotSelected()
        composeRule.onNodeWithTag(ReaderStudioTestTags.WideMargins)
            .performScrollTo()
            .assertIsOff()
            .performClick()
            .assertIsOn()
        composeRule.onNodeWithText(context.getString(R.string.reader_font_sans))
            .performScrollTo()
            .performClick()
            .assertIsSelected()
    }

    private fun document() = ReaderDocument(
        title = "Reader test article",
        sourceUrl = "https://example.com/article",
        siteName = "Example",
        blocks = listOf(
            ReaderBlock(
                ReaderBlockKind.Heading,
                "A useful heading",
                level = 2,
            ),
            ReaderBlock(
                ReaderBlockKind.Paragraph,
                "A readable paragraph that remains local to the device and is rendered only as Compose text.",
            ),
        ),
    )
}
