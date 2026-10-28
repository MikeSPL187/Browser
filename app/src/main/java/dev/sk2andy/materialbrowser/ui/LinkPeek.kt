package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.LinkPeekAction
internal object LinkPeekTestTags {
    const val Root = "link_peek"
    const val OpenTarget = "link_peek_open_target"
    const val Card = "link_peek_card"
    const val Preview = "link_peek_preview"
    const val Url = "link_peek_url"
    const val NewTabTargetOverlay = "link_peek_new_tab_target_overlay"
    const val NewTabTargetPulseRing = "link_peek_new_tab_target_pulse_ring"
    const val CopyLink = "link_peek_copy_link"
    const val OpenPrivate = "link_peek_open_private"
    const val Share = "link_peek_share"
    const val ReaderLater = "link_peek_reader_later"
    const val Favorite = "link_peek_favorite"
    const val Snooze = "link_peek_snooze"
    const val OpenForeground = "link_peek_open_foreground"
    const val DownloadLink = "link_peek_download_link"
    const val DownloadImage = "link_peek_download_image"
    const val PullHandle = "link_peek_pull_handle"
    const val Close = "link_peek_close"
    const val HistoryNote = "link_peek_history_note"
}

@Composable
private fun LinkPeekAction.contentDescription(
    copyLabel: String,
    openPrivateLabel: String,
    shareLabel: String,
    isFavorite: Boolean,
): String = when (this) {
    LinkPeekAction.ReaderLater -> stringResource(R.string.reader_save_offline)
    LinkPeekAction.OpenPrivate -> openPrivateLabel
    LinkPeekAction.Copy -> copyLabel
    LinkPeekAction.Share -> shareLabel
    LinkPeekAction.Favorite -> stringResource(
        if (isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite,
    )
    LinkPeekAction.Snooze -> stringResource(R.string.action_snooze_tab)
    LinkPeekAction.OpenForeground -> stringResource(R.string.action_open_in_new_tab_and_switch)
}

private fun LinkPeekAction.testTag(): String = when (this) {
    LinkPeekAction.ReaderLater -> LinkPeekTestTags.ReaderLater
    LinkPeekAction.OpenPrivate -> LinkPeekTestTags.OpenPrivate
    LinkPeekAction.Copy -> LinkPeekTestTags.CopyLink
    LinkPeekAction.Share -> LinkPeekTestTags.Share
    LinkPeekAction.Favorite -> LinkPeekTestTags.Favorite
    LinkPeekAction.Snooze -> LinkPeekTestTags.Snooze
    LinkPeekAction.OpenForeground -> LinkPeekTestTags.OpenForeground
}

