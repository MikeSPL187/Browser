package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.DistractionPickingStep
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The «Hide distracting items» panel on the device (DS-05): its line says each tap's result in a
 * live region (DS-01), «Undo» stands in for «Show hidden items» after a hide (DS-03), and «Done» ends.
 */
@RunWith(AndroidJUnit4::class)
class V5DistractionsPanelInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private fun show(hasHidden: Boolean, canUndo: Boolean, step: DistractionPickingStep, privateTab: Boolean = false) {
        composeRule.setContent {
            VolaV5Theme {
                V5DistractionsPanel(
                    visible = true,
                    site = "news.example",
                    privateTab = privateTab,
                    hasHidden = hasHidden,
                    canUndo = canUndo,
                    step = step,
                    onUndo = { calls += "undo" },
                    onShowHidden = { calls += "show hidden" },
                    onDone = { calls += "done" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun aSelectedBlockIsSaidInALiveRegion() {
        show(hasHidden = false, canUndo = false, step = DistractionPickingStep.Selected)

        composeRule.onNodeWithTag(V5DistractionsPanelTestTags.Step)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        composeRule.onNodeWithText(context.getString(R.string.v5_distractions_selected)).assertExists()
        composeRule.onNodeWithTag(V5DistractionsPanelTestTags.ShowHidden).assertDoesNotExist()
    }

    @Test
    fun afterAHideUndoBringsTheBlockBack() {
        show(hasHidden = true, canUndo = true, step = DistractionPickingStep.Hidden)

        composeRule.onNodeWithText(context.getString(R.string.v5_distractions_hidden)).assertExists()
        composeRule.onNodeWithTag(V5DistractionsPanelTestTags.ShowHidden).assertDoesNotExist()
        composeRule.onNodeWithTag(V5DistractionsPanelTestTags.Undo).performClick()
        composeRule.onNodeWithTag(V5DistractionsPanelTestTags.Done).performClick()

        assertEquals(listOf("undo", "done"), calls)
    }

    @Test
    fun whileChoosingTheLineNamesTheSiteAndHiddenBlocksCanComeBack() {
        show(hasHidden = true, canUndo = false, step = DistractionPickingStep.Choosing)

        composeRule.onNodeWithText(context.getString(R.string.v5_distractions_site, "news.example")).assertExists()
        composeRule.onNodeWithTag(V5DistractionsPanelTestTags.ShowHidden).performClick()
        assertEquals(listOf("show hidden"), calls)
    }

    @Test
    fun aPrivateTabSaysTheChoiceLastsUntilThePrivateTabsClose() {
        show(hasHidden = false, canUndo = false, step = DistractionPickingStep.Choosing, privateTab = true)

        composeRule.onNodeWithText(context.getString(R.string.v5_distractions_private)).assertExists()
    }
}
