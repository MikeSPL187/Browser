package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5ColorScheme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Colors
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Tile
import java.net.URI
/**
 * Warning page for navigations that HTTPS-only mode stopped because the site has no working HTTPS.
 *
 * GeckoView renders the page returned from NavigationDelegate.onLoadError as an error document and
 * exposes document.reloadWithHttpsOnlyException() to it, which reloads the request over HTTP for
 * this page only. That call is the page's only privileged action; everything shown is escaped.
 *
 * The page follows canvas «HttpsOnly» in the v5 colors: the warning near the middle, «Go back»
 * under the thumb and «Open over HTTP anyway» quieter below it.
 */
internal class GeckoHttpsOnlyErrorPages(private val context: Context) {
    fun dataUri(failedUri: String?): String {
        val host = GeckoHttpsOnlyErrorPage.displayHost(failedUri)
        val strings = GeckoHttpsOnlyErrorPage.Strings(
            languageTag = context.resources.configuration.locales[0].toLanguageTag(),
            title = context.getString(R.string.https_only_error_title),
            body = context.getString(
                R.string.https_only_error_body,
                GeckoHttpsOnlyErrorPage.HOST_MARKER,
            ),
            advice = context.getString(R.string.https_only_error_advice),
            continueLabel = context.getString(R.string.https_only_error_continue),
            backLabel = context.getString(R.string.https_only_error_back),
            adviceTitle = context.getString(R.string.https_only_error_advice_title),
            exceptionNote = context.getString(R.string.https_only_error_exception, GeckoHttpsOnlyErrorPage.HOST_MARKER),
        )
        return GeckoHttpsOnlyErrorPage.dataUri(GeckoHttpsOnlyErrorPage.htmlV5(strings, host))
    }
}

internal object GeckoHttpsOnlyErrorPage {
    /** Stands in for the host in the localized body, so the host can be escaped and emphasized. */
    const val HOST_MARKER = "\u2063host\u2063"

    data class Strings(
        val languageTag: String,
        val title: String,
        val body: String,
        val advice: String,
        val continueLabel: String,
        val backLabel: String,
        /** The v5 card's heading over [advice]. */
        val adviceTitle: String = "",
        /** The v5 line under the actions: the exception is for this site only ([HOST_MARKER]). */
        val exceptionNote: String = "",
    )

    /** The v5 roles the page uses (system-v5.md 1.1, 1.5), as CSS colors, for one theme. */
    data class PaletteV5(
        val background: String,
        val surface: String,
        val label: String,
        val labelSecondary: String,
        val fill: String,
        val accentFill: String,
        val onAccentFill: String,
        val badge: String,
        val safe: String,
    ) {
        companion object {
            fun from(scheme: V5ColorScheme) = PaletteV5(
                background = css(scheme.background),
                surface = css(scheme.surface),
                label = css(scheme.label),
                labelSecondary = css(scheme.labelSecondary),
                fill = css(scheme.fill),
                accentFill = css(scheme.accentFill),
                onAccentFill = css(scheme.onAccentFill),
                // The section tiles of system-v5.md 1.5: blue for the connection, green for what is safe.
                badge = css(V5Tile.Blue.color),
                safe = css(V5Tile.Green.color),
            )

            /** `#RRGGBBAA`, so translucent roles such as `fill` keep their alpha. */
            private fun css(color: Color): String {
                val argb = color.toArgb()
                return "#%06X%02X".format(argb and 0xFFFFFF, (argb ushr 24) and 0xFF)
            }
        }
    }

    /** The page is not tied to a profile, so it takes the default accent. */
    val defaultPalettesV5: Pair<PaletteV5, PaletteV5> =
        PaletteV5.from(V5Colors.light(V5Accent.Default)) to PaletteV5.from(V5Colors.dark(V5Accent.Default))

    /** The ASCII (punycode) host keeps look-alike Unicode domains recognizable. */
    fun displayHost(failedUri: String?): String {
        if (failedUri.isNullOrBlank()) return ""
        return runCatching { URI(failedUri).host }.getOrNull()
            ?.takeIf(String::isNotBlank)
            ?: failedUri
    }

