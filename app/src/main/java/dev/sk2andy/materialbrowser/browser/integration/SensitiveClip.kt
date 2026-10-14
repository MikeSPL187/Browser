package dev.sk2andy.materialbrowser.browser.integration

import android.content.ClipData
import android.os.PersistableBundle

/** `ClipDescription.EXTRA_IS_SENSITIVE`, spelled out so it also reaches Android 12. */
private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

/**
 * Marks this clip sensitive when [sensitive]: Android hides it from the clipboard preview, and
 * keyboards keep it out of their clipboard history. Used for passwords and for links copied in a
 * private tab, which must not outlive the private session there.
 */
internal fun ClipData.markedSensitive(sensitive: Boolean = true): ClipData = apply {
    if (sensitive) description.extras = PersistableBundle().apply { putBoolean(EXTRA_IS_SENSITIVE, true) }
}
