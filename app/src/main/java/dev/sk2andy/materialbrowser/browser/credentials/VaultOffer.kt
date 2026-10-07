package dev.sk2andy.materialbrowser.browser.credentials

import android.content.Context

/**
 * When Vola offers its password vault on a sign-in field (#123, H2): without a vault the page has
 * nothing to fill, since the system password manager serves only browsers it trusts. The offer is
 * a reminder, never a nag: at most [MAX_OFFERS] times, once per app run, never again once taken.
 */
internal object VaultOfferRules {
    const val MAX_OFFERS = 3

    fun shouldOffer(
        vaultExists: Boolean,
        offeredBefore: Int,
        accepted: Boolean,
        offeredThisRun: Boolean,
        sheetShowing: Boolean,
    ): Boolean = !vaultExists && !accepted && !offeredThisRun && !sheetShowing && offeredBefore < MAX_OFFERS
}

/** How often the vault offer appeared and whether it was taken; nothing about the sites. */
internal class VaultOfferStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    val offered: Int get() = preferences.getInt(KEY_OFFERED, 0)

    val accepted: Boolean get() = preferences.getBoolean(KEY_ACCEPTED, false)

    fun markOffered() {
        offeredThisRun = true
        preferences.edit().putInt(KEY_OFFERED, offered + 1).apply()
    }

    fun markAccepted() {
        preferences.edit().putBoolean(KEY_ACCEPTED, true).apply()
    }

    companion object {
        /** Once per process: every browser window shares it. */
        var offeredThisRun = false
            private set

        private const val PREFERENCES = "vola_vault_offer"
        private const val KEY_OFFERED = "offered"
        private const val KEY_ACCEPTED = "accepted"
    }
}
