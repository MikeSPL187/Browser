package dev.sk2andy.materialbrowser.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.EssentialCandidate
import dev.sk2andy.materialbrowser.data.EssentialEntry
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewTabPageInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun tapOpensTheEssential() {
        var opened: String? = null
        setNewTab(essentials = listOf(entry(1), entry(2)), onOpen = { opened = it })

        composeRule.onNodeWithText("Site 2").performClick()

        assertEquals("https://site2.example.com/", opened)
    }

    @Test
    fun editShowsRemoveButtonsAndAddTile() {
        val editor = FakeEditor()
        setNewTab(essentials = listOf(entry(1)), editor = editor)

        composeRule.onNodeWithTag(NewTabEssentialsTestTags.Edit).performClick()
        composeRule.onNodeWithTag(NewTabEssentialsTestTags.Add).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.essentials_remove, "Site 1"))
            .performClick()

        assertEquals(listOf("https://site1.example.com/"), editor.removed)
        composeRule.onNodeWithTag(NewTabEssentialsTestTags.Done).performClick()
        composeRule.onNodeWithTag(NewTabEssentialsTestTags.Add).assertDoesNotExist()
    }

    @Test
    fun emptyGridShowsTheStateMessage() {
        setNewTab(essentials = emptyList(), editor = FakeEditor())

        composeRule.onNodeWithText(context.getString(R.string.essentials_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.essentials_empty_action)).assertIsDisplayed()
    }

    @Test
    fun previewAndPrivateTabShowNoEditing() {
        setNewTab(essentials = listOf(entry(1)), editor = null, interactive = false)
        composeRule.onNodeWithTag(NewTabEssentialsTestTags.Edit).assertDoesNotExist()
        assertEquals(1, composeRule.onAllNodesWithTag(NewTabEssentialsTestTags.Tile).fetchSemanticsNodes().size)
    }

    @Test
    fun privateTabHidesEssentials() {
        setNewTab(essentials = listOf(entry(1)), editor = FakeEditor(), incognito = true)

        composeRule.onNodeWithTag(NewTabEssentialsTestTags.Tile).assertDoesNotExist()
    }

    private fun setNewTab(
        essentials: List<EssentialEntry>,
        editor: NewTabEssentialsEditor? = null,
        interactive: Boolean = true,
        incognito: Boolean = false,
        onOpen: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialBrowserTheme {
                NewTabPage(
                    essentials = essentials,
                    incognito = incognito,
                    modeProgress = if (incognito) 1f else 0f,
                    revealOriginInRoot = Offset.Zero,
                    onOpenEssential = onOpen,
                    editor = editor,
                    interactive = interactive,
                )
            }
        }
    }

    private fun entry(index: Int) = EssentialEntry("https://site$index.example.com/", "Site $index")

    private class FakeEditor : NewTabEssentialsEditor {
        val removed = mutableListOf<String>()
        override val candidates: List<EssentialCandidate> = emptyList()
        override val candidateIcons: Map<String, Bitmap> = emptyMap()
        override val isFull: Boolean = false

        override fun remove(entry: EssentialEntry) {
            removed += entry.url
        }

        override fun move(entry: EssentialEntry, toIndex: Int) = Unit

        override fun add(candidate: EssentialCandidate) = Unit
    }
}
