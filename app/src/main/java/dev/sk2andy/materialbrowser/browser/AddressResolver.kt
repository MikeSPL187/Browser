package dev.sk2andy.materialbrowser.browser

import java.net.IDN
import java.net.URI

object AddressResolver {
    private val schemePattern = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    private val ipPattern = Regex("^(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d+)?(?:/.*)?$")
    private val hostPattern = Regex(
        // An international top-level domain (.рф, .онлайн) arrives in punycode: xn--p1ai.
        "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+(?:[a-zA-Z]{2,}|xn--[a-zA-Z0-9-]{2,59})" +
            "(?::\\d+)?(?:[/?#].*)?$",
    )

    fun resolve(input: String): String = resolve(input, SearchEngine.Google)

    fun resolve(
        input: String,
        searchEngine: SearchEngine,
        searchMode: SearchMode = SearchMode.Web,
        searxngInstanceUrl: String = "",
        forceSearch: Boolean = false,
    ): String = when (val target = if (forceSearch) searchTarget(input) else classify(input)) {
        AddressTarget.Blank -> BLANK_URL
        is AddressTarget.Url -> target.value
        is AddressTarget.Search -> searchEngine.buildSearchUrl(
            query = target.query,
            mode = searchMode,
            searxngInstanceUrl = searxngInstanceUrl,
        )
    }

    fun isSearchQuery(input: String): Boolean = classify(input) is AddressTarget.Search

    /** A search suggestion stays a search, whatever it looks like. */
    private fun searchTarget(input: String): AddressTarget =
        input.trim().takeIf(String::isNotEmpty)?.let { AddressTarget.Search(it) } ?: AddressTarget.Blank

    private fun classify(input: String): AddressTarget {
        val value = input.trim()
        if (value.isEmpty()) return AddressTarget.Blank

        if (schemePattern.containsMatchIn(value)) {
            val scheme = runCatching { URI(value).scheme }.getOrNull()
            return if (scheme.equals("http", true) || scheme.equals("https", true)) {
                AddressTarget.Url(value)
            } else {
                AddressTarget.Search(value)
            }
        }

        val asciiCandidate = value.toAsciiHostCandidate()
        val isLocalhost = asciiCandidate.startsWith("localhost:") ||
            asciiCandidate == "localhost" ||
            asciiCandidate.startsWith("localhost/")
        // A word with a dot is a site only when it ends in a real top-level domain: «file.txt» and
        // «node.js» are searches, «example.com» and «пример.рф» are addresses.
        val isSite = hostPattern.matches(asciiCandidate) && hasKnownTopLevelDomain(asciiCandidate)
        return if (
            !value.contains(' ') &&
            (isSite || ipPattern.matches(asciiCandidate) || isLocalhost)
        ) {
            AddressTarget.Url("https://$asciiCandidate")
        } else {
            AddressTarget.Search(value)
        }
    }

    fun displayText(url: String): String = when (url) {
        BLANK_URL -> ""
        else -> displayHost(url)?.removePrefix("www.") ?: url
    }

    private fun displayHost(url: String): String? {
        runCatching { URI(url).host }.getOrNull()?.let { return it }

        val authorityStart = url.indexOf("://").takeIf { it > 0 }?.plus(3) ?: return null
        val authorityEnd = url.indexOfAny(charArrayOf('/', '?', '#'), authorityStart)
            .takeIf { it >= 0 }
            ?: url.length
        return runCatching { URI(url.substring(0, authorityEnd)).host }.getOrNull()
    }

    /** Whether the last label of the host in [candidate] (ASCII, as [toAsciiHostCandidate] makes it) can end an address. */
    private fun hasKnownTopLevelDomain(candidate: String): Boolean {
        val host = candidate.substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')
        val label = host.substringAfterLast('.')
        // An international top-level domain arrives in punycode; all of them are real.
        return label.startsWith("xn--", ignoreCase = true) || KnownTopLevelDomains.contains(label)
    }

    private fun String.toAsciiHostCandidate(): String {
        val suffixIndex = listOf(indexOf('/'), indexOf('?'), indexOf('#'))
            .filter { it >= 0 }
            .minOrNull()
            ?: length
        val hostPort = substring(0, suffixIndex)
        val path = substring(suffixIndex)
        val colonIndex = hostPort.lastIndexOf(':')
        val hasPort = colonIndex > 0 && hostPort.substring(colonIndex + 1).all(Char::isDigit)
        val host = if (hasPort) hostPort.substring(0, colonIndex) else hostPort
        val port = if (hasPort) hostPort.substring(colonIndex) else ""
        return runCatching { IDN.toASCII(host) }.getOrDefault(host) + port + path
    }

    private sealed interface AddressTarget {
        data object Blank : AddressTarget
        data class Url(val value: String) : AddressTarget
        data class Search(val query: String) : AddressTarget
    }
}
