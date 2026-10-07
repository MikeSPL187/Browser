package dev.sk2andy.materialbrowser.browser

/**
 * When the first run (or a new gesture lesson) covers the screen. A cold start from a link shows
 * the page it was opened for; the first run waits, already marked as started, and appears on the
 * next start from the launcher: a later cold start or a return to the running app.
 */
internal object FirstRunPresentationRules {
    /** At creation: the first run is due and Vola was not cold-started to open a link. */
    fun showNow(onboardingRequired: Boolean, isColdExternalLinkLaunch: Boolean): Boolean =
        onboardingRequired && !isColdExternalLinkLaunch

    /** A new intent to the running app: a launcher start brings up a first run still waiting. */
    fun showOnNewIntent(isLauncherLaunch: Boolean, isOnboardingCompleted: Boolean): Boolean =
        isLauncherLaunch && !isOnboardingCompleted
}
