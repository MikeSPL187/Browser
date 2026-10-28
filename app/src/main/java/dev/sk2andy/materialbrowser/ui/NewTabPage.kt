@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)

package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import dev.sk2andy.materialbrowser.data.ProtectionSummary
import dev.sk2andy.materialbrowser.data.ProtectionWeek
import dev.sk2andy.materialbrowser.ui.theme.VolaSpacing
/** Where the new tab's content may reach. */
internal object NewTabLayoutRules {
    /**
     * Space kept under the last card. The floating address bar covers the bottom of the page by
     * its height and its margins above and below; with the bar docked aside, a plain gap.
     */
    fun bottomContentPadding(addressBarDocked: Boolean, addressBarHeight: Dp): Dp {
        if (addressBarDocked) return VolaSpacing.x12
        return maxOf(VolaSpacing.x12, addressBarHeight + ADDRESS_BAR_VERTICAL_MARGIN * 2)
    }
}

/** The private new tab: what the engine keeps, and how many private tabs «Close all» closes. */
internal class NewTabPrivate(
    val storage: PrivateTabStorage,
    val count: Int,
    val onCloseAll: () -> Unit,
    val lock: PrivateTabLock? = null,
)

/** The protection card's week and what its report can do. */
internal class NewTabProtection(
    val week: ProtectionWeek,
    val onClear: () -> Unit,
    val onHideCard: () -> Unit,
    /** The privacy report over the last given days, for the v5 report (V18). */
    val summary: (windowDays: Int) -> ProtectionSummary,
)

internal const val NEW_TAB_RECENT_TAB_COUNT = 3

