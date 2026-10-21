package dev.sk2andy.materialbrowser.ui.v5

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
 * The v5 new tab on the device (audit 20.10, Н-9): editing and unpinning, the line that explains an
 * empty or a full grid, no «Edit» without an editor, and «Pin a site» offering favorites and history.
 */
@RunWith(AndroidJUnit4::class)
class V5NewTabInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val calls = mutableListOf<String>()

    private fun site(id: String) = V5PinnedSite(id = id, title = "Site $id", icon = null)

    private fun show(pinned: List<V5PinnedSite>, canAdd: Boolean = true, canEdit: Boolean = true) {
        composeRule.setContent {
            VolaV5Theme {
                V5NewTab(
                    state = V5NewTabState(
                        profileLabel = null,
                        pinned = pinned,
                        canAdd = canAdd,
                        report = null,
                        recent = emptyList(),
                        canEdit = canEdit,
                    ),
                    onOpenPinned = { calls += "open $it" },
                    onRemovePinned = { calls += "remove $it" },
                    onAddPinned = { calls += "add" },
                    onOpenReport = {},
                    onHideReport = {},
                    onOpenRecent = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun editingThenTappingATileUnpinsIt() {
        show(listOf(site("a"), site("b")))

        composeRule.onNodeWithTag(V5NewTabTestTags.Edit).performClick()
        composeRule.onNodeWithTag(V5NewTabTestTags.pinned("b")).performClick()
        // The tile fades first, then it is unpinned.
        composeRule.waitUntil(timeoutMillis = 3_000) { calls.isNotEmpty() }

        assertEquals(listOf("remove b"), calls)
    }

    @Test
    fun anEmptyGridSaysWhatPinningIsFor() {
        show(emptyList())

        composeRule.onNodeWithText(context.getString(R.string.v5_newtab_pinned_empty)).assertExists()
        composeRule.onNodeWithTag(V5NewTabTestTags.Add).performClick()
        assertEquals(listOf("add"), calls)
    }

    @Test
    fun aFullGridSaysWhyAddIsGone() {
        show(List(16) { site("s$it") }, canAdd = false)

        composeRule.onNodeWithText(context.getString(R.string.v5_newtab_pinned_full)).assertExists()
        composeRule.onNodeWithTag(V5NewTabTestTags.Add).assertDoesNotExist()
    }

    @Test
    fun aPreviewWithoutAnEditorHasNoEdit() {
        show(listOf(site("a")), canEdit = false)

        composeRule.onNodeWithTag(V5NewTabTestTags.Edit).assertDoesNotExist()
    }

    @Test
    fun pinASiteOffersFavoritesAndHistory() {
        composeRule.setContent {
            VolaV5Theme {
                V5PinnedAddSheet(
                    visible = true,
                    candidates = emptyList(),
                    onAdd = { calls += "pin $it" },
                    onDismiss = {},
                    favorites = listOf(V5RecentTab("favorite:news", "News", "news.example", null)),
                    history = listOf(V5RecentTab("history:https://old.example", "Old", "old.example", null)),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(context.getString(R.string.v5_newtab_add_favorites)).assertExists()
        composeRule.onNodeWithTag(V5PinnedAddSheetTestTags.candidate("history:https://old.example")).performClick()

        assertEquals(listOf("pin history:https://old.example"), calls)
    }
}
