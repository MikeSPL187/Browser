package dev.sk2andy.materialbrowser.browser.gecko

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.sk2andy.materialbrowser.R
import dev.sk2andy.materialbrowser.browser.CertificateProblem
import dev.sk2andy.materialbrowser.browser.CertificateProblemRules
import dev.sk2andy.materialbrowser.browser.SiteCertificateRules
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Accent
import dev.sk2andy.materialbrowser.ui.theme.v5.V5ColorScheme
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Colors
import java.security.cert.X509Certificate
import java.text.DateFormat
import java.util.Date

/**
 * Warning page for a site whose certificate failed (canvas screen «CertError»).
 *
 * GeckoView renders the page returned from NavigationDelegate.onLoadError as the certificate error
 * document and exposes document.addCertException(isTemporary) to it. The page asks for a temporary
 * exception only: it lasts until Vola's process ends, for this site alone. That call is the page's
 * only privileged action and sits behind «Details»; everything shown is escaped.
 */
internal class GeckoCertErrorPages(private val context: Context) {
    fun dataUri(failedUri: String?, certificate: X509Certificate?, nowMillis: Long = System.currentTimeMillis()): String? {
        val host = GeckoHttpsOnlyErrorPage.displayHost(failedUri)
        val problem = CertificateProblemRules.problem(certificate, host, nowMillis)
        val dates = DateFormat.getDateInstance(DateFormat.LONG, context.resources.configuration.locales[0])
        val site = SiteCertificateRules.fromX509(host, certificate)
        val marker = GeckoHttpsOnlyErrorPage.HOST_MARKER
        val rows = buildList {
            add(context.getString(R.string.cert_error_problem) to problemLabel(problem))
            when (problem) {
                CertificateProblem.Expired -> site?.validUntilMillis?.let { until ->
                    add(context.getString(R.string.cert_error_valid_until) to dates.format(Date(until)))
                }
                CertificateProblem.NotYetValid -> site?.validFromMillis?.let { from ->
                    add(context.getString(R.string.cert_error_valid_from) to dates.format(Date(from)))
                }
                CertificateProblem.WrongSite -> site?.subjectName?.let { name ->
                    add(context.getString(R.string.cert_error_issued_for) to name)
                }
                CertificateProblem.SelfSigned,
                CertificateProblem.Untrusted,
                -> site?.issuerName?.let { issuer -> add(context.getString(R.string.cert_error_issuer) to issuer) }
            }
            // A wrong date on the phone makes a good certificate look expired or not yet valid:
            // the page shows the phone's date for the reader to check, it does not judge it.
            if (problem == CertificateProblem.Expired || problem == CertificateProblem.NotYetValid) {
                add(context.getString(R.string.cert_error_phone_date) to dates.format(Date(nowMillis)))
            }
        }
        val strings = GeckoCertErrorPage.Strings(
            languageTag = context.resources.configuration.locales[0].toLanguageTag(),
            title = context.getString(R.string.cert_error_title),
            body = context.getString(
                when (problem) {
                    CertificateProblem.Expired -> R.string.cert_error_body_expired
                    CertificateProblem.NotYetValid -> R.string.cert_error_body_not_yet_valid
                    CertificateProblem.WrongSite -> R.string.cert_error_body_wrong_site
                    CertificateProblem.SelfSigned -> R.string.cert_error_body_self_signed
                    CertificateProblem.Untrusted -> R.string.cert_error_body_untrusted
                },
                marker,
            ) + " " + context.getString(R.string.cert_error_risk),
            backLabel = context.getString(R.string.cert_error_back),
            detailsLabel = context.getString(R.string.cert_error_details),
            detailsText = context.getString(R.string.cert_error_details_text, marker),
            continueLabel = context.getString(R.string.cert_error_continue),
            continueNote = context.getString(R.string.cert_error_continue_note),
            failedLabel = context.getString(R.string.cert_error_continue_failed),
        )
        return GeckoHttpsOnlyErrorPage.dataUri(GeckoCertErrorPage.html(strings, host, rows))
    }

