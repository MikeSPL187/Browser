package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * How every v5 sheet is shown: a [V5Sheet] in a window of its own over the whole app, so the sheet
 * and its scrim cover the bar too and what lies under it is out of reach of touch and of TalkBack,
 * as under a sheet on iOS. Drawn inside the browser, the bar and the new tab under the menu stayed
 * in TalkBack's order and overlapped the sheet's rows (the v5 screen walk, 15.10). The window stays
 * until the sheet has left, so it goes on the same spring it came in on. Previews show the sheet in
 * place.
 */
@Composable
fun V5WindowSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalInspectionMode.current) {
        V5Sheet(visible = visible, onDismiss = onDismiss, title = title, modifier = modifier, content = content)
        return
    }
    var leaving by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (visible) leaving = true }
    if (!visible && !leaving) return
    Dialog(
        onDismissRequest = onDismiss,
        // The sheet handles back itself (predictive back lowers it first) and brings its own scrim.
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setDimAmount(0f)
            window?.setWindowAnimations(0)
        }
        V5Sheet(
            visible = visible,
            onDismiss = onDismiss,
            title = title,
            modifier = modifier,
            onHidden = { leaving = false },
            content = content,
        )
    }
}
