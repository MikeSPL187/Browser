package dev.sk2andy.materialbrowser.browser.gecko

import dev.sk2andy.materialbrowser.blocking.CandyPublicSuffixRules
import dev.sk2andy.materialbrowser.blocking.PrivacyRequestSanitizer
import dev.sk2andy.materialbrowser.browser.protection.SiteProtectionCounts
import org.mozilla.geckoview.ContentBlockingController.Event
import org.mozilla.geckoview.ContentBlockingController.LogEntry

/**
 * Per-site exceptions to Gecko's fingerprinting protection (v5 site protection; owner's decision
 * 2026-10-11: a list of its own, the rest of the protection stays on). Gecko reads them from
 * `privacy.fingerprintingProtection.granularOverrides`, a JSON list of first-party domains whose
 * targets are switched off. Each site gets two entries: its own pages, and frames other sites put
 * in them, so an embedded player or payment form works too.
 */
internal object GeckoFingerprintingExceptions {
    const val PREF = "privacy.fingerprintingProtection.granularOverrides"
    private const val ALL_TARGETS_OFF = "-AllTargets"

    /**
     * The domain shape Gecko's override schema accepts: labels of up to 63 characters and a top
     * level of 2–6 Latin letters. Gecko silently drops anything else, so `.рф` (`xn--p1ai`), longer
     * zones such as `.digital` and IP addresses cannot be excepted.
     */
    private val GECKO_DOMAIN = Regex("^([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,6}$")

    /** Whether Gecko can except [host]; the panel hides the fingerprinting switch when it cannot. */
    fun supports(
        host: String,
        registrableDomain: (String) -> String? = CandyPublicSuffixRules::registrableDomain,
    ): Boolean = exceptionDomain(host, registrableDomain) != null

    /**
     * The pref value for [hosts]: their registrable domains, sorted, as Gecko's JSON; empty with no
     * exception, which restores Gecko's default. A host Gecko cannot except ([supports]) is left
     * out rather than escaped.
     */
    fun prefValue(
        hosts: Collection<String>,
        registrableDomain: (String) -> String? = CandyPublicSuffixRules::registrableDomain,
    ): String {
        val domains = hosts.asSequence()
            .mapNotNull { host -> exceptionDomain(host, registrableDomain) }
            .toSortedSet()
        if (domains.isEmpty()) return ""
        return domains.joinToString(separator = ",", prefix = "[", postfix = "]") { domain ->
            """{"firstPartyDomain":"$domain","overrides":"$ALL_TARGETS_OFF"},""" +
                """{"firstPartyDomain":"$domain","thirdPartyDomain":"*","overrides":"$ALL_TARGETS_OFF"}"""
        }
    }

    private fun exceptionDomain(host: String, registrableDomain: (String) -> String?): String? {
        val normalized = PrivacyRequestSanitizer.normalizeHost(host) ?: return null
        return (registrableDomain(normalized) ?: normalized).takeIf(GECKO_DOMAIN::matches)
    }
}

/**
 * Gecko's content blocking log of one page as [SiteProtectionCounts]: how many sites of each kind
 * Gecko blocked (a site that tried many times counts once, as in Firefox's protections panel).
 */
internal object GeckoContentBlockingCounts {
    private val trackers = setOf(
        Event.BLOCKED_TRACKING_CONTENT,
        Event.REPLACED_TRACKING_CONTENT,
        Event.BLOCKED_EMAILTRACKING_CONTENT,
        Event.PURGED_BOUNCETRACKER,
    )
    private val fingerprinters = setOf(
        Event.BLOCKED_FINGERPRINTING_CONTENT,
        Event.REPLACED_FINGERPRINTING_CONTENT,
        Event.BLOCKED_SUSPICIOUS_FINGERPRINTING,
    )
    private val cryptominers = setOf(Event.BLOCKED_CRYPTOMINING_CONTENT)
    private val socialTrackers = setOf(Event.BLOCKED_SOCIALTRACKING_CONTENT)
    private val trackingCookies = setOf(
        Event.COOKIES_BLOCKED_TRACKER,
        Event.COOKIES_BLOCKED_SOCIALTRACKER,
    )

    fun fromLog(log: List<LogEntry>?): SiteProtectionCounts? = log?.let { entries ->
        counts(
            entries.flatMap { entry ->
                entry.blockingData.orEmpty().map { data -> BlockedEvent(entry.origin, data.category, data.blocked) }
            },
        )
    }

    /**
     * Counts the sites per kind: origins of one registrable domain (`a.tracker.example` and
     * `b.tracker.example`) are one site. Events the engine only logged (not blocked) do not count.
     */
    fun counts(
        events: List<BlockedEvent>,
        registrableDomain: (String) -> String? = CandyPublicSuffixRules::registrableDomain,
    ): SiteProtectionCounts {
        val blocked = events.mapNotNull { event ->
            if (!event.blocked) return@mapNotNull null
            val host = event.origin?.let(PrivacyRequestSanitizer::webHost) ?: return@mapNotNull null
            event.category to (registrableDomain(host) ?: host)
        }
        fun sites(categories: Set<Int>) = blocked.asSequence()
            .filter { (category, _) -> category in categories }
            .map { (_, site) -> site }
            .distinct()
            .count()
        return SiteProtectionCounts(
            trackerSites = sites(trackers),
            fingerprinterSites = sites(fingerprinters),
            cryptominerSites = sites(cryptominers),
            socialTrackerSites = sites(socialTrackers),
            trackingCookieSites = sites(trackingCookies),
        )
    }

    data class BlockedEvent(val origin: String?, val category: Int, val blocked: Boolean)
}
