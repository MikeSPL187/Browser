package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import dev.sk2andy.materialbrowser.shared.topping.ToppingFrameScope
import dev.sk2andy.materialbrowser.ui.theme.MaterialBrowserTheme
import dev.sk2andy.materialbrowser.ui.theme.VolaPreviews

/* Б12a: Toppings under ui.v5. Preview-only sample names and sources are not localized. */

private val previewScripts = listOf(
    UserscriptUiItem(
        id = "wide-reader",
        name = "Wide reader",
        source = userscriptTemplate("Wide reader"),
        enabled = true,
        runAtLabel = "document end",
        urlPatterns = listOf("https://news.example/*", "https://*.wiki.example/*"),
        declaredFrameScope = ToppingFrameScope.AllMatching,
        allowedFrameScope = ToppingFrameScope.SameOrigin,
    ),
    UserscriptUiItem(
        id = "quiet-comments",
        name = "Quiet comments",
        source = userscriptTemplate("Quiet comments"),
        enabled = false,
        runAtLabel = "document start",
        urlPatterns = listOf("https://video.example/watch*"),
    ),
)

@Composable
private fun ToppingsPreview(
    scripts: List<UserscriptUiItem> = previewScripts,
    isRuntimeSupported: Boolean = true,
    openId: String? = null,
    editor: UserscriptEditorRequest? = null,
) {
    MaterialBrowserTheme {
        UserscriptManagementV5(
            scripts = scripts,
            isRuntimeSupported = isRuntimeSupported,
            onToggle = { _, _, _ -> },
            onSetFrameScope = { _, _, _ -> },
            onSave = { _, _, _ -> },
            onDelete = { _, _ -> },
            onImport = {},
            onDiscover = {},
            onDismiss = {},
            initialOpenId = openId,
            initialEditor = editor,
        )
    }
}

/** The catalog, a new script and import, then the saved scripts with whether each is on. */
@VolaPreviews
@Composable
private fun ToppingsListV5Preview() {
    ToppingsPreview()
}

/** No scripts yet, and the Gecko host could not start. */
@VolaPreviews
@Composable
private fun ToppingsEmptyV5Preview() {
    ToppingsPreview(scripts = emptyList(), isRuntimeSupported = false)
}

/** One script's page: the switch, its addresses, embedded-frame access, edit and delete. */
@VolaPreviews
@Composable
private fun ToppingPageV5Preview() {
    ToppingsPreview(openId = "wide-reader")
}

/** The source editor over the list. */
@VolaPreviews
@Composable
private fun ToppingEditorV5Preview() {
    ToppingsPreview(editor = UserscriptEditorRequest(id = null, source = userscriptTemplate("New Topping")))
}
