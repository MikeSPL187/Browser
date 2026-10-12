package dev.sk2andy.materialbrowser.ui.v5

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

object V5PrivateNewTabTestTags {
    const val Page = "v5_private_new_tab"
    const val Lock = "v5_private_lock"
    const val CloseAll = "v5_private_close_all"
}

/** One line of what a private tab keeps and does not; [warning] marks a promise the engine cannot keep. */
@Immutable
data class V5PrivateFact(
    @param:DrawableRes val icon: Int,
    val title: String,
    val caption: String,
    val warning: Boolean = false,
)

/** «Lock on exit»: [available] is false without a strong biometric, and [caption] says so. */
@Immutable
data class V5PrivateLock(val checked: Boolean, val available: Boolean, val caption: String)

@Immutable
data class V5PrivateNewTabState(
    /** What the engine lets the page promise: everything gone, or only the history. */
    val body: String,
    val facts: List<V5PrivateFact>,
    val lock: V5PrivateLock?,
    /** Private tabs open now; «Close all» shows from one. */
    val privateTabCount: Int,
)

/**
 * The v5 private new tab (V14a, canvas screen «h1», system-v5.md section 1.4): always the dark
 * violet-gray private palette with a soft accent glow from the top, the mask, «Private browsing»,
 * what is and is not kept for this engine, «Lock on exit» and «Close all private tabs».
 */
@Composable
fun V5PrivateNewTab(
    state: V5PrivateNewTabState,
    onLockChange: (Boolean) -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = V5Theme.colors
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag(V5PrivateNewTabTestTags.Page),
    ) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }
        Box(
            Modifier
                .fillMaxWidth()
                .height(V5Sizes.newTabWash)
                .background(
                    Brush.radialGradient(
                        colors = listOf(colors.accentSoft, Color.Transparent),
                        center = Offset(width / 2f, 0f),
                        radius = width,
                    ),
                ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = V5Spacing.space6, vertical = V5Spacing.space16),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
        ) {
            Box(
                Modifier
                    .size(V5Sizes.privateGem)
                    .clip(V5Theme.shapes.full)
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_incognito_filled),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(V5Sizes.privateGemIcon),
                )
            }
            Text(
                stringResource(R.string.v5_private_title),
                style = V5Theme.type.largeTitle,
                color = colors.label,
                modifier = Modifier.semantics { heading() },
            )
            Text(state.body, style = V5Theme.type.callout, color = colors.labelSecondary)
            Facts(state.facts, state.lock, onLockChange)
            if (state.privateTabCount > 0) {
                CloseAll(state.privateTabCount, onCloseAll)
            }
        }
    }
}

@Composable
private fun Facts(facts: List<V5PrivateFact>, lock: V5PrivateLock?, onLockChange: (Boolean) -> Unit) {
    Column(
        Modifier
            .padding(top = V5Spacing.space2)
            .fillMaxWidth()
            .clip(V5Theme.shapes.m)
            .background(V5Theme.colors.surface),
    ) {
        facts.forEach { fact -> FactRow(fact) }
        if (lock != null) {
            V5ListRow(
                title = stringResource(R.string.private_tab_lock_title),
                subtitle = lock.caption,
                icon = V5RowIcon(VolaIcons.Fingerprint),
                trailing = V5RowTrailing.Toggle(
                    checked = lock.checked && lock.available,
                    onCheckedChange = onLockChange,
                ),
                enabled = lock.available,
                modifier = Modifier.testTag(V5PrivateNewTabTestTags.Lock),
            )
        }
    }
}

@Composable
private fun FactRow(fact: V5PrivateFact) {
    val colors = V5Theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.rowTwoLine)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = V5Spacing.space4, vertical = V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
    ) {
        Box(Modifier.size(V5Sizes.rowTile), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(fact.icon),
                contentDescription = null,
                tint = if (fact.warning) colors.warning else colors.accent,
                modifier = Modifier.size(V5Sizes.iconSmall),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(fact.title, style = V5Theme.type.body, color = if (fact.warning) colors.warning else colors.label)
            Text(fact.caption, style = V5Theme.type.footnote, color = colors.labelSecondary)
        }
    }
}

@Composable
private fun CloseAll(count: Int, onCloseAll: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.rowSingleLine)
            .clip(V5Theme.shapes.m)
            .background(V5Theme.colors.surface)
            .clickable(role = Role.Button, onClick = onCloseAll)
            .testTag(V5PrivateNewTabTestTags.CloseAll),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            pluralStringResource(R.plurals.private_tab_close_all, count, count),
            style = V5Theme.type.body,
            color = V5Theme.colors.danger,
        )
    }
}
