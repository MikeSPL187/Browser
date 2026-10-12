package dev.sk2andy.materialbrowser.ui.v5

/*
 * What the v5 site protection panel (V17, canvas screens «f1» and «f2», system-v5.md section 12)
 * shows of a site, and the breakage report it copies: plain text for a GitHub issue, made on the
 * device and never sent anywhere. The report names the site's host only, never a path or a query.
 */
object V5SiteProtectionRules {
    /** The panel's pages: the simple view, «Site works incorrectly?» and «Details». */
    enum class Page { Main, Troubleshoot, Details }

    /** Everything the breakage report says; null fields are settings the engine does not offer. */
    data class Report(
        val host: String,
        val engine: String,
        val appVersion: String,
        val protectionOn: Boolean,
        val strict: Boolean,
        val blocked: Int,
        val thirdPartyCookiesAllowed: Boolean?,
        val cookieBannersHidden: Boolean?,
        val deAmp: Boolean?,
        val fingerprintingProtection: Boolean?,
        val httpsOnly: String?,
    )

    /** The report as Markdown for a GitHub issue; English, as the issues are. */
    fun report(report: Report): String = buildString {
        appendLine("### Site breakage report")
        appendLine()
        appendLine("- Site: ${report.host}")
        appendLine("- Vola: ${report.appVersion} (${report.engine})")
        appendLine("- Protection: ${onOff(report.protectionOn)}, level ${if (report.strict) "Strict" else "Standard"}")
        appendLine("- Blocked on this page: ${report.blocked}")
        report.thirdPartyCookiesAllowed?.let { appendLine("- Third-party cookies: ${if (it) "allowed" else "blocked"}") }
        report.cookieBannersHidden?.let { appendLine("- Cookie banners: ${if (it) "hidden" else "shown"}") }
        report.deAmp?.let { appendLine("- AMP: ${if (it) "original page" else "as linked"}") }
        report.fingerprintingProtection?.let { appendLine("- Fingerprinting protection: ${onOff(it)}") }
        report.httpsOnly?.let { appendLine("- HTTPS-only: $it") }
        appendLine()
        appendLine("What goes wrong:")
    }

    private fun onOff(on: Boolean) = if (on) "on" else "off"
}
