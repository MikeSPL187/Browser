package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.WorkspaceAccent
import dev.sk2andy.materialbrowser.ui.theme.VolaSchemeTokens
import dev.sk2andy.materialbrowser.ui.theme.VolaSchemes
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Warning page for navigations that HTTPS-only mode stopped because the site has no working HTTPS.
 *
 * GeckoView renders the page returned from NavigationDelegate.onLoadError as an error document and
 * exposes document.reloadWithHttpsOnlyException() to it, which reloads the request over HTTP for
 * this page only. That call is the page's only privileged action; everything shown is escaped.
 *
 * The page follows board W-HttpsOnly: the warning near the middle, «Go back» under the thumb and
 * «Open over HTTP anyway» quieter below it, in the v4 colors of the default workspace.
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
        )
        return GeckoHttpsOnlyErrorPage.dataUri(GeckoHttpsOnlyErrorPage.html(strings, host))
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
    )

    /** The few v4 color roles the page uses, as CSS colors, for one theme. */
    data class Palette(
        val surface: String,
        val onSurface: String,
        val onSurfaceVariant: String,
        val primary: String,
        val onPrimary: String,
        val warnContainer: String,
        val onWarnContainer: String,
    ) {
        companion object {
            fun from(scheme: VolaSchemeTokens) = Palette(
                surface = css(scheme.surface),
                onSurface = css(scheme.onSurface),
                onSurfaceVariant = css(scheme.onSurfaceVariant),
                primary = css(scheme.primary),
                onPrimary = css(scheme.onPrimary),
                warnContainer = css(scheme.warnContainer),
                onWarnContainer = css(scheme.onWarnContainer),
            )

            private fun css(argb: Long): String = "#%06X".format(argb and 0xFFFFFF)
        }
    }

    /** The page is not tied to a workspace, so it takes the default workspace's colors. */
    val defaultPalettes: Pair<Palette, Palette> =
        VolaSchemes.forAccent(WorkspaceAccent.Default).let { set ->
            Palette.from(set.light) to Palette.from(set.dark)
        }

    /** The ASCII (punycode) host keeps look-alike Unicode domains recognizable. */
    fun displayHost(failedUri: String?): String {
        if (failedUri.isNullOrBlank()) return ""
        return runCatching { URI(failedUri).host }.getOrNull()
            ?.takeIf(String::isNotBlank)
            ?: failedUri
    }

    fun html(
        strings: Strings,
        host: String,
        palettes: Pair<Palette, Palette> = defaultPalettes,
    ): String {
        val body = escape(strings.body).replace(
            escape(HOST_MARKER),
            "<strong>${escape(host)}</strong>",
        )
        return listOf(
            "<!DOCTYPE html>",
            "<html lang=\"${escape(strings.languageTag)}\">",
            "<head>",
            "<meta charset=\"utf-8\">",
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">",
            "<meta name=\"color-scheme\" content=\"light dark\">",
            "<title>${escape(strings.title)}</title>",
            "<style>${colors(palettes)}\n$STYLE</style>",
            "</head>",
            "<body>",
            "<main>",
            "<div class=\"badge\" aria-hidden=\"true\">$NO_ENCRYPTION_ICON</div>",
            "<h1>${escape(strings.title)}</h1>",
            "<p>$body</p>",
            "<p class=\"callout\"><span aria-hidden=\"true\">$WARNING_ICON</span>" +
                "<span>${escape(strings.advice)}</span></p>",
            "</main>",
            "<div class=\"actions\">",
            "<button id=\"back\" class=\"primary\">${escape(strings.backLabel)}</button>",
            "<button id=\"continue\" class=\"quiet\">${escape(strings.continueLabel)}</button>",
            "</div>",
            "<script>$SCRIPT</script>",
            "</body>",
            "</html>",
        ).joinToString(separator = "\n")
    }

    fun dataUri(html: String): String =
        "data:text/html;charset=utf-8;base64," +
            Base64.getEncoder().encodeToString(html.toByteArray(StandardCharsets.UTF_8))

    fun escape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(character)
            }
        }
    }

    private fun colors(palettes: Pair<Palette, Palette>): String {
        fun Palette.variables() = "--sf: $surface; --on-sf: $onSurface; " +
            "--on-sf-v: $onSurfaceVariant; --pri: $primary; --on-pri: $onPrimary; " +
            "--warn-c: $warnContainer; --on-warn-c: $onWarnContainer;"
        return ":root { ${palettes.first.variables()} }\n" +
            "@media (prefers-color-scheme: dark) { :root { ${palettes.second.variables()} } }"
    }

    // Material Symbols Rounded (Apache License 2.0): no_encryption (filled) and warning (filled).
    private const val NO_ENCRYPTION_ICON =
        "<svg viewBox=\"0 -960 960 960\" width=\"44\" height=\"44\" fill=\"currentColor\">" +
            "<path d=\"M775-334q-11 5-22 3.5T732-342L502-572q-10-10-11.5-21t3.5-22q5-11 14-18t23-7" +
            "h69v-80q0-50-34.5-85T481-840q-35 0-63 16.5T379-779q-7 17-22.5 23.5t-31.5.5q-17-6-24" +
            "-21.5t-1-31.5q20-51 69-81.5T481-920q83 0 141 58.5T680-720v80h40q33 0 56.5 23.5T800-" +
            "560v189q0 14-7 23t-18 14ZM240-80q-33 0-56.5-23.5T160-160v-400q0-26 14.5-46.5T212-" +
            "636L56-792q-11-11-11-28t11-28q11-11 28-11t28 11l736 736q11 11 11 28t-11 28q-11 11-" +
            "28 11t-28-11l-34-34q-9 5-18 7.5T720-80H240Zm184-342q-11 11-17 25.5t-6 31.5q0 33 " +
            "23.5 56.5T481-285q17 0 31.5-6t25.5-17L424-422Z\"/></svg>"

    private const val WARNING_ICON =
        "<svg viewBox=\"0 -960 960 960\" width=\"20\" height=\"20\" fill=\"currentColor\">" +
            "<path d=\"M109-120q-11 0-20-5.5T75-140q-5-9-5.5-19.5T75-180l370-640q6-10 15.5-15t19.5" +
            "-5q10 0 19.5 5t15.5 15l370 640q6 10 5.5 20.5T885-140q-5 9-14 14.5t-20 5.5H109Zm371-" +
            "120q17 0 28.5-11.5T520-280q0-17-11.5-28.5T480-320q-17 0-28.5 11.5T440-280q0 17 11.5" +
            " 28.5T480-240Zm0-120q17 0 28.5-11.5T520-400v-120q0-17-11.5-28.5T480-560q-17 0-28.5 " +
            "11.5T440-520v120q0 17 11.5 28.5T480-360Z\"/></svg>"

    // Sizes follow VolaStatePage tokens and the type scale (board W-HttpsOnly).
    private val STYLE = """
        * { box-sizing: border-box; }
        html, body { margin: 0; min-height: 100%; background: var(--sf); }
        body {
          color: var(--on-sf);
          font: 500 15px/22px system-ui, -apple-system, "Roboto", sans-serif;
          display: flex; flex-direction: column; min-height: 100vh;
        }
        main, .actions { width: 100%; max-width: 560px; margin: 0 auto; }
        main {
          flex: 1; display: flex; flex-direction: column; align-items: center;
          justify-content: center; gap: 14px; padding: 32px 28px; text-align: center;
        }
        .badge {
          width: 84px; height: 84px; border-radius: 30px; margin-bottom: 6px;
          display: flex; align-items: center; justify-content: center;
          background: var(--warn-c); color: var(--on-warn-c);
        }
        h1 {
          font-size: 28px; line-height: 34px; font-weight: 700; letter-spacing: -0.02em;
          margin: 0;
        }
        p { margin: 0; color: var(--on-sf-v); overflow-wrap: anywhere; }
        strong { color: var(--on-sf); }
        .callout {
          display: flex; gap: 12px; align-items: center; text-align: left;
          margin-top: 4px; padding: 12px 16px; border-radius: 16px;
          background: var(--warn-c); color: var(--on-warn-c);
          font-size: 14px; line-height: 20px; font-weight: 600;
        }
        .callout svg { display: block; flex: none; }
        .actions { display: flex; flex-direction: column; gap: 10px; padding: 0 20px 24px; }
        button {
          font: inherit; font-weight: 600; min-height: 52px; padding: 0 24px;
          border: 0; border-radius: 26px; cursor: pointer;
        }
        .primary { background: var(--pri); color: var(--on-pri); }
        .quiet { min-height: 48px; background: transparent; color: var(--on-sf-v); }
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
