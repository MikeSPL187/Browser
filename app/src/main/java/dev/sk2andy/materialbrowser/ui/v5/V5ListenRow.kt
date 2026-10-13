package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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

internal object V5ListenRowTestTags {
    const val Row = "v5_listen_row"
    const val PlayPause = "v5_listen_play_pause"
    const val Close = "v5_listen_close"
}

/** What the mini player shows: the article (or why it cannot speak) and whether it speaks now. */
internal data class V5ListenRowState(
    /** The article's title; null when it is a private page seen from a normal tab. */
    val title: String?,
    val speaking: Boolean,
    val unavailable: Boolean,
)

/**
 * The listening mini player (V19b, system-v5.md section 10): a second row above the bar, in the
 * bar's colors (the content color comes from the bar). Pause or resume, and stop.
 */
@Composable
internal fun V5ListenRow(
    state: V5ListenRowState,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when {
        state.unavailable -> stringResource(R.string.listen_unavailable)
        else -> state.title ?: stringResource(R.string.listen_private_page)
    }
    val paneTitle = stringResource(R.string.reader_listen)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.touchTarget)
            .padding(start = V5Spacing.space4, end = V5Spacing.space1)
            .semantics { this.paneTitle = paneTitle }
            .testTag(V5ListenRowTestTags.Row),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(VolaIcons.Headphones, contentDescription = null)
        Text(
            text = label,
            style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (!state.unavailable) {
            IconButton(onClick = onPlayPause, modifier = Modifier.testTag(V5ListenRowTestTags.PlayPause)) {
                Icon(
                    if (state.speaking) VolaIcons.Pause else VolaIcons.PlayArrow,
                    contentDescription = stringResource(
                        if (state.speaking) R.string.reader_speech_pause else R.string.reader_speech_resume,
                    ),
                )
            }
        }
        IconButton(onClick = onClose, modifier = Modifier.testTag(V5ListenRowTestTags.Close)) {
            Icon(VolaIcons.Close, contentDescription = stringResource(R.string.reader_speech_stop))
        }
    }
}

/** [V5ListenRow] that unfolds above the bar on the sheet spring; without animations it only fades. */
@Composable
internal fun V5ListenAccessory(
    state: V5ListenRowState?,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
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
        last.state?.let { V5ListenRow(state = it, onPlayPause = onPlayPause, onClose = onClose) }
    }
}

private class LastListenRow {
    var state: V5ListenRowState? = null
}
