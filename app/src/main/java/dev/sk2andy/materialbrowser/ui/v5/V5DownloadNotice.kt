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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

internal object V5DownloadNoticeTestTags {
    const val Row = "v5_download_notice"
    const val Action = "v5_download_notice_action"
}

/** A download that has just ended (V22b): what the line above the bar says. */
@Immutable
internal sealed interface V5DownloadNotice {
    val id: Long
    val name: String

    data class Done(override val id: Long, override val name: String) : V5DownloadNotice

    data class Failed(override val id: Long, override val name: String) : V5DownloadNotice
}

/**
 * «Downloaded: file — Open» or «Couldn't download: file — Downloads» (V22b, system-v5.md
 * section 10): a line above the bar in the bar's colors, said by TalkBack when it appears.
 */
@Composable
internal fun V5DownloadNoticeRow(
    notice: V5DownloadNotice,
    onOpen: () -> Unit,
    onDownloads: () -> Unit,
    modifier: Modifier = Modifier,
    /** In a private tab: the file is not private, it lies in the shared Downloads folder. */
    privateTab: Boolean = false,
) {
    val failed = notice is V5DownloadNotice.Failed
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.touchTarget)
            .padding(start = V5Spacing.space4, end = V5Spacing.space1)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(V5DownloadNoticeTestTags.Row),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (failed) {
            Icon(VolaIcons.Error, contentDescription = null, tint = V5Theme.colors.danger)
        } else {
            Icon(VolaIcons.CheckCircle, contentDescription = null)
        }
        Text(
            text = stringResource(
                when {
                    failed -> R.string.v5_download_failed
                    privateTab -> R.string.v5_download_done_private
                    else -> R.string.v5_download_done
                },
                notice.name,
            ),
            style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = if (failed) onDownloads else onOpen,
            modifier = Modifier.testTag(V5DownloadNoticeTestTags.Action),
        ) {
            Text(
                stringResource(if (failed) R.string.v5_menu_downloads else R.string.v5_download_open),
                style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

/** [V5DownloadNoticeRow] that unfolds above the bar as the listening row does. */
@Composable
internal fun V5DownloadNoticeAccessory(
    notice: V5DownloadNotice?,
    onOpen: (V5DownloadNotice) -> Unit,
    onDownloads: () -> Unit,
    privateTab: Boolean = false,
) {
    val last = remember { LastNotice() }
    if (notice != null) last.notice = notice
    val animated = V5Motion.animationsEnabled
    AnimatedVisibility(
        visible = notice != null,
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
        last.notice?.let { shown ->
            V5DownloadNoticeRow(
                notice = shown,
                onOpen = { onOpen(shown) },
                onDownloads = onDownloads,
                privateTab = privateTab,
            )
        }
    }
}

private class LastNotice {
    var notice: V5DownloadNotice? = null
}
