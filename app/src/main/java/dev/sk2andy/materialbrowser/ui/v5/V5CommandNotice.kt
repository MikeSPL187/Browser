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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme

internal object V5CommandNoticeTestTags {
    const val Row = "v5_command_notice"
}

/** What an address bar command answered (V28, B6b): done, or not understood. */
@Immutable
internal data class V5CommandNotice(val text: String, val rejected: Boolean)

/**
 * «Cache cleared» or «No such command»: a line above the bar in the bar's colors, said by TalkBack
 * when it appears, like the download line. The bar stays as it is.
 */
@Composable
internal fun V5CommandNoticeRow(notice: V5CommandNotice, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = V5Sizes.touchTarget)
            .padding(horizontal = V5Spacing.space4)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(V5CommandNoticeTestTags.Row),
        horizontalArrangement = Arrangement.spacedBy(V5Spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (notice.rejected) {
            Icon(VolaIcons.Error, contentDescription = null, tint = V5Theme.colors.danger)
        } else {
            Icon(VolaIcons.CheckCircle, contentDescription = null)
        }
        Text(
            text = notice.text,
            style = V5Theme.type.footnote.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** [V5CommandNoticeRow] that unfolds above the bar as the download line does. */
@Composable
internal fun V5CommandNoticeAccessory(notice: V5CommandNotice?) {
    val last = remember { LastCommandNotice() }
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
        last.notice?.let { shown -> V5CommandNoticeRow(shown) }
    }
}

private class LastCommandNotice {
    var notice: V5CommandNotice? = null
}
