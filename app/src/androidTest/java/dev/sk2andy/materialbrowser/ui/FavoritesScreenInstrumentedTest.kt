package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.BrowsingFavoritesRules
import dev.sk2andy.materialbrowser.data.FavoriteEntry
import dev.sk2andy.materialbrowser.data.FavoriteFolder
import dev.sk2andy.materialbrowser.data.FavoriteLibrary
import dev.sk2andy.materialbrowser.data.FavoriteMutation
import dev.sk2andy.materialbrowser.data.FavoriteUndoRules
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesScreenInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchDeleteUndoAndOpenFavorite() {
        val alpha = favorite("https://alpha.example/", "Alpha")
        val beta = favorite("https://beta.example/guide", "Beta Guide")
        val opened = AtomicReference<FavoriteEntry?>()
        composeRule.setContent {
            var favorites by remember { mutableStateOf(listOf(alpha, beta)) }
            var revision by remember { mutableLongStateOf(0L) }
            MaterialBrowserTheme {
                FavoritesScreen(
                    favorites = favorites,
                    onDeleteFavorite = { target, onComplete ->
                        val before = favorites
                        val updated = BrowsingFavoritesRules.remove(before, target)
                        if (updated == before) {
                            onComplete(null)
                        } else {
                            favorites = updated
                            onComplete(
                                FavoriteMutation(
                                    before = before,
                                    applied = updated,
                                    added = false,
                                    revision = ++revision,
                                ),
                            )
                        }
                    },
                    onUndoDelete = { mutation ->
                        FavoriteUndoRules.restore(favorites, revision, mutation)?.let { restored ->
                            revision++
                            favorites = restored
                        }
                    },
                    onOpenFavorite = opened::set,
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText(alpha.title).assertIsDisplayed()
        composeRule.onNodeWithText(beta.title).assertIsDisplayed()
        composeRule.onNodeWithTag(FavoritesScreenTestTags.SearchField).performTextInput("beta")
        composeRule.onNodeWithText(alpha.title).assertDoesNotExist()
        composeRule.onNodeWithText(beta.title).assertIsDisplayed()

        composeRule.onNodeWithTag("favorites_actions:${beta.id}").performClick()
        composeRule.onNodeWithTag(FavoritesScreenTestTags.delete(beta.url)).performClick()
        composeRule.onNodeWithText(beta.title).assertDoesNotExist()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(context.getString(R.string.action_undo)).performClick()
        composeRule.onNodeWithText(beta.title).assertIsDisplayed().performClick()
        assertEquals(beta, opened.get())

        composeRule.onNodeWithTag(FavoritesScreenTestTags.SearchField).performTextClearance()
        composeRule.onNodeWithText(alpha.title).assertIsDisplayed()
    }

    @Test
    fun staleUndoLeavesTheScreen() {
        val alpha = favorite("https://alpha.example/", "Alpha")
        val beta = favorite("https://beta.example/", "Beta")
        var canUndo by mutableStateOf(true)
        composeRule.setContent {
            var favorites by remember { mutableStateOf(listOf(alpha, beta)) }
            MaterialBrowserTheme {
                FavoritesScreen(
                    favorites = favorites,
                    onDeleteFavorite = { target, onComplete ->
                        val before = favorites
                        favorites = BrowsingFavoritesRules.remove(before, target)
                        onComplete(FavoriteMutation(before, favorites, added = false, revision = 1))
                    },
                    onUndoDelete = {},
                    onOpenFavorite = {},
                    onBack = {},
                    canUndo = canUndo,
                )
            }
        }
        val undo = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.action_undo)

        composeRule.onNodeWithTag("favorites_actions:${beta.id}").performClick()
        composeRule.onNodeWithTag(FavoritesScreenTestTags.delete(beta.url)).performClick()
        composeRule.onNodeWithText(undo).assertIsDisplayed()
        // A later rename or move replaces the snapshot the undo would restore.
        canUndo = false
        composeRule.waitForIdle()
        composeRule.onNodeWithText(undo).assertDoesNotExist()
    }

    @Test
    fun renameDialogKeepsItsTargetAndDraftAcrossRecreation() {
        val alpha = favorite("https://alpha.example/", "Alpha")
        val renamed = AtomicReference<Pair<String, String>?>()
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            MaterialBrowserTheme {
                FavoritesScreen(
                    favorites = listOf(alpha),
                    onDeleteFavorite = { _, _ -> },
                    onUndoDelete = {},
                    onOpenFavorite = {},
                    onBack = {},
                    onRenameEntry = { entry, title -> renamed.set(entry.id to title) },
                )
            }
        }

        composeRule.onNodeWithTag("favorites_actions:${alpha.id}").performClick()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(context.getString(R.string.favorites_rename)).performClick()
        composeRule.onNodeWithTag("favorites_name").performTextClearance()
        composeRule.onNodeWithTag("favorites_name").performTextInput("Draft")

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag("favorites_name").assertTextContains("Draft")
        composeRule.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        assertEquals(alpha.id to "Draft", renamed.get())
    }

    @Test
    fun recursiveNavigationKeepsEmptyFoldersEmptyAndSearchFindsNestedFavorite() {
        val child = FavoriteFolder("child", "Child", parentFolderId = "parent")
        val parent = FavoriteFolder("parent", "Parent")
        val nested = favorite("https://nested.example/", "Nested").copy(parentFolderId = child.id)
        val empty = FavoriteFolder("empty", "Empty", parentFolderId = child.id)
        val source = FavoriteLibrary(listOf(parent, child, nested, empty))
        composeRule.setContent {
            MaterialBrowserTheme {
                FavoritesScreen(
                    favorites = source.favorites,
                    library = source,
                    onDeleteFavorite = { _, done -> done(null) },
                    onUndoDelete = {},
                    onOpenFavorite = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithText("Nested").assertDoesNotExist()
        composeRule.onNodeWithTag("favorites_folder:parent").performClick()
        composeRule.onNodeWithTag("favorites_folder:child").performClick()
        composeRule.onNodeWithText("Nested").assertIsDisplayed()
        composeRule.onNodeWithTag("favorites_folder:empty").performClick()
        composeRule.onNodeWithTag(FavoritesScreenTestTags.favorite(nested.url)).assertDoesNotExist()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(context.getString(R.string.favorites_folder_empty)).assertIsDisplayed()
        composeRule.onNodeWithTag("favorites_parent").performClick()
        composeRule.onNodeWithText("Nested").assertIsDisplayed()
        composeRule.onNodeWithTag("favorites_parent").performClick()
        composeRule.onNodeWithTag(FavoritesScreenTestTags.SearchField).performTextInput("nested")
        composeRule.onNodeWithText("Nested").assertIsDisplayed()
    }

    @Test
    fun managerReordersRenamesAndMovesFavoriteIntoFolderAndBack() {
        val alpha = favorite("https://alpha.example/", "Alpha")
        val beta = favorite("https://beta.example/", "Beta")
        val folder = FavoriteFolder("folder", "Folder")
        val latest = AtomicReference(FavoriteLibrary(listOf(alpha, beta, folder)))
        composeRule.setContent {
            var library by remember { mutableStateOf(latest.get()) }
            fun update(value: FavoriteLibrary) { library = value; latest.set(value) }
            MaterialBrowserTheme {
                FavoritesScreen(
                    favorites = library.favorites,
                    library = library,
                    onDeleteFavorite = { _, done -> done(null) },
                    onUndoDelete = {},
                    onOpenFavorite = {},
                    onBack = {},
                    onRenameEntry = { entry, title -> update(BrowsingFavoritesRules.rename(library, entry.id, title)) },
                    onMoveEntry = { entry, parent -> update(BrowsingFavoritesRules.move(library, entry.id, parent)) },
                    onReorderEntry = { entry, index -> update(BrowsingFavoritesRules.reorder(library, entry.id, index)) },
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithTag("favorites_actions:${beta.id}").performClick()
        composeRule.onNodeWithText(context.getString(R.string.favorites_move_up)).performClick()
        composeRule.runOnIdle { assertEquals(beta.id, latest.get().entries.first().id) }
        composeRule.onNodeWithTag("favorites_actions:${beta.id}").performClick()
        composeRule.onNodeWithText(context.getString(R.string.favorites_rename)).performClick()
        composeRule.onNodeWithTag("favorites_name").performTextClearance()
        composeRule.onNodeWithTag("favorites_name").performTextInput("Renamed")
        composeRule.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        composeRule.onNodeWithText("Renamed").assertIsDisplayed()
        composeRule.onNodeWithTag("favorites_actions:${beta.id}").performClick()
        composeRule.onNodeWithText(context.getString(R.string.favorites_move)).performClick()
        composeRule.onNodeWithTag("favorites_destination:folder").performClick()
        composeRule.onNodeWithText(context.getString(R.string.favorites_move_here)).performClick()
        composeRule.onNodeWithText("Renamed").assertDoesNotExist()
        composeRule.onNodeWithTag("favorites_folder:folder").performClick()
        composeRule.onNodeWithText("Renamed").assertIsDisplayed()
        composeRule.onNodeWithTag("favorites_actions:${beta.id}").performClick()
        composeRule.onNodeWithText(context.getString(R.string.favorites_move)).performClick()
        composeRule.onNodeWithTag("favorites_move_parent").performClick()
        composeRule.onNodeWithText("Renamed").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(null, latest.get().favorites.first { it.id == beta.id }.parentFolderId) }
    }

    @Test
    fun folderCannotSelectItselfOrItsDescendantsAsMoveDestination() {
        val parent = FavoriteFolder("parent", "Parent")
        val child = FavoriteFolder("child", "Child", parentFolderId = parent.id)
        val sibling = FavoriteFolder("sibling", "Sibling")
        val library = FavoriteLibrary(listOf(parent, child, sibling))
        composeRule.setContent {
            MaterialBrowserTheme {
                FavoritesScreen(
                    favorites = emptyList(), library = library,
                    onDeleteFavorite = { _, done -> done(null) }, onUndoDelete = {},
                    onOpenFavorite = {}, onBack = {},
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithTag("favorites_actions:parent").performClick()
        composeRule.onNodeWithText(context.getString(R.string.favorites_move)).performClick()
        composeRule.onNodeWithTag("favorites_destination:parent").assertDoesNotExist()
        composeRule.onNodeWithTag("favorites_destination:child").assertDoesNotExist()
        composeRule.onNodeWithTag("favorites_destination:sibling").assertIsDisplayed()
    }

    @Test
    fun sortMenuOrdersSitesByNameOrNewestFirst() {
        val beta = favorite("https://beta.example/", "Beta", addedAt = 1)
        val alpha = favorite("https://alpha.example/", "Alpha", addedAt = 2)
        val chosen = mutableListOf<FavoritesSort>()
        composeRule.setContent {
            var sort by remember { mutableStateOf(FavoritesSort.Manual) }
            MaterialBrowserTheme {
                FavoritesScreen(
                    favorites = listOf(beta, alpha),
                    onDeleteFavorite = { _, onComplete -> onComplete(null) },
                    onUndoDelete = {},
                    onOpenFavorite = {},
                    onBack = {},
                    sort = sort,
                    onSortChange = { option ->
                        chosen += option
                        sort = option
                    },
                )
            }
        }

        assertEquals(listOf("Beta", "Alpha"), siteOrder("Beta", "Alpha"))
        composeRule.onNodeWithTag(FavoritesScreenTestTags.Sort).performClick()
        composeRule.onNodeWithTag(FavoritesScreenTestTags.sort(FavoritesSort.Name)).performClick()
        assertEquals(listOf("Alpha", "Beta"), siteOrder("Beta", "Alpha"))
        composeRule.onNodeWithTag(FavoritesScreenTestTags.Sort).performClick()
        composeRule.onNodeWithTag(FavoritesScreenTestTags.sort(FavoritesSort.Recent)).performClick()
        assertEquals(listOf("Alpha", "Beta"), siteOrder("Beta", "Alpha"))
        assertEquals(listOf(FavoritesSort.Name, FavoritesSort.Recent), chosen)
    }

    @Test
    fun emptyLibraryOffersBookmarkImportButSearchDoesNot() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var imports = 0
        composeRule.setContent {
            MaterialBrowserTheme {
                FavoritesScreen(
                    favorites = emptyList(),
                    onDeleteFavorite = { _, onComplete -> onComplete(null) },
                    onUndoDelete = {},
                    onOpenFavorite = {},
                    onBack = {},
                    onImportBookmarks = { imports++ },
                )
            }
        }

        val action = context.getString(R.string.favorites_empty_action)
        composeRule.onNodeWithText(action).performClick()
        assertEquals(1, imports)
        composeRule.onNodeWithTag(FavoritesScreenTestTags.SearchField).performTextInput("nothing")
        composeRule.onNodeWithText(action).assertDoesNotExist()
    }

    /** The given site titles, top to bottom as they sit on screen. */
    private fun siteOrder(vararg titles: String): List<String> = titles.sortedBy { title ->
        composeRule.onNodeWithText(title).getUnclippedBoundsInRoot().top.value
    }

    private fun favorite(url: String, title: String, addedAt: Long = System.currentTimeMillis()) = FavoriteEntry(
        url = url,
        title = title,
        addedAt = addedAt,
    )
}
