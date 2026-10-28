package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Type.tabular

internal object V5ListenRowTestTags {
    const val Row = "v5_listen_row"
    const val PlayPause = "v5_listen_play_pause"
    const val Close = "v5_listen_close"
    const val Speed = "v5_listen_speed"
    const val Progress = "v5_listen_progress"
}

/** What the mini player shows: the article (or why it cannot speak) and whether it speaks now. */
internal data class V5ListenRowState(
    /** The article's title; null when it is a private page seen from a normal tab. */
    val title: String?,
    val speaking: Boolean,
    /** Pause and resume apply: the voice has the article. */
    val canPlay: Boolean = true,
    /** Said instead of the title: the page is being prepared, or why it cannot be read aloud. */
    val message: String? = null,
    /** How much of the article is said, from 0 to 1, with the time said and the whole («3:12 / 8:40»). */
    val timing: V5ListenTiming? = null,
    /** The voice's speed on the capsule («1,2×»); null hides it. */
    val speed: String? = null,
)

/** The line under the title: the article said so far, as a share and as time. */
internal data class V5ListenTiming(
    val progress: Float,
    val text: String,
    /** What TalkBack says: the time is an estimate from the article's words. */
    val description: String,
)

/**
 * The listening mini player (V19b, system-v5.md section 10; canvas screen «Reader»): a second row
 * above the bar, in the bar's colors. A round play button, the title over the line of the article
 * said so far, the speed capsule and stop.
 */
@Composable
internal fun V5ListenRow(
    state: V5ListenRowState,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onSpeed: () -> Unit = {},
) {
    val bar = listenColors()
    val label = state.message ?: state.title ?: stringResource(R.string.listen_private_page)
    val paneTitle = stringResource(R.string.reader_listen)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.listenRow)
            .padding(start = V5Spacing.space3, end = V5Spacing.space1)
            .semantics { this.paneTitle = paneTitle }
            .testTag(V5ListenRowTestTags.Row),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.canPlay) {
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(V5Sizes.listenPlay).testTag(V5ListenRowTestTags.PlayPause),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = bar.onTint,
                    contentColor = bar.container,
                ),
            ) {
                Icon(
                    if (state.speaking) VolaIcons.Pause else VolaIcons.PlayArrow,
                    contentDescription = stringResource(
                        if (state.speaking) R.string.reader_speech_pause else R.string.reader_speech_resume,
                    ),
                    modifier = Modifier.size(V5Sizes.iconSmall),
                )
            }
        } else {
            Box(Modifier.size(V5Sizes.listenPlay), contentAlignment = Alignment.Center) {
                Icon(VolaIcons.Headphones, contentDescription = null)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(V5Spacing.space1)) {
            Text(
                text = label,
                style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            state.timing?.let { timing -> TimingLine(timing, bar) }
        }
        state.speed?.let { speed -> SpeedCapsule(speed, bar, onSpeed) }
        IconButton(onClick = onClose, modifier = Modifier.testTag(V5ListenRowTestTags.Close)) {
            Icon(
                VolaIcons.Close,
                contentDescription = stringResource(R.string.reader_speech_stop),
                tint = bar.onTintSecondary,
            )
        }
    }
}

@Composable
private fun TimingLine(timing: V5ListenTiming, bar: V5BarColors) {
    Row(
        // The tag goes before the cleared semantics, which would drop it.
        Modifier
            .testTag(V5ListenRowTestTags.Progress)
            .clearAndSetSemantics { contentDescription = timing.description },
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(V5Sizes.listenProgress)
                .clip(V5Theme.shapes.full)
                .background(bar.field),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(timing.progress.coerceIn(0f, 1f))
                    .clip(V5Theme.shapes.full)
                    .background(bar.onTint),
            )
        }
        Text(
            text = timing.text,
            style = V5Theme.type.caption.tabular(),
            color = bar.onTintSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun SpeedCapsule(speed: String, bar: V5BarColors, onSpeed: () -> Unit) {
    val description = stringResource(R.string.v5_listen_speed_description, speed)
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = V5Sizes.listenSpeed)
            .clip(V5Theme.shapes.full)
            .border(V5Sizes.listenSpeedBorder, bar.field, V5Theme.shapes.full)
            .clickable(role = Role.Button, onClick = onSpeed)
            .semantics { contentDescription = description }
            .padding(horizontal = V5Spacing.space3)
            .testTag(V5ListenRowTestTags.Speed),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.v5_listen_speed, speed),
            style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold).tabular(),
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/** The bar's colors when the row sits in the bar; the theme's chrome in a preview of the row alone. */
@Composable
private fun listenColors(): V5BarColors {
    val bar = LocalV5BarColors.current
    val colors = V5Theme.colors
    return V5BarColors(
        container = bar.container.takeOrElse { colors.chrome },
        onTint = bar.onTint.takeOrElse { colors.label },
        onTintSecondary = bar.onTintSecondary.takeOrElse { colors.labelSecondary },
        field = bar.field.takeOrElse { colors.fill },
    )
}

/** [V5ListenRow] that unfolds above the bar on the sheet spring; without animations it only fades. */
@Composable
internal fun V5ListenAccessory(
    state: V5ListenRowState?,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    onSpeed: () -> Unit = {},
) {
    // The last state stays on screen while the row folds away (kept out of snapshot state, so
    // composition does not write the state it reads).
    val last = remember { LastListenRow() }
    if (state != null) last.state = state
    val animated = V5Motion.animationsEnabled
    AnimatedVisibility(
        visible = state != null,
        enter = if (animated) {
            expandVertically(V5Motion.sheet.spec()) + fadeIn(V5Motion.effects.spec())
        } else {
            fadeIn(V5Motion.reducedFade())
        },
        exit = if (animated) {
            shrinkVertically(V5Motion.sheet.spec()) + fadeOut(V5Motion.effects.spec())
        } else {
            fadeOut(V5Motion.reducedFade())
        },
    ) {
        last.state?.let { V5ListenRow(state = it, onPlayPause = onPlayPause, onClose = onClose, onSpeed = onSpeed) }
    }
}

private class LastListenRow {
    var state: V5ListenRowState? = null
}
