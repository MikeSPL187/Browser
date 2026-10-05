package dev.sk2andy.materialbrowser.browser.safety

import android.content.Context
import dev.sk2andy.materialbrowser.blocking.SortedHostIndex
import java.net.IDN
import java.util.concurrent.CompletableFuture

/**
 * Known phishing, malware and scam hosts (HaGeZi Threat Intelligence Feeds, stage S2), bundled with
 * the app and checked on the phone: no address leaves it. The list loads off the main thread;
 * until it is ready no host counts as listed.
 */
internal class ThreatHostList(private val index: CompletableFuture<SortedHostIndex>) {
    /** True when [host] or one of its parent domains is on the list. */
    fun contains(host: String): Boolean {
        val loaded = index.getNow(null) ?: return false
        var candidate = asciiHost(host) ?: return false
        while (true) {
            if (candidate in loaded) return true
            val dot = candidate.indexOf('.')
            if (dot < 0) return false
            candidate = candidate.substring(dot + 1)
        }
    }

    /** The list stores hosts in ASCII (punycode for international names), lowercase. */
    private fun asciiHost(host: String): String? = runCatching {
        IDN.toASCII(host.trim().trim('.'), IDN.ALLOW_UNASSIGNED).lowercase()
    }.getOrNull()?.takeIf(String::isNotEmpty)

    companion object {
        private const val ASSET = "threat_hosts.txt"

        @Volatile
        private var instance: ThreatHostList? = null

        fun get(context: Context): ThreatHostList = instance ?: synchronized(this) {
            instance ?: run {
                val assets = context.applicationContext.assets
                ThreatHostList(
                    CompletableFuture.supplyAsync {
                        runCatching {
                            assets.open(ASSET).use { SortedHostIndex.from(it.readBytes()) }
                        }.getOrDefault(SortedHostIndex.Empty)
                    },
                )
            }.also { instance = it }
        }
    }
}