    private fun problemLabel(problem: CertificateProblem): String = context.getString(
        when (problem) {
            CertificateProblem.Expired -> R.string.cert_error_problem_expired
            CertificateProblem.NotYetValid -> R.string.cert_error_problem_not_yet_valid
            CertificateProblem.WrongSite -> R.string.cert_error_problem_wrong_site
            CertificateProblem.SelfSigned -> R.string.cert_error_problem_self_signed
            CertificateProblem.Untrusted -> R.string.cert_error_problem_untrusted
        },
    )
}

internal object GeckoCertErrorPage {
    data class Strings(
        val languageTag: String,
        val title: String,
        /** Says why, with [GeckoHttpsOnlyErrorPage.HOST_MARKER] for the site. */
        val body: String,
        val backLabel: String,
        val detailsLabel: String,
        /** What opening anyway means, with the site's marker. */
        val detailsText: String,
        val continueLabel: String,
        /** How long the exception lasts. */
        val continueNote: String,
        /** Said when the engine refused the exception. */
        val failedLabel: String,
    )

    /** The v5 roles the page uses, as CSS colors, for one theme. */
    data class Palette(
        val background: String,
        val surface: String,
        val label: String,
        val labelSecondary: String,
        val separator: String,
        val fill: String,
        val accentFill: String,
        val onAccentFill: String,
        val warning: String,
        val danger: String,
    ) {
        companion object {
            fun from(scheme: V5ColorScheme) = Palette(
                background = css(scheme.background),
                surface = css(scheme.surface),
                label = css(scheme.label),
                labelSecondary = css(scheme.labelSecondary),
                separator = css(scheme.separator),
                fill = css(scheme.fill),
                accentFill = css(scheme.accentFill),
                onAccentFill = css(scheme.onAccentFill),
                warning = css(scheme.warning),
                danger = css(scheme.danger),
            )

            /** `#RRGGBBAA`, so translucent roles such as `fill` keep their alpha. */
            private fun css(color: Color): String {
                val argb = color.toArgb()
                return "#%06X%02X".format(argb and 0xFFFFFF, (argb ushr 24) and 0xFF)
            }
        }
    }

    /** The page is not tied to a profile, so it takes the default accent. */
    val defaultPalettes: Pair<Palette, Palette> =
        Palette.from(V5Colors.light(V5Accent.Default)) to Palette.from(V5Colors.dark(V5Accent.Default))

