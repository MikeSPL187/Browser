package dev.sk2andy.materialbrowser.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.sk2andy.materialbrowser.shared.ui.icons.VolaIcons
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Sizes
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Spacing
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Theme
import dev.sk2andy.materialbrowser.ui.theme.v5.VolaV5Theme
import dev.sk2andy.materialbrowser.ui.v5.V5StateGlyph

internal object AppDataTransferStatusTestTags {
    const val Screen = "app_data_transfer_status"
}

/** What the transfer window says: the work under way, or that recovery failed. */
internal data class AppDataTransferStatusState(
    @StringRes val message: Int,
    val export: Boolean,
    val failed: Boolean = false,
)

/**
 * The window that exports or restores Vola's data while the browser is stopped. Under v5 on the
 * phone it is a v5 state screen: the export or restore glyph, what is happening and a spinner, or a
 * red mark when recovery failed; nothing on it can be pressed, as the transfer must not be cut
 * short. Elsewhere (the tablet, v4) it stays the plain line it was.
 */
@Composable
internal fun AppDataTransferStatus(state: AppDataTransferStatusState) {
    AppDataTransferStatusV5(state)
}

@Composable
internal fun AppDataTransferStatusV5(state: AppDataTransferStatusState) {
    // The transfer runs in its own process with no browser around it: the system decides light or dark.
    VolaV5Theme {
        val colors = V5Theme.colors
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .safeDrawingPadding()
                .testTag(AppDataTransferStatusTestTags.Screen),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .widthIn(max = V5Sizes.stateContentMax)
                    .fillMaxWidth()
                    .padding(horizontal = V5Spacing.space6, vertical = V5Spacing.space4),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(V5Spacing.space4),
            ) {
                V5StateGlyph(
                    icon = when {
                        state.failed -> VolaIcons.WarningFilled
                        state.export -> VolaIcons.UploadFile
                        else -> VolaIcons.Download
                    },
                    tint = if (state.failed) colors.danger else colors.accent,
                )
                Text(
                    stringResource(state.message),
                    style = if (state.failed) V5Theme.type.callout else V5Theme.type.headline,
                    color = if (state.failed) colors.labelSecondary else colors.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (!state.failed) {
                    CircularProgressIndicator(
                        color = colors.accent,
                        strokeWidth = V5Sizes.barProgress,
                        trackColor = colors.fill,
                        modifier = Modifier.size(V5Sizes.iconSmall),
                    )
                }
            }
        }
    }
}
