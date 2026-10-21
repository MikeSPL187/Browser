package dev.sk2andy.materialbrowser.browser.safety

import java.lang.Character.UnicodeScript
import java.net.IDN
import java.util.Locale

/**
 * How an international host name is shown to a person (П-4). A name reads in its own letters
 * (`пример.рф`, `münchen.de`) only when it cannot pass for another; otherwise it stays in
 * punycode (`xn--…`), as Safari, Chrome and Firefox do, so `аррӏе.com` in Cyrillic never shows
 * as «apple.com». Engines hand over either spelling, so both are read here.
 *
 * A label shows in letters when all of these hold:
 * - its letters come from one script (Latin, Cyrillic, Greek…), or from Chinese and Japanese or
 *   Chinese and Korean together, besides digits and `-`;
 * - it has no symbols, spaces or invisible marks;
 * - a Cyrillic or Greek label is not made only of letters that look Latin, unless the top-level
 *   domain is in that same script (`сос.рф` is fine, `сос.com` is not).
 */
object IdnDisplayRules {
    /** [host] as it should be shown: letters when safe, else punycode; unchanged when it is not a name. */
    fun displayHost(host: String): String {
        if (host.isEmpty() || host.startsWith("[")) return host
        val unicode = runCatching { IDN.toUnicode(host, IDN.ALLOW_UNASSIGNED) }.getOrNull() ?: return host
        if (unicode.all { it.code < 0x80 }) return unicode
        val labels = unicode.split('.')
        val topScript = labels.lastOrNull { it.isNotEmpty() }?.let(::singleScript)
        val safe = labels.all { label -> isSafeLabel(label, topScript) }
        if (safe) return unicode
        return runCatching { IDN.toASCII(host, IDN.ALLOW_UNASSIGNED) }.getOrNull()?.lowercase(Locale.ROOT) ?: host
    }

    private fun isSafeLabel(label: String, topScript: UnicodeScript?): Boolean {
        if (label.all { it.code < 0x80 }) return true
        val scripts = HashSet<UnicodeScript>()
        var index = 0
        while (index < label.length) {
            val codePoint = label.codePointAt(index)
            index += Character.charCount(codePoint)
            if (codePoint == '-'.code || codePoint in '0'.code..'9'.code) continue
            if (!Character.isLetterOrDigit(codePoint)) {
                // Combining marks belong to the letter before them (ё, й written in two parts).
                if (Character.getType(codePoint) != Character.NON_SPACING_MARK.toInt()) return false
                continue
            }
            val script = UnicodeScript.of(codePoint)
            if (script == UnicodeScript.COMMON || script == UnicodeScript.INHERITED) return false
            scripts += script
        }
        if (!isAllowedMix(scripts)) return false
        val script = scripts.singleOrNull() ?: return true
        if (script == UnicodeScript.CYRILLIC || script == UnicodeScript.GREEK) {
            val looksLatin = label.all { it == '-' || it.isDigit() || it in LATIN_LOOKALIKES }
            if (looksLatin && topScript != script) return false
        }
        return true
    }

    private fun isAllowedMix(scripts: Set<UnicodeScript>): Boolean {
        if (scripts.size <= 1) return true
        return scripts.all { it in CHINESE_JAPANESE } || scripts.all { it in CHINESE_KOREAN }
    }

    /** The one script of [label]'s letters, or null when it has none or several. */
    private fun singleScript(label: String): UnicodeScript? {
        var found: UnicodeScript? = null
        var index = 0
        while (index < label.length) {
            val codePoint = label.codePointAt(index)
            index += Character.charCount(codePoint)
            if (!Character.isLetter(codePoint)) continue
            val script = UnicodeScript.of(codePoint)
            if (found != null && found != script) return null
            found = script
        }
        return found
    }

    private val CHINESE_JAPANESE = setOf(UnicodeScript.HAN, UnicodeScript.HIRAGANA, UnicodeScript.KATAKANA)
    private val CHINESE_KOREAN = setOf(UnicodeScript.HAN, UnicodeScript.HANGUL)

    /** Cyrillic and Greek letters that read as a Latin one, in lower case as hosts are. */
    private val LATIN_LOOKALIKES: Set<Char> = setOf(
        // Cyrillic
        'а', 'с', 'ԁ', 'е', 'ё', 'һ', 'і', 'ї', 'ј', 'ӏ', 'о', 'р', 'ԛ', 'ѕ', 'ԝ', 'х', 'у', 'ъ',
        'ь', 'ҽ', 'п', 'г', 'ѵ', 'ѡ', 'ү',
        // Greek
        'α', 'ε', 'ι', 'κ', 'ν', 'ο', 'ρ', 'υ', 'χ',
    )
}
