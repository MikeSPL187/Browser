package dev.sk2andy.materialbrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal interface QrCodeScanner {
    fun startScan(
        onSuccess: (String) -> Unit,
        onCanceled: () -> Unit,
        onFailure: () -> Unit,
    )
}

/**
 * Vola ships without Google Play services, so there is no system code scanner to delegate to.
 * The address bar hides its scan action; this fallback only reports failure if it is reached.
 */
@Composable
internal fun rememberQrCodeScanner(): QrCodeScanner = remember {
    object : QrCodeScanner {
        override fun startScan(
            onSuccess: (String) -> Unit,
            onCanceled: () -> Unit,
            onFailure: () -> Unit,
        ) = onFailure()
    }
}
