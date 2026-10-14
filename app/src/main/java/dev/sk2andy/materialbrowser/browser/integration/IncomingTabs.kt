package dev.sk2andy.materialbrowser.browser.integration

import dev.sk2andy.materialbrowser.browser.BrowserController

/** Where a link from another app opens (V29 security audit). */
internal object IncomingTabRules {
    /**
     * Another app's link never joins the private session: in a private tab it would share that
     * session's cookies and sign-ins, even behind the private-tabs lock. Chrome and Firefox do the
     * same.
     */
    const val OPENS_PRIVATE = false

    /** «Back» returns to the tab the link came over only when both are on the same side. */
    fun openerTabId(selectedTabId: String, selectedIsPrivate: Boolean): String? =
        selectedTabId.takeUnless { selectedIsPrivate }
}

/** Opens [url] from another app in a new regular tab; false when no tab could be made. */
internal fun BrowserController.openUrlFromOtherApp(url: String): Boolean {
    leaveSiteCapsule()
    val previousTabId = selectedTabId
    return createTab(
        initialUrl = url,
        isIncognito = IncomingTabRules.OPENS_PRIVATE,
        openerTabId = IncomingTabRules.openerTabId(previousTabId, selectedTab.isIncognito),
    ) != previousTabId
}
