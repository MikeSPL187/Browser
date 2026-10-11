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
    private val SAFE_DOMAIN = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$")

    /**
     * The pref value for [hosts]: their registrable domains, sorted, as Gecko's JSON; empty with no
     * exception, which restores Gecko's default. A host that does not reduce to a plain ASCII
     * domain is left out rather than escaped.
     */
    fun prefValue(
        hosts: Collection<String>,
        registrableDomain: (String) -> String? = CandyPublicSuffixRules::registrableDomain,
    ): String {
        val domains = hosts.asSequence()
            .mapNotNull(PrivacyRequestSanitizer::normalizeHost)
            .map { host -> registrableDomain(host) ?: host }
            .filter(SAFE_DOMAIN::matches)
            .toSortedSet()
        if (domains.isEmpty()) return ""
        return domains.joinToString(separator = ",", prefix = "[", postfix = "]") { domain ->
            """{"firstPartyDomain":"$domain","overrides":"$ALL_TARGETS_OFF"},""" +
                """{"firstPartyDomain":"$domain","thirdPartyDomain":"*","overrides":"$ALL_TARGETS_OFF"}"""
        }
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
        Event.COOKIES_PARTITIONED_TRACKER,
    )

    fun fromLog(log: List<LogEntry>?): SiteProtectionCounts? = log?.let { entries ->
        counts(
            entries.flatMap { entry ->
                entry.blockingData.orEmpty().map { data -> BlockedEvent(entry.origin, data.category, data.blocked) }
            },
        )
    }

    /** Counts the sites per kind; events the engine only logged (not blocked) do not count. */
    fun counts(events: List<BlockedEvent>): SiteProtectionCounts {
        val blocked = events.filter { it.blocked && !it.origin.isNullOrBlank() }
        fun sites(categories: Set<Int>) = blocked.asSequence()
            .filter { it.category in categories }
            .map { it.origin }
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
