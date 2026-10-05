package dev.sk2andy.materialbrowser.shared.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.data.BrowserDensity

/** The density the theme sets for rows of lists and settings. */
val LocalBrowserDensity = staticCompositionLocalOf { BrowserDensity.Normal }

/**
 * Rows of lists and settings (Q17c): «Compact» takes height off every row, «Comfortable» adds
 * some. The step goes half above and half below the content, and no row drops under the 48 dp
 * touch target.
 */
object ListDensityRules {
    val minTouchTarget = 48.dp
    private val compactStep = (-12).dp
    private val comfortableStep = 8.dp

    fun step(density: BrowserDensity): Dp = when (density) {
        BrowserDensity.Compact -> compactStep
        BrowserDensity.Normal -> 0.dp
        BrowserDensity.Comfortable -> comfortableStep
    }

    fun rowMinHeight(base: Dp, density: BrowserDensity): Dp =
        maxOf(base + step(density), minTouchTarget)

    fun verticalPadding(base: Dp, density: BrowserDensity): Dp =
        maxOf(base + step(density) / 2, 0.dp)
}

/** [base] as the row height for the current density. */
@Composable
@ReadOnlyComposable
fun densityRowMinHeight(base: Dp): Dp =
    ListDensityRules.rowMinHeight(base, LocalBrowserDensity.current)

/** [base] as the padding above and below a row's content for the current density. */
@Composable
@ReadOnlyComposable
fun densityVerticalPadding(base: Dp): Dp =
    ListDensityRules.verticalPadding(base, LocalBrowserDensity.current)

/** [base] with its top and bottom moved for the current density; start and end stay. */
@Composable
@ReadOnlyComposable
fun densityRowPadding(base: PaddingValues): PaddingValues {
    val density = LocalBrowserDensity.current
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = base.calculateStartPadding(direction),
        top = ListDensityRules.verticalPadding(base.calculateTopPadding(), density),
        end = base.calculateEndPadding(direction),
        bottom = ListDensityRules.verticalPadding(base.calculateBottomPadding(), density),
    )
}