    fun html(
        strings: Strings,
        host: String,
        rows: List<Pair<String, String>>,
        palettes: Pair<Palette, Palette> = defaultPalettes,
    ): String {
        val escape = GeckoHttpsOnlyErrorPage::escape
        fun withHost(text: String) =
            escape(text).replace(escape(GeckoHttpsOnlyErrorPage.HOST_MARKER), "<strong>${escape(host)}</strong>")
        val list = rows.mapIndexed { index, (label, value) ->
            "<div class=\"row\"><span>${escape(label)}</span>" +
                "<b${if (index == 0) " class=\"problem\"" else ""}>${escape(value)}</b></div>"
        }.joinToString(separator = "")
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
            "<div class=\"badge\" aria-hidden=\"true\">$WARNING_ICON</div>",
            "<h1>${escape(strings.title)}</h1>",
            "<p>${withHost(strings.body)}</p>",
            "<div class=\"card\">$list</div>",
            "<div class=\"actions\">",
            "<button id=\"back\" class=\"primary\">${escape(strings.backLabel)}</button>",
            "<button id=\"details\" class=\"link\" aria-expanded=\"false\" aria-controls=\"risk\">" +
                "${escape(strings.detailsLabel)}</button>",
            "<section id=\"risk\" hidden>",
            "<p>${withHost(strings.detailsText)}</p>",
            "<button id=\"continue\" class=\"danger\">${escape(strings.continueLabel)}</button>",
            "<p class=\"note\">${escape(strings.continueNote)}</p>",
            "<p id=\"failed\" class=\"note\" role=\"alert\" hidden>${escape(strings.failedLabel)}</p>",
            "</section>",
            "</div>",
            "</main>",
            "<script>$SCRIPT</script>",
            "</body>",
            "</html>",
        ).joinToString(separator = "\n")
    }

    private fun colors(palettes: Pair<Palette, Palette>): String {
        fun Palette.variables() = "--bg: $background; --sf: $surface; --label: $label; " +
            "--label2: $labelSecondary; --sep: $separator; --fill: $fill; --acc: $accentFill; " +
            "--on-acc: $onAccentFill; --warn: $warning; --danger: $danger;"
        return ":root { ${palettes.first.variables()} }\n" +
            "@media (prefers-color-scheme: dark) { :root { ${palettes.second.variables()} } }"
    }

    // Material Symbols Rounded (Apache License 2.0): warning (filled).
    private const val WARNING_ICON =
        "<svg viewBox=\"0 -960 960 960\" width=\"40\" height=\"40\" fill=\"currentColor\">" +
            "<path d=\"M109-120q-11 0-20-5.5T75-140q-5-9-5.5-19.5T75-180l370-640q6-10 15.5-15t19.5" +
            "-5q10 0 19.5 5t15.5 15l370 640q6 10 5.5 20.5T885-140q-5 9-14 14.5t-20 5.5H109Zm371-" +
            "120q17 0 28.5-11.5T520-280q0-17-11.5-28.5T480-320q-17 0-28.5 11.5T440-280q0 17 11.5" +
            " 28.5T480-240Zm0-120q17 0 28.5-11.5T520-400v-120q0-17-11.5-28.5T480-560q-17 0-28.5 " +
            "11.5T440-520v120q0 17 11.5 28.5T480-360Z\"/></svg>"

    // Sizes follow the v5 tokens (system-v5.md 2, 3, 5), as the v5 HTTPS-only page: 24 margins,
    // largeTitle 34/41, body 17/24, callout 15/20, footnote 13/18, card radius 16, rows 48 high,
    // capsules 52 high, Inter where installed.
    private val STYLE = """
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
          background: var(--warn); color: #FFFFFF;
        }
        h1 {
          font-size: 34px; line-height: 41px; font-weight: 700; letter-spacing: -0.022em;
          margin: 28px 0 0;
        }
        p { margin: 12px 0 0; color: var(--label2); overflow-wrap: anywhere; }
        strong { color: var(--label); font-weight: 600; }
        .card { margin-top: 20px; border-radius: 16px; background: var(--sf); overflow: hidden; }
        .row {
          display: flex; gap: 12px; align-items: center; justify-content: space-between;
          min-height: 48px; padding: 12px 16px; font-size: 15px; line-height: 20px;
        }
        .row + .row { border-top: 0.5px solid var(--sep); margin-left: 16px; padding-left: 0; }
        .row span { color: var(--label2); }
        .row b { font-weight: 500; text-align: right; overflow-wrap: anywhere; }
        .row b.problem { font-weight: 600; color: var(--warn); }
        .actions { display: flex; flex-direction: column; gap: 6px; margin-top: auto; padding-top: 28px; }
        button {
          font: inherit; font-weight: 600; min-height: 52px; padding: 0 24px;
          border: 0; border-radius: 26px; cursor: pointer;
        }
        .primary { background: var(--acc); color: var(--on-acc); }
        .link {
          min-height: 48px; font-size: 13px; font-weight: 400; background: transparent;
          color: var(--label2); text-decoration: underline; text-underline-offset: 3px;
        }
        #risk p { margin: 0 0 12px; font-size: 15px; line-height: 20px; }
        .danger { width: 100%; min-height: 48px; background: var(--fill); color: var(--danger); }
        #risk .note { margin: 8px 0 0; text-align: center; font-size: 13px; line-height: 18px; }
    """.trimIndent()

    // «Back» leaves when there is a page to go back to. «Details» shows what opening anyway means
    // before the button that does it; the exception is temporary, and a refusal is said, not hidden.
    private val SCRIPT = """
        const back = document.getElementById("back");
        if (history.length < 2) back.hidden = true;
        back.addEventListener("click", () => history.back());
        const details = document.getElementById("details");
        details.addEventListener("click", () => {
          const risk = document.getElementById("risk");
          risk.hidden = !risk.hidden;
          details.setAttribute("aria-expanded", String(!risk.hidden));
        });
        document.getElementById("continue").addEventListener("click", () => {
          Promise.resolve()
            .then(() => document.addCertException(true))
            .then(() => location.reload())
            .catch(() => { document.getElementById("failed").hidden = false; });
        });
    """.trimIndent()
}
