package dev.sk2andy.materialbrowser.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** How one kind of connection stands now («What to check» under «No connection», canvas «Error»). */
enum class NetworkLink { Connected, NoInternet, NotConnected }

@Immutable
internal data class NetworkChecks(val wifi: NetworkLink, val cellular: NetworkLink)

internal object NetworkCheckRules {
    /** A network the phone sees: its transports and whether Android found the internet through it. */
    data class Seen(val wifi: Boolean, val cellular: Boolean, val validated: Boolean)

    /**
     * Wi-Fi or the mobile network: connected with the internet, connected without it, or not at
     * all. Android does not say whether a radio is off without more permissions, so «not connected»
     * covers both.
     */
    fun link(networks: Collection<Seen>, wifi: Boolean): NetworkLink {
        val ofKind = networks.filter { if (wifi) it.wifi else it.cellular }
        return when {
            ofKind.isEmpty() -> NetworkLink.NotConnected
            ofKind.any { it.validated } -> NetworkLink.Connected
            else -> NetworkLink.NoInternet
        }
    }

    fun checks(networks: Collection<Seen>): NetworkChecks =
        NetworkChecks(wifi = link(networks, wifi = true), cellular = link(networks, wifi = false))
}

/** Wi-Fi and the mobile network as they stand, following changes while the page is shown. */
@Composable
internal fun rememberNetworkChecks(): NetworkChecks {
    val context = LocalContext.current
    val seen = remember { mutableStateMapOf<Network, NetworkCheckRules.Seen>() }
    DisposableEffect(context) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                seen[network] = NetworkCheckRules.Seen(
                    wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                    cellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
                    validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                )
            }

            override fun onLost(network: Network) {
                seen.remove(network)
            }
        }
        val registered = manager != null && runCatching {
            manager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
        }.isSuccess
        onDispose {
            if (registered) runCatching { manager?.unregisterNetworkCallback(callback) }
        }
    }
    return NetworkCheckRules.checks(seen.values)
}
