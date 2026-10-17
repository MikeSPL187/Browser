package dev.sk2andy.materialbrowser.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.cast.CastUiState
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5ListGroup
import dev.sk2andy.materialbrowser.ui.v5.V5ListRow
import dev.sk2andy.materialbrowser.ui.v5.V5RowIcon
import dev.sk2andy.materialbrowser.ui.v5.V5SheetHeader
import dev.sk2andy.materialbrowser.ui.v5.V5Slider
import dev.sk2andy.materialbrowser.ui.v5.V5WindowSheet

/*
 * Casting under v5 on the phone (owner 17.10: the phone all v5): the line above the bar reads like
 * the v5 notices, a card with what plays where, how far it is, play or pause and stop; tapping it
 * opens a v5 sheet with the position, the volume and the same two actions.
 */

@Composable
internal fun CastMiniControllerV5(
    state: CastUiState,
    onExpand: () -> Unit,
    onTogglePlayback: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = V5Theme.colors
    val progress = state.durationMillis
        ?.takeIf { it > 0L }
        ?.let { duration -> (state.positionMillis.toFloat() / duration).coerceIn(0f, 1f) }
        ?: 0f
    Column(
        modifier
            .padding(horizontal = V5Spacing.marginPhone)
            .fillMaxWidth()
            .clip(V5Theme.shapes.s)
            .background(colors.surfaceElevated)
            .clickable(role = Role.Button, onClick = onExpand)
            .testTag(CastControlsTestTags.MiniController),
    ) {
        Row(
            Modifier
                .heightIn(min = V5Sizes.rowTwoLine)
                .padding(start = V5Spacing.space4, end = V5Spacing.space1),
            horizontalArrangement = Arrangement.spacedBy(V5Spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_cast_connected),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(V5Sizes.icon),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    castTitle(state),
                    style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    castDevice(state),
                    style = V5Theme.type.caption,
                    color = colors.labelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onTogglePlayback, modifier = Modifier.testTag(CastControlsTestTags.PlayPause)) {
                Icon(playPausePainter(state), contentDescription = playPauseLabel(state), tint = colors.label)
            }
            IconButton(onClick = onDisconnect, modifier = Modifier.testTag(CastControlsTestTags.Disconnect)) {
                Icon(VolaIcons.Close, contentDescription = stringResource(R.string.cd_cast_stop), tint = colors.labelSecondary)
            }
        }
        // How far it has played, as the bar's loading line.
        Box(Modifier.fillMaxWidth().height(V5Sizes.barProgress).background(colors.fill)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(colors.accentFill))
        }
    }
}

@Composable
internal fun CastExpandedControllerV5(
    state: CastUiState,
    visible: Boolean,
    onDismiss: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDisconnect: () -> Unit,
) {
    val colors = V5Theme.colors
    val duration = state.durationMillis?.coerceAtLeast(0L) ?: 0L
    var seekPosition by remember(state.positionMillis, duration) {
        mutableFloatStateOf(state.positionMillis.coerceIn(0L, duration.coerceAtLeast(1L)).toFloat())
    }
    var volume by remember(state.deviceVolume) { mutableFloatStateOf(state.deviceVolume.coerceIn(0f, 1f)) }
    val title = castTitle(state)
    V5WindowSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = title,
        modifier = Modifier.testTag(CastControlsTestTags.ExpandedController),
    ) {
        V5SheetHeader(title = title, subtitle = castDevice(state), onClose = onDismiss)
        Column(
            Modifier.padding(horizontal = V5Spacing.marginPhone + V5Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        ) {
            Text(
                stringResource(R.string.cast_position, formattedMediaTime(seekPosition.toLong()), formattedMediaTime(duration)),
                style = V5Theme.type.footnote,
                color = colors.labelSecondary,
            )
            V5Slider(
                value = seekPosition,
                onValueChange = { seekPosition = it },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                onValueChangeFinished = { onSeek(seekPosition.toLong()) },
                enabled = duration > 0L,
                modifier = Modifier.testTag(CastControlsTestTags.Seek),
            )
            Text(stringResource(R.string.cast_volume), style = V5Theme.type.footnote, color = colors.labelSecondary)
            V5Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    onVolumeChange(it)
                },
                modifier = Modifier.testTag(CastControlsTestTags.Volume),
            )
        }
        V5ListGroup(Modifier.padding(vertical = V5Spacing.space4)) {
            V5ListRow(
                title = playPauseLabel(state),
                icon = V5RowIcon(if (state.isPlaying) VolaIcons.Pause else VolaIcons.PlayArrowFilled),
                titleColor = colors.accent,
                onClick = onTogglePlayback,
            )
            V5ListRow(
                title = stringResource(R.string.cd_cast_stop),
                icon = V5RowIcon(VolaIcons.Close),
                titleColor = colors.danger,
                onClick = {
                    onDismiss()
                    onDisconnect()
                },
            )
        }
    }
}

@Composable
private fun castTitle(state: CastUiState): String = state.title.ifBlank { stringResource(R.string.cast_unknown_title) }

@Composable
private fun castDevice(state: CastUiState): String =
    stringResource(R.string.cast_playing_on, state.deviceName.ifBlank { stringResource(R.string.cast_unknown_device) })

@Composable
private fun playPauseLabel(state: CastUiState): String =
    stringResource(if (state.isPlaying) R.string.cd_cast_pause else R.string.cd_cast_play)

@Composable
private fun playPausePainter(state: CastUiState): Painter =
    if (state.isPlaying) painterResource(R.drawable.ic_pause) else rememberVectorPainter(VolaIcons.PlayArrowFilled)
