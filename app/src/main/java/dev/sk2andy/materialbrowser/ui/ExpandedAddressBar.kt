@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
)

package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object SegmentedAddressBarGeometry {
    val ACTION_SIZE = 48.dp
    val INSET = 8.dp
    val SEGMENT_GAP = INSET
    val EXPANDED_HEIGHT = ACTION_SIZE + INSET * 2
    private val MAX_INNER_CORNER_RADIUS = 24.dp
    private val MAX_OUTER_CORNER_RADIUS = 32.dp

    fun innerCornerRadius(configuredCornerRadius: Dp): Dp =
        minOf(configuredCornerRadius, MAX_INNER_CORNER_RADIUS)

    fun outerCornerRadius(configuredCornerRadius: Dp): Dp = minOf(
        innerCornerRadius(configuredCornerRadius) + INSET,
        MAX_OUTER_CORNER_RADIUS,
    )
}

internal object AddressBarTestTags {
    const val AiModeToggle = "address_bar_ai_mode_toggle"
    const val Editor = "address_bar_editor"
    const val PrimaryField = "address_bar_primary_field"
    const val IncognitoToggle = "address_bar_incognito_toggle"
    const val QrScanner = "address_bar_qr_scanner"
    const val SegmentedContainer = "address_bar_segmented_container"
    const val TabButton = "address_bar_tab_button"
    const val SegmentedPrimary = "address_bar_segmented_primary"
    const val SegmentedSecondary = "address_bar_segmented_secondary"
}