    // Material Symbols Rounded (Apache License 2.0): no_encryption (filled).
    private const val NO_ENCRYPTION_ICON =
        "<svg viewBox=\"0 -960 960 960\" width=\"44\" height=\"44\" fill=\"currentColor\">" +
            "<path d=\"M775-334q-11 5-22 3.5T732-342L502-572q-10-10-11.5-21t3.5-22q5-11 14-18t23-7" +
            "h69v-80q0-50-34.5-85T481-840q-35 0-63 16.5T379-779q-7 17-22.5 23.5t-31.5.5q-17-6-24" +
            "-21.5t-1-31.5q20-51 69-81.5T481-920q83 0 141 58.5T680-720v80h40q33 0 56.5 23.5T800-" +
            "560v189q0 14-7 23t-18 14ZM240-80q-33 0-56.5-23.5T160-160v-400q0-26 14.5-46.5T212-" +
            "636L56-792q-11-11-11-28t11-28q11-11 28-11t28 11l736 736q11 11 11 28t-11 28q-11 11-" +
            "28 11t-28-11l-34-34q-9 5-18 7.5T720-80H240Zm184-342q-11 11-17 25.5t-6 31.5q0 33 " +
            "23.5 56.5T481-285q17 0 31.5-6t25.5-17L424-422Z\"/></svg>"

    // Material Symbols Rounded (Apache License 2.0): visibility.
    private const val VISIBILITY_ICON =
        "<svg viewBox=\"0 -960 960 960\" width=\"18\" height=\"18\" fill=\"currentColor\">" +
            "<path d=\"M480-320q75 0 127.5-52.5T660-500q0-75-52.5-127.5T480-680q-75 0-127.5 52.5T300-500" +
            "q0 75 52.5 127.5T480-320Zm0-72q-45 0-76.5-31.5T372-500q0-45 31.5-76.5T480-608q45 0 76.5 " +
            "31.5T588-500q0 45-31.5 76.5T480-392Zm0 192q-134 0-244.5-72T61-462q-5-9-7.5-18.5T51-500q0-10 " +
            "2.5-19.5T61-538q64-118 174.5-190T480-800q134 0 244.5 72T899-538q5 9 7.5 18.5T909-500q0 10-2.5 " +
            "19.5T899-462q-64 118-174.5 190T480-200Zm0-300Zm0 220q113 0 207.5-59.5T832-500q-50-101-144.5-" +
            "160.5T480-720q-113 0-207.5 59.5T128-500q50 101 144.5 160.5T480-280Z\"/></svg>"

    // Sizes follow the v5 tokens (system-v5.md 2, 3, 5): 16 margins, largeTitle 34/41, body 17/24,
    // callout 15/20, footnote 13/18, card radius 16, capsules 52 and 48 high, Inter where installed.
    private val STYLE_V5 = """
        * { box-sizing: border-box; }
        html, body { margin: 0; min-height: 100%; background: var(--bg); }
        body {
          color: var(--label);
          font: 400 17px/24px Inter, system-ui, -apple-system, "Roboto", sans-serif;
          letter-spacing: -0.013em;
        }
        main {
          width: 100%; max-width: 560px; margin: 0 auto; min-height: 100vh;
          display: flex; flex-direction: column; padding: 72px 24px 24px;
        }
        .badge {
          width: 72px; height: 72px; border-radius: 24px;
          display: flex; align-items: center; justify-content: center;
          background: var(--badge); color: #FFFFFF;
        }
        .badge svg { width: 40px; height: 40px; }
        h1 {
          font-size: 34px; line-height: 41px; font-weight: 700; letter-spacing: -0.022em;
          margin: 28px 0 0;
        }
        p { margin: 12px 0 0; color: var(--label2); overflow-wrap: anywhere; }
        strong { color: var(--label); font-weight: 600; }
        .card {
          display: flex; gap: 12px; margin-top: 20px; padding: 14px 16px;
          border-radius: 16px; background: var(--sf);
          font-size: 13px; line-height: 18px; color: var(--label2);
        }
        .card b { display: block; font-size: 15px; line-height: 20px; font-weight: 600; color: var(--label); }
        .tile {
          flex: none; width: 30px; height: 30px; border-radius: 8px;
          display: flex; align-items: center; justify-content: center;
          background: var(--safe); color: #FFFFFF;
        }
        .actions { display: flex; flex-direction: column; gap: 8px; margin-top: auto; padding-top: 28px; }
        button {
          font: inherit; font-weight: 600; min-height: 52px; padding: 0 24px;
          border: 0; border-radius: 26px; cursor: pointer;
        }
        .primary { background: var(--acc); color: var(--on-acc); }
        .plate { min-height: 48px; font-weight: 500; background: var(--fill); color: var(--label); }
        .note { margin: 4px 0 0; text-align: center; font-size: 13px; line-height: 18px; }
    """.trimIndent()

    private val SCRIPT = """
        const back = document.getElementById("back");
        if (history.length < 2) back.hidden = true;
        back.addEventListener("click", () => history.back());
        document.getElementById("continue").addEventListener("click", () => {
          document.reloadWithHttpsOnlyException();
        });
    """.trimIndent()
}
