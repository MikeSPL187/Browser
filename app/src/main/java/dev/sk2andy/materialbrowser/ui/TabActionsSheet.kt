package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.sk2andy.materialbrowser.R

private fun TabQuickAction.labelRes(): Int = when (this) {
    TabQuickAction.AddToEssentials -> R.string.tab_actions_add_essential
    TabQuickAction.RemoveFromEssentials -> R.string.tab_actions_remove_essential
    TabQuickAction.Duplicate -> R.string.tab_actions_duplicate
    TabQuickAction.Pin -> R.string.tab_actions_pin
    TabQuickAction.Unpin -> R.string.action_remove_pin
    TabQuickAction.Share -> R.string.tab_actions_share
}

private fun TabMoreAction.labelRes(): Int = when (this) {
    TabMoreAction.SideBySide -> R.string.split_view_open_side_by_side
    TabMoreAction.AddBookmark -> R.string.action_add_favorite
    TabMoreAction.RemoveBookmark -> R.string.action_remove_favorite
    TabMoreAction.MuteSite -> R.string.action_mute_domain
    TabMoreAction.UnmuteSite -> R.string.tab_actions_unmute_site
    TabMoreAction.OpenInApp -> R.string.action_open_in_app
    TabMoreAction.Print -> R.string.action_print
    TabMoreAction.Summarize -> R.string.action_summarize
    TabMoreAction.CandyTrail -> R.string.action_open_candy_trail
    TabMoreAction.SiteCapsule -> R.string.action_add_site_capsule
    TabMoreAction.CloseAll -> R.string.action_close_all_tabs
}
