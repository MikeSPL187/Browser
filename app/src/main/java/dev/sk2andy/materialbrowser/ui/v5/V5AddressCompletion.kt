package dev.sk2andy.materialbrowser.ui.v5

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import dev.sk2andy.materialbrowser.browser.AddressResolver
import dev.sk2andy.materialbrowser.browser.commands.AddressSuggestionItem

/*
 * Address completion in the v5 editor (canvas «Address»): while a site's name is typed, the rest of
 * the best match's address shows after the cursor, on the accent's soft fill, and Go opens it.
 * The rest is drawn, not typed into the field: the keyboard keeps composing only what was typed,
 * so a completion never fights it, and the next letter or Delete simply moves on from there.
 */
object V5AddressCompletionRules {
    /**
     * The rest of the best match's site after [value], or null. Only while typing forward at the
     * end of the text (not after Delete, [deleting]), only a site name (no spaces, no path), and
     * only a page from the library or open tabs: a search or a command is never completed.
     */
    fun suffix(value: TextFieldValue, best: AddressSuggestionItem?, deleting: Boolean): String? {
        if (deleting) return null
        val typed = value.text
        if (typed.isEmpty() || !value.selection.collapsed || value.selection.end != typed.length) return null
        if (typed.any { it.isWhitespace() || it == '/' }) return null
        val url = (best as? AddressSuggestionItem.Navigation)?.suggestion?.url ?: return null
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        val site = AddressResolver.displayText(url)
        if (site.length <= typed.length || !site.startsWith(typed, ignoreCase = true)) return null
        return site.substring(typed.length)
    }

    /** What Go opens: the typed text with the completion, if one shows. */
    fun submitted(typed: String, suffix: String?): String = typed + suffix.orEmpty()
}

/** Draws [suffix] after the typed text in [style]; the cursor and taps stay within the typed text. */
internal class V5CompletionTransformation(
    private val suffix: String,
    private val style: SpanStyle,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val typed = text.length
        val shown = buildAnnotatedString {
            append(text)
            withStyle(style) { append(suffix) }
        }
        return TransformedText(
            shown,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset
                override fun transformedToOriginal(offset: Int): Int = offset.coerceAtMost(typed)
            },
        )
    }

    override fun equals(other: Any?): Boolean =
        other is V5CompletionTransformation && other.suffix == suffix && other.style == style

    override fun hashCode(): Int = 31 * suffix.hashCode() + style.hashCode()
}
