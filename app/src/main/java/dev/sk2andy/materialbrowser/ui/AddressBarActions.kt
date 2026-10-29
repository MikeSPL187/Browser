package dev.sk2andy.materialbrowser.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.data.AddressBarAction
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons

@Composable
internal fun AddressBarActionGlyph(
    action: AddressBarAction,
    tabCount: Int = 1,
    selected: Boolean,
    isLoading: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val contentColor = LocalContentColor.current
    val accentColor = LocalCandyChromeAccentColor.current.let { color ->
        if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    }
    val tint = when {
        !enabled -> contentColor.copy(alpha = 0.38f)
        selected -> accentColor
        else -> contentColor.copy(alpha = 0.78f)
    }
    when (action) {
        AddressBarAction.Tabs -> AddressBarTabCounterGlyph(
            tabCount = tabCount,
            modifier = modifier,
        )
        AddressBarAction.FindInPage -> Icon(
            VolaIcons.Search,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
        AddressBarAction.NewTab -> Icon(
            VolaIcons.Add,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
        AddressBarAction.CloseTab -> Icon(
            VolaIcons.Close,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
        AddressBarAction.Home -> Icon(
            VolaIcons.Home,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
        else -> Icon(
            painter = painterResource(
                when (action) {
                    AddressBarAction.Favorite -> if (selected) {
                        R.drawable.ic_symbol_favorite_filled
                    } else {
                        R.drawable.ic_symbol_favorite
                    }
                    AddressBarAction.Pin -> R.drawable.ic_push_pin
                    AddressBarAction.Desktop -> R.drawable.ic_symbol_desktop
                    AddressBarAction.ForceVerticalScroll ->
                        R.drawable.ic_symbol_vertical_scroll
                    AddressBarAction.Reader -> R.drawable.ic_reader_align_start
                    AddressBarAction.Share -> R.drawable.ic_symbol_share
                    AddressBarAction.Print -> R.drawable.ic_symbol_print
                    AddressBarAction.Reload -> if (isLoading) {
                        R.drawable.ic_symbol_close
                    } else {
                        R.drawable.ic_symbol_refresh
                    }
                    AddressBarAction.Back -> R.drawable.ic_symbol_arrow_back
                    AddressBarAction.Forward -> R.drawable.ic_symbol_arrow_forward
                    AddressBarAction.ParkRight ->
                        R.drawable.ic_symbol_chevron_physical_right
                    AddressBarAction.FindInPage,
                    AddressBarAction.NewTab,
                    AddressBarAction.CloseTab,
                    AddressBarAction.Home,
                    AddressBarAction.Tabs,
                    -> error("Vector icon handled above")
                },
            ),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
    }
}

@Composable
internal fun AddressBarTabCounterGlyph(
    tabCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(25.dp)
            .border(
                width = 2.dp,
                color = LocalContentColor.current,
                shape = RoundedCornerShape(6.dp),
            )
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = AddressBarControlRules.tabCountLabel(tabCount),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@StringRes
internal fun AddressBarAction.labelRes(): Int = when (this) {
    AddressBarAction.Favorite -> R.string.action_favorite
    AddressBarAction.Pin -> R.string.action_pin_tab
    AddressBarAction.Desktop -> R.string.action_desktop_view
    AddressBarAction.ForceVerticalScroll -> R.string.privacy_force_vertical_scrolling
    AddressBarAction.Reader -> R.string.reader_open_action
    AddressBarAction.FindInPage -> R.string.action_find_in_page
    AddressBarAction.Tabs -> R.string.address_bar_action_tabs
    AddressBarAction.Share -> R.string.action_share
    AddressBarAction.Print -> R.string.action_print
    AddressBarAction.NewTab -> R.string.cd_new_tab
    AddressBarAction.Home -> R.string.action_home
    AddressBarAction.Reload -> R.string.action_reload
    AddressBarAction.CloseTab -> R.string.cd_close_tab
    AddressBarAction.Back -> R.string.action_back
    AddressBarAction.Forward -> R.string.action_forward
    AddressBarAction.ParkRight -> R.string.action_park_address_pill_right
}
