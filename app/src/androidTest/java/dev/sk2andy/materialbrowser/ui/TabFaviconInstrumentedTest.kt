package dev.sk2andy.materialbrowser.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.sk2andy.materialbrowser.browser.BrowserTab
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The letter that stands in for a site's icon stays inside its tile at any font size. */
@RunWith(AndroidJUnit4::class)
class TabFaviconInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun letterFitsItsTileAtDoubleFontSize() {
        var tilePx = 0f
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                tilePx = with(LocalDensity.current) { TILE.toPx() }
                MaterialTheme {
                    TabFavicon(
                        tab = BrowserTab(id = "tab", lastAccessedAt = 0, title = "Vola", url = "https://example.com/"),
                        favicon = null,
                        size = TILE,
                    )
                }
            }
        }

        val node = composeRule.onNodeWithText("V").fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        assertTrue("letter ${layout.size.height} px tall in a $tilePx px tile", layout.size.height <= tilePx + 1f)
        assertFalse("letter is cut off", layout.hasVisualOverflow)
    }

    private companion object {
        val TILE = 20.dp
    }
}
