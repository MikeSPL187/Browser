package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The v5 translate sheet on the device (TR-07): choosing a language on the sheet's second page,
 * «Translate», the lists loading and failing with a way to ask again (TR-01), the same language on
 * both sides (TR-04), and a download of unknown size said before it starts (TR-03).
 */
@RunWith(AndroidJUnit4::class)
class V5TranslateSheetInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private val english = V5TranslateLanguage("en", "English")
    private val russian = V5TranslateLanguage("ru", "Russian")
    private val german = V5TranslateLanguage("de", "German")
    private val ready = V5TranslateSheetState(
        host = "news.example",
        from = english,
        to = russian,
        fromOptions = listOf(english, german),
        toOptions = listOf(russian, german),
        alwaysTranslate = false,
        downloadBytes = 0,
    )

    private val actions = V5TranslateSheetActions(
        onFromChange = { calls += "from $it" },
        onToChange = { calls += "to $it" },
        onAlwaysTranslateChange = { calls += "always $it" },
        onTranslate = { calls += "translate" },
        onRetryLanguages = { calls += "retry" },
    )

    private fun show(state: V5TranslateSheetState) {
        composeRule.setContent {
            VolaV5Theme { V5TranslateSheet(visible = true, state = state, actions = actions, onDismiss = { calls += "dismiss" }) }
        }
        composeRule.waitForIdle()
    }

    private fun text(id: Int) = context.getString(id)

    @Test
    fun aLanguageIsChosenOnItsPageAndThePageTranslates() {
        show(ready)

        composeRule.onNodeWithTag(V5TranslateTestTags.To).performClick()
        composeRule.onNodeWithTag(V5TranslateTestTags.language("de")).performClick()
        composeRule.onNodeWithTag(V5TranslateTestTags.Always).performClick()
        composeRule.onNodeWithTag(V5TranslateTestTags.Translate).performClick()

        assertEquals(listOf("to de", "always true", "translate"), calls)
    }

    @Test
    fun languagesThatFailedToLoadCanBeAskedForAgain() {
        show(ready.copy(from = null, to = null, fromOptions = emptyList(), toOptions = emptyList(), languagesStatus = V5TranslateLanguagesStatus.Failed))

        composeRule.onNodeWithText(text(R.string.v5_translate_languages_failed)).assertExists()
        composeRule.onNodeWithTag(V5TranslateTestTags.From).assertIsNotEnabled()
        composeRule.onNodeWithTag(V5TranslateTestTags.Translate).assertIsNotEnabled()
        composeRule.onNodeWithTag(V5TranslateTestTags.RetryLanguages).performClick()

        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun loadingLanguagesSaysSo() {
        show(ready.copy(fromOptions = emptyList(), toOptions = emptyList(), languagesStatus = V5TranslateLanguagesStatus.Loading))

        composeRule.onNodeWithText(text(R.string.v5_translate_languages_loading)).assertExists()
    }

    @Test
    fun theSameLanguageOnBothSidesIsExplainedAndNotTranslated() {
        show(ready.copy(to = english))

        composeRule.onNodeWithText(text(R.string.v5_translate_same_language)).assertExists()
        composeRule.onNodeWithTag(V5TranslateTestTags.Translate).assertIsNotEnabled()
    }

    @Test
    fun aDownloadOfUnknownSizeIsSaidBeforeItStarts() {
        show(ready.copy(downloadBytes = null, downloadSizeUnknown = true))

        composeRule.onNodeWithText(text(R.string.v5_translate_note_download_unknown)).assertExists()
    }
}
