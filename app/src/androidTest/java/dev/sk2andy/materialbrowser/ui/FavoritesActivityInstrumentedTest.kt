package dev.sk2andy.materialbrowser.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.FavoritesActivity
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.BrowserSessionStore
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.ui.v5.V5LibraryTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesActivityInstrumentedTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun nestedDeleteAndUndoPersistFullLibraryIncludingSiblingOrder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = BrowserSessionStore(context)
        val previous = store.loadFavoriteLibrary()
        val parent = FavoriteFolder("activity-parent", "Parent")
        val child = FavoriteFolder("activity-child", "Child", parentFolderId = parent.id)
        val first = FavoriteEntry("https://first.example/", "First", 1, parentFolderId = child.id)
        val second = FavoriteEntry("https://second.example/", "Second", 2, parentFolderId = child.id)
        val source = FavoriteLibrary(listOf(parent, child, first, second))
        assertTrue(store.saveFavoriteLibraryCommitted(source))
        try {
            ActivityScenario.launch(FavoritesActivity::class.java).use {
                composeRule.onNodeWithTag(V5LibraryTestTags.row(parent.id)).performClick()
                composeRule.onNodeWithTag(V5LibraryTestTags.row(child.id)).performClick()
                // «Delete» as TalkBack offers it: the same action as the swipe, without its timing.
                composeRule.onNodeWithTag(V5LibraryTestTags.row(first.id))
                    .performSemanticsAction(SemanticsActions.CustomActions) { actions -> actions.first().action() }
                composeRule.waitUntil(5_000) { store.loadFavoriteLibrary().favorites.size == 1 }
                assertEquals(listOf(parent, child, second), store.loadFavoriteLibrary().entries)
                composeRule.onNodeWithText(context.getString(R.string.action_undo)).performClick()
                composeRule.waitUntil(5_000) { store.loadFavoriteLibrary() == source }
                assertEquals(source, store.loadFavoriteLibrary())
            }
        } finally {
            store.saveFavoriteLibraryCommitted(previous)
        }
    }
}
