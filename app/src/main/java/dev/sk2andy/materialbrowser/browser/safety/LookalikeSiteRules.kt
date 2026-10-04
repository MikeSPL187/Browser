package dev.sk2andy.materialbrowser.browser.safety

import com.google.common.net.InternetDomainName
import java.net.IDN
import java.net.URI
import java.util.Locale

/**
 * Sites that pretend to be another (board W-DangerousSite), found on the phone with no network:
 * an address whose letters only look like those of a site the user knows — `paypa1.com`,
 * `bank-exarnple.ru`, a Cyrillic `аpple.com` — is flagged; the real site and its own
 * subdomains never are. «Known» is [WELL_KNOWN_HOSTS] plus the user's Essentials and favorites.
 */
object LookalikeSiteRules {
    /**
     * The genuine host [url] imitates, or null when it imitates none of [knownHosts] (hosts or
     * page addresses).
     */
    fun imitatedHost(url: String, knownHosts: Collection<String>): String? {
        val host = hostOf(url) ?: return null
        val registrable = registrableDomain(host)
        val hostSkeleton = skeleton(host)
        val registrableSkeleton = skeleton(registrable)
        var imitated: String? = null
        for (raw in knownHosts) {
            val known = (if ("://" in raw) hostOf(raw) else normalizedHost(raw)) ?: continue
            val knownRegistrable = registrableDomain(known)
            // The real site, or one of its own subdomains: trusted, whatever else matches.
            if (knownRegistrable == registrable) return null
            if (imitated != null) continue
            if (skeleton(known) == hostSkeleton || skeleton(knownRegistrable) == registrableSkeleton) {
                imitated = known
            }
        }
        return imitated
    }

    /** How an address reads to the eye: look-alike letters folded, separators dropped. */
    fun skeleton(host: String): String {
        val folded = buildString(host.length) {
            host.lowercase(Locale.ROOT).forEach { character ->
                when (character) {
                    '-', '.', '_' -> Unit
                    else -> append(confusables[character] ?: character)
                }
            }
        }
        return folded
            .replace("rn", "m")
            .replace("vv", "w")
            .replace("cl", "d")
    }

    private fun hostOf(url: String): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") return null
        // URI gives no host for a non-ASCII one; the authority still holds it.
        val host = uri.host ?: uri.authority?.substringAfterLast('@')?.substringBefore(':')
        return normalizedHost(host)
    }

    private fun normalizedHost(raw: String?): String? {
        val host = raw?.trim()?.trimEnd('.')?.lowercase(Locale.ROOT)
            ?.takeIf(String::isNotEmpty)
            ?: return null
        if (host.all { it.isDigit() || it == '.' } || ':' in host) return null
        val unicode = runCatching { IDN.toUnicode(host, IDN.ALLOW_UNASSIGNED) }.getOrDefault(host)
        return unicode.removePrefix("www.")
    }

    private fun registrableDomain(host: String): String {
        val ascii = runCatching { IDN.toASCII(host, IDN.ALLOW_UNASSIGNED) }.getOrNull()
            ?: return host
        val domain = runCatching { InternetDomainName.from(ascii) }.getOrNull() ?: return host
        if (!domain.isUnderPublicSuffix) return host
        val top = domain.topPrivateDomain().toString()
        return runCatching { IDN.toUnicode(top, IDN.ALLOW_UNASSIGNED) }.getOrDefault(top)
    }

    /** Letters and digits that are read as another Latin letter. */
    private val confusables: Map<Char, Char> = mapOf(
        // Digits
        '0' to 'o', '1' to 'l', '3' to 'e', '5' to 's',
        // Latin look-alikes
        'i' to 'l', 'ı' to 'l', 'ł' to 'l', 'ɡ' to 'g', 'ν' to 'v',
        // Cyrillic
        'а' to 'a', 'в' to 'b', 'е' to 'e', 'ё' to 'e', 'к' to 'k', 'м' to 'm', 'н' to 'h',
        'о' to 'o', 'р' to 'p', 'с' to 'c', 'т' to 't', 'у' to 'y', 'х' to 'x', 'і' to 'l',
        'ј' to 'j', 'ѕ' to 's', 'ԁ' to 'd', 'ԛ' to 'q', 'ԝ' to 'w', 'һ' to 'h', 'ӏ' to 'l',
        // Greek
        'α' to 'a', 'ο' to 'o', 'ρ' to 'p', 'τ' to 't', 'υ' to 'u', 'ε' to 'e', 'κ' to 'k',
        'ι' to 'l',
    )

    /**
     * Sites phishing pages most often pretend to be: payments, mail, banks and government
     * services in the regions Vola is used in. The user's own sites are added to these.
     */
    val WELL_KNOWN_HOSTS: List<String> = listOf(
        "google.com", "gmail.com", "youtube.com", "apple.com", "icloud.com", "microsoft.com",
        "live.com", "outlook.com", "office.com", "facebook.com", "instagram.com", "whatsapp.com",
        "telegram.org", "x.com", "twitter.com", "amazon.com", "ebay.com", "paypal.com",
        "netflix.com", "spotify.com", "steampowered.com", "steamcommunity.com", "github.com",
        "binance.com", "coinbase.com", "linkedin.com", "dropbox.com", "adobe.com",
        "yandex.ru", "ya.ru", "mail.ru", "vk.com", "ok.ru", "gosuslugi.ru", "nalog.gov.ru",
        "sberbank.ru", "online.sberbank.ru", "tbank.ru", "tinkoff.ru", "vtb.ru",
        "alfabank.ru", "gazprombank.ru", "raiffeisen.ru", "pochtabank.ru", "ozon.ru",
        "wildberries.ru", "avito.ru", "kaspi.kz", "privatbank.ua", "monobank.ua",
    )
}
