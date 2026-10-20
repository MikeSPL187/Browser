package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.DistractionPickingStep
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Materials
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile

object V5DistractionsPanelTestTags {
    const val Panel = "v5_distractions_panel"
    const val ShowHidden = "v5_distractions_show_hidden"
    const val Undo = "v5_distractions_undo"
    const val Done = "v5_distractions_done"
    const val Step = "v5_distractions_step"
}

/**
 * The «Hide distracting items» hint while a page is in choosing mode (V20, canvas screen «k1»,
 * system-v5.md section 12): at the top edge, since the bar is at the bottom. A tap on the page
 * outlines a block, a second tap hides it; «Undo» brings back the block hidden last, «Show hidden
 * items» all of the site's, and «Done» ends choosing. In a private tab the choice lasts only until
 * the private tabs close.
 */
@Composable
fun V5DistractionsPanel(
    visible: Boolean,
    site: String,
    privateTab: Boolean,
    /** Whether the site has blocks hidden, which «Show hidden items» brings back. */
    hasHidden: Boolean,
    /** A block was hidden since choosing started: «Undo» takes the place of «Show hidden items». */
    canUndo: Boolean,
    /** What the last tap did, said under the hint, so TalkBack hears the outline and the hiding. */
    step: DistractionPickingStep,
    onUndo: () -> Unit,
    onShowHidden: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Without system animations the panel only fades (plan-v5.md, section 5).
    val animated = V5Motion.animationsEnabled
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (animated) slideInVertically { -it } + fadeIn() else fadeIn(V5Motion.reducedFade()),
        exit = if (animated) slideOutVertically { -it } + fadeOut() else fadeOut(V5Motion.reducedFade()),
    ) {
        val colors = V5Theme.colors
        val shape = V5Theme.shapes.l
        val title = stringResource(R.string.v5_distractions_hint)
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = V5Spacing.marginPhone, vertical = V5Spacing.space2)
                .fillMaxWidth()
                .clip(shape)
                .background(V5Materials.thick.tint(dark = colors.isDark, blurAvailable = false, reduceTransparency = false))
                .border(V5Spacing.hairline, colors.edge, shape)
                .semantics { paneTitle = title }
                .padding(V5Spacing.space4)
                .testTag(V5DistractionsPanelTestTags.Panel),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space3),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
            ) {
                Box(
                    Modifier
                        .size(V5Sizes.suggestionTile)
                        .clip(V5Theme.shapes.xs)
                        .background(V5Tile.Blue.color),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VolaIcons.VisibilityOff, contentDescription = null, tint = V5Tile.Blue.glyph, modifier = Modifier.size(V5Sizes.iconSmall))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = V5Theme.type.headline, color = colors.label)
                    // What the last tap did takes the line's place, in a polite live region: TalkBack
                    // says the outline and the hiding, which only show on the page.
                    Text(
                        when {
                            step == DistractionPickingStep.Selected -> stringResource(R.string.v5_distractions_selected)
                            step == DistractionPickingStep.Hidden -> stringResource(R.string.v5_distractions_hidden)
                            privateTab -> stringResource(R.string.v5_distractions_private)
                            else -> stringResource(R.string.v5_distractions_site, site)
                        },
                        style = V5Theme.type.footnote,
                        color = colors.labelSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .testTag(V5DistractionsPanelTestTags.Step)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
            ) {
                // TalkBack hears the hint and the button change as blocks are hidden and brought back.
                Box(Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }) {
                    when {
                        canUndo -> PanelButton(
                            text = stringResource(R.string.v5_distractions_undo),
                            background = colors.fill,
                            content = colors.label,
                            onClick = onUndo,
                            modifier = Modifier.fillMaxWidth().testTag(V5DistractionsPanelTestTags.Undo),
                        )
                        hasHidden -> PanelButton(
                            text = stringResource(R.string.v5_distractions_show_all),
                            background = colors.fill,
                            content = colors.label,
                            onClick = onShowHidden,
                            modifier = Modifier.fillMaxWidth().testTag(V5DistractionsPanelTestTags.ShowHidden),
                        )
                    }
                }
                PanelButton(
                    text = stringResource(R.string.v5_distractions_done),
                    background = colors.accentFill,
                    content = colors.onAccentFill,
                    onClick = onDone,
                    modifier = Modifier.testTag(V5DistractionsPanelTestTags.Done),
                )
            }
        }
    }
}

@Composable
private fun PanelButton(text: String, background: Color, content: Color, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.full)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = V5Spacing.space5, vertical = V5Spacing.space1),
        contentAlignment = Alignment.Center,
    ) {
        // Two lines rather than an ellipsis: at the largest font a label still reads whole.
        Text(
            text,
            style = V5Theme.type.headline,
            color = content,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
