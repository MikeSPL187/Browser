package dev.sk2andy.materialbrowser.ui

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.SystemClock
import android.view.textclassifier.TextClassifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.sk2andy.materialbrowser.browser.ClipboardDescription
import dev.sk2andy.materialbrowser.browser.ClipboardOffer
import dev.sk2andy.materialbrowser.browser.ClipboardOfferRules

/** What the clipboard chip offers, and the clip it belongs to. */
@Immutable
internal data class AddressClipboardOffer(
    val offer: ClipboardOffer,
    val copiedAtElapsedMillis: Long,
)

/**
 * The clipboard chip's offer while [enabled]. Only the clip's description is read here, never the
 * clip: Android 12 and later show a "pasted from your clipboard" notice for every read, so the
 * text is read in [readClipboardText] when the user taps the chip.
 */
@Composable
internal fun rememberAddressClipboardOffer(
    enabled: Boolean,
    usedCopiedAtElapsedMillis: Long?,
): AddressClipboardOffer? {
    val context = LocalContext.current
    val clipboard = remember(context) { context.getSystemService(ClipboardManager::class.java) }
    var description by remember { mutableStateOf<ClipboardDescription?>(null) }
    DisposableEffect(enabled, clipboard) {
        if (!enabled || clipboard == null) {
            description = null
            return@DisposableEffect onDispose {}
        }
        description = clipboard.readDescription()
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            description = clipboard.readDescription()
        }
        clipboard.addPrimaryClipChangedListener(listener)
        onDispose { clipboard.removePrimaryClipChangedListener(listener) }
    }
    val current = description ?: return null
    val offer = ClipboardOfferRules.offer(
        description = current,
        nowElapsedMillis = SystemClock.elapsedRealtime(),
        usedCopiedAtElapsedMillis = usedCopiedAtElapsedMillis,
    ) ?: return null
    return AddressClipboardOffer(offer, current.copiedAtElapsedMillis)
}

/** Reads the clip's text. Call it only from the user's tap on the chip. */
internal fun readClipboardText(context: Context): String? {
    val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}

private fun ClipboardManager.readDescription(): ClipboardDescription? {
    val description = runCatching { primaryClipDescription }.getOrNull() ?: return null
    val hasUriList = description.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST)
    val hasText = hasUriList ||
        description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
        description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
    val linkConfidence = when {
        hasUriList -> 1f
        description.classificationStatus == ClipDescription.CLASSIFICATION_COMPLETE ->
            runCatching { description.getConfidenceScore(TextClassifier.TYPE_URL) }.getOrNull()
        else -> null
    }
    return ClipboardDescription(
        hasText = hasText,
        linkConfidence = linkConfidence,
        copiedAtElapsedMillis = description.timestamp,
    )
}
