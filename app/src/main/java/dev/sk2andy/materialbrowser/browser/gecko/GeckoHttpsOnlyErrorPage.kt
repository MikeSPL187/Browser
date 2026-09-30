package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import dev.sk2andy.materialbrowser.R
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Warning page for navigations that HTTPS-only mode stopped because the site has no working HTTPS.
 *
 * GeckoView renders the page returned from NavigationDelegate.onLoadError as an error document and
 * exposes document.reloadWithHttpsOnlyException() to it, which reloads the request over HTTP for
 * this page only. That call is the page's only privileged action; everything shown is escaped.
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

    /** The ASCII (punycode) host keeps look-alike Unicode domains recognizable. */
    fun displayHost(failedUri: String?): String {
        if (failedUri.isNullOrBlank()) return ""
        return runCatching { URI(failedUri).host }.getOrNull()
            ?.takeIf(String::isNotBlank)
            ?: failedUri
    }

    fun html(strings: Strings, host: String): String {
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
            "<style>$STYLE</style>",
            "</head>",
            "<body>",
            "<main>",
            "<div class=\"badge\" aria-hidden=\"true\">$OPEN_LOCK_ICON</div>",
            "<h1>${escape(strings.title)}</h1>",
            "<p>$body</p>",
            "<p class=\"advice\">${escape(strings.advice)}</p>",
            "<div class=\"actions\">",
            "<button id=\"back\" class=\"primary\">${escape(strings.backLabel)}</button>",
            "<button id=\"continue\" class=\"secondary\">${escape(strings.continueLabel)}</button>",
            "</div>",
            "</main>",
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

    private const val OPEN_LOCK_ICON =
        "<svg viewBox=\"0 0 24 24\" width=\"36\" height=\"36\" fill=\"none\" " +
            "stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" " +
            "stroke-linejoin=\"round\">" +
            "<rect x=\"5\" y=\"11\" width=\"14\" height=\"10\" rx=\"2\"/>" +
            "<path d=\"M8 11V7a4 4 0 0 1 7.46-2\"/>" +
            "<circle cx=\"12\" cy=\"16\" r=\"1.5\" fill=\"currentColor\" stroke=\"none\"/>" +
            "</svg>"

    // The violet palette Vola used before the v4 workspace schemes; the page moves to v4 with the
    // HTTPS-only screen redesign.
    private val STYLE = """
        :root {
          --surface: #FDF8FE; --on-surface: #1C1B1F; --on-surface-variant: #484550;
          --badge: #FFDAD6; --on-badge: #93000A;
          --primary: #5E4EB7; --on-primary: #FFFFFF; --outline: #797581;
        }
        @media (prefers-color-scheme: dark) {
          :root {
            --surface: #141317; --on-surface: #E5E1E7; --on-surface-variant: #C9C4D1;
            --badge: #93000A; --on-badge: #FFDAD6;
            --primary: #C9BFFF; --on-primary: #2F1887; --outline: #938F9B;
          }
        }
        * { box-sizing: border-box; }
        html, body { margin: 0; min-height: 100%; background: var(--surface); }
        body {
          color: var(--on-surface);
          font: 16px/1.5 system-ui, -apple-system, "Roboto", sans-serif;
          display: flex; align-items: center; justify-content: center;
          min-height: 100vh; padding: 32px 24px;
        }
        main { width: 100%; max-width: 560px; }
        .badge {
          width: 72px; height: 72px; border-radius: 24px; margin-bottom: 24px;
          display: flex; align-items: center; justify-content: center;
          background: var(--badge); color: var(--on-badge);
        }
        h1 { font-size: 26px; line-height: 1.25; font-weight: 700; margin: 0 0 12px; }
        p { margin: 0 0 12px; overflow-wrap: anywhere; }
        .advice { color: var(--on-surface-variant); font-size: 14px; }
        .actions { display: flex; flex-direction: column; gap: 12px; margin-top: 28px; }
        button {
          font: inherit; font-weight: 600; min-height: 52px; padding: 0 24px;
          border-radius: 26px; cursor: pointer;
        }
        .primary { background: var(--primary); color: var(--on-primary); border: 0; }
        .secondary {
          background: transparent; color: var(--on-surface);
          border: 1px solid var(--outline);
        }
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
