package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Elevation
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.v5Elevation
import dev.sk2andy.materialbrowser.ui.theme.v5.v5Glow

/** The private tabs are locked: how many, and whether this device can unlock them. */
@Immutable
data class V5OverviewPrivateLock(val count: Int, val unlockAvailable: Boolean)

internal object V5OverviewPrivateLockTestTags {
    const val Card = "v5_overview_private_lock"
    const val Unlock = "v5_overview_private_unlock"
    const val CloseAll = "v5_overview_private_close_all"
}

/**
 * The private segment while its tabs are locked (canvas «PrivateLock»): a card with «Unlock» and
 * «Close all private tabs» over blurred plates where the cards would be. The plates are plain
 * shapes, not the tabs: nothing of a locked tab (title, address, picture) is drawn, even blurred.
 * Blur needs Android 12; before it the plates stay sharp and faded, which shows no more.
 */
@Composable
internal fun V5OverviewPrivateLockLayer(
    lock: V5OverviewPrivateLock,
    onUnlock: () -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        Plates(count = lock.count)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = V5Spacing.space6, vertical = V5Spacing.space6),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LockCard(lock, onUnlock, onCloseAll)
        }
    }
}

@Composable
private fun Plates(count: Int) {
    val colors = V5Theme.colors
    val plates = count.coerceIn(1, MAX_PLATES)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = V5Spacing.marginPhone, end = V5Spacing.marginPhone, top = V5Spacing.space3)
            .blur(V5Sizes.privateLockBlur)
            .alpha(PLATE_ALPHA)
            .clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
    ) {
        (0 until plates).chunked(V5Sizes.TAB_COLUMNS_PHONE).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(V5Sizes.tabGridGap)) {
                repeat(V5Sizes.TAB_COLUMNS_PHONE) { column ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(V5Sizes.OVERVIEW_CARD_RATIO)
                            .then(
                                if (column < row.size) {
                                    Modifier.clip(V5Theme.shapes.m).background(colors.surfaceHighest)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun LockCard(lock: V5OverviewPrivateLock, onUnlock: () -> Unit, onCloseAll: () -> Unit) {
    val colors = V5Theme.colors
    val title = stringResource(R.string.private_tabs_locked_title)
    Column(
        Modifier
            .widthIn(max = V5Sizes.stateContentMax)
            .fillMaxWidth()
            .v5Elevation(V5Elevation.e3, V5Theme.shapes.xl)
            .clip(V5Theme.shapes.xl)
            .background(colors.surfaceElevated)
            .semantics { paneTitle = title }
            .testTag(V5OverviewPrivateLockTestTags.Card)
            .padding(
                start = V5Spacing.space6,
                end = V5Spacing.space6,
                top = V5Spacing.space8,
                bottom = V5Spacing.space5,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(V5Sizes.privateGem)
                .v5Glow(colors.accentFill, V5Theme.shapes.full)
                .clip(V5Theme.shapes.full)
                .background(colors.accentFill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                VolaIcons.Lock,
                contentDescription = null,
                tint = colors.onAccentFill,
                modifier = Modifier.size(V5Sizes.privateGemIcon),
            )
        }
        Spacer(Modifier.height(V5Spacing.space5))
        Text(
            title,
            style = V5Theme.type.title,
            color = colors.label,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(V5Spacing.space2))
        Text(
            if (lock.unlockAvailable) {
                pluralStringResource(R.plurals.private_tabs_locked_message, lock.count, lock.count)
            } else {
                stringResource(R.string.profile_protection_unavailable)
            },
            style = V5Theme.type.callout,
            color = colors.labelSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(V5Spacing.space5))
        V5StateActions(
            primary = V5StateAction(
                label = stringResource(R.string.private_tabs_unlock_action),
                onClick = onUnlock,
                icon = VolaIcons.Fingerprint,
                enabled = lock.unlockAvailable,
                testTag = V5OverviewPrivateLockTestTags.Unlock,
            ),
            secondary = listOf(
                V5StateAction(
                    pluralStringResource(R.plurals.private_tab_close_all, lock.count, lock.count),
                    onCloseAll,
                    testTag = V5OverviewPrivateLockTestTags.CloseAll,
                    destructive = true,
                ),
            ),
        )
    }
}

/** Two rows of plates are enough to read as «tabs are here». */
private const val MAX_PLATES = 4

/** The plates sit back behind the card, as the canvas has them. */
private const val PLATE_ALPHA = 0.7f
