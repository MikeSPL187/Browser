package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

internal object V5FeedbackNoticeTestTags {
    const val Row = "v5_feedback_notice"
    const val Action = "v5_feedback_notice_action"
    const val Close = "v5_feedback_notice_close"
}

/**
 * A message for the v5 notice line that says it went wrong: the line shows the red mark (canvas
 * «ц2»). Plain [SnackbarVisuals] read as news or an offer.
 */
@Immutable
internal data class V5FeedbackVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
    val error: Boolean = false,
) : SnackbarVisuals

/**
 * The browser's short messages under v5 (canvas «ц1», «ц2»): «Tab closed — Undo», an offer with
 * «Open», an error with its red mark. A card just above the bar, over every screen, read like the
 * line of a finished download: the message, then its one action in the accent. TalkBack reads it when it
 * appears: Material's SnackbarHost announces it, so the card adds no live region of its own (two read it
 * twice). It is the same message channel as before ([SnackbarData]), so timing, the action and
 * dismissing stay as they were.
 */
@Composable
internal fun V5FeedbackNotice(data: SnackbarData, modifier: Modifier = Modifier) {
    val colors = V5Theme.colors
    val visuals = data.visuals
    val error = (visuals as? V5FeedbackVisuals)?.error == true
    Row(
        modifier
            .padding(horizontal = V5Spacing.marginPhone)
            .fillMaxWidth()
            .heightIn(min = V5Sizes.touchTarget)
            .clip(V5Theme.shapes.s)
            .background(colors.surfaceElevated)
            .padding(start = V5Spacing.space4, end = V5Spacing.space1)
            .testTag(V5FeedbackNoticeTestTags.Row),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (error) {
            // Heard as well as seen: «Error» before the message, whatever its words (СС-4).
            Icon(
                VolaIcons.Error,
                contentDescription = stringResource(R.string.v5_feedback_error),
                tint = colors.danger,
                modifier = Modifier.size(V5Sizes.iconSmall),
            )
        }
        Text(
            visuals.message,
            style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
            color = colors.label,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = V5Spacing.space2),
        )
        visuals.actionLabel?.let { label ->
            TextButton(onClick = data::performAction, modifier = Modifier.testTag(V5FeedbackNoticeTestTags.Action)) {
                Text(label, style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = colors.accent)
            }
        }
        if (visuals.withDismissAction) {
            TextButton(onClick = data::dismiss, modifier = Modifier.testTag(V5FeedbackNoticeTestTags.Close)) {
                Icon(
                    VolaIcons.Close,
                    contentDescription = stringResource(R.string.v5_sheet_close),
                    tint = colors.labelSecondary,
                    modifier = Modifier.size(V5Sizes.iconSmall),
                )
            }
        }
    }
}
