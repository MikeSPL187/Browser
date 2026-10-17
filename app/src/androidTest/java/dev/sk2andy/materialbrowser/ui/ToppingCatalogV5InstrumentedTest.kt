package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Toppings catalog under ui.v5: the same states, tags and callbacks as the v4 screen. */
@RunWith(AndroidJUnit4::class)
class ToppingCatalogV5InstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun loadingAndErrorAreStatesOfThePage() {
        var state by mutableStateOf<ToppingCatalogUiState>(ToppingCatalogUiState.Loading)
        var retried = false
        composeRule.setContent { Catalog(state, onRetry = { retried = true }) }

        composeRule.onNodeWithTag(ToppingCatalogTestTags.Loading).assertExists()
        state = ToppingCatalogUiState.Error("Catalog offline")
        composeRule.onNodeWithText("Catalog offline").assertExists()
        composeRule.onNodeWithTag(ToppingCatalogTestTags.Retry).performClick()

        assertTrue(retried)
    }

    @Test
    fun aToppingSaysWhatItIsAndInstallsAndUpdates() {
        val toggles = mutableListOf<Pair<String, Boolean>>()
        val updates = mutableListOf<String>()
        composeRule.setContent {
            Catalog(
                ToppingCatalogUiState.Content(listOf(topping())),
                onToggle = { id, enabled -> toggles += id to enabled },
                onUpdate = { updates += it },
            )
        }

        composeRule.onNodeWithText("Readable copy buttons", substring = true).assertExists()
        composeRule.onNodeWithText(
            context.getString(R.string.topping_catalog_author, "Vola Community"),
            substring = true,
        ).assertExists()
        composeRule.onNodeWithTag(ToppingCatalogTestTags.toggle("clean-copy")).assertIsOff().performClick()
        composeRule.onNodeWithTag(ToppingCatalogTestTags.update("clean-copy")).performScrollTo().performClick()

        assertEquals(listOf("clean-copy" to true), toggles)
        assertEquals(listOf("clean-copy"), updates)
    }

    @Test
    fun aSavedCatalogIsSaidAndABusyToppingWaits() {
        composeRule.setContent { Catalog(ToppingCatalogUiState.Cached(listOf(topping().copy(busy = true)))) }

        composeRule.onNodeWithTag(ToppingCatalogTestTags.CachedNotice).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.topping_catalog_working)).assertExists()
        composeRule.onNodeWithTag(ToppingCatalogTestTags.toggle("clean-copy")).assertIsNotEnabled()
        composeRule.onNodeWithTag(ToppingCatalogTestTags.update("clean-copy")).assertIsNotEnabled()
    }

    @Test
    fun anEmptyCatalogSaysSo() {
        composeRule.setContent { Catalog(ToppingCatalogUiState.Content(emptyList())) }

        composeRule.onNodeWithTag(ToppingCatalogTestTags.Empty).assertExists()
    }

    @Composable
    private fun Catalog(
        state: ToppingCatalogUiState,
        onToggle: (String, Boolean) -> Unit = { _, _ -> },
        onUpdate: (String) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        MaterialBrowserTheme {
            ToppingCatalogV5(state = state, onToggle = onToggle, onUpdate = onUpdate, onRetry = onRetry, onDismiss = {})
        }
    }

    private fun topping() = ToppingCatalogItem(
        id = "clean-copy",
        name = "Clean Copy",
        description = "Readable copy buttons",
        author = "Vola Community",
        license = "MIT",
        version = "1.2.0",
        scopes = listOf("https://example.com/*"),
        installed = true,
        enabled = false,
        updateAvailable = true,
    )
}
