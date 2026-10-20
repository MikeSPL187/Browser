package dev.sk2andy.materialbrowser.browser

import androidx.compose.runtime.mutableStateMapOf

/**
 * Sites a workspace marked for one per-site setting, such as muted sound or the desktop version
 * (#500). Regular tabs keep their marks in [save]'s storage; private tabs only in memory, until the
 * last private tab closes.
 */
internal class ProfileSiteDomains(
    load: () -> Map<String, Set<String>>,
    private val save: (Map<String, Set<String>>) -> Unit,
) {
    private val permanent = mutableStateMapOf<String, Set<String>>().apply { putAll(load()) }
    private val temporary = mutableStateMapOf<String, Set<String>>()

    private fun domainsFor(isPrivate: Boolean) = if (isPrivate) temporary else permanent

    fun contains(profileId: String, isPrivate: Boolean, url: String?): Boolean =
        SiteDomainRules.contains(url, domainsFor(isPrivate)[profileId].orEmpty())

    /** Marks or unmarks [domain] for [profileId]; returns whether the marks changed. */
    fun set(profileId: String, isPrivate: Boolean, domain: String, marked: Boolean): Boolean {
        val domainsByProfile = domainsFor(isPrivate)
        val current = domainsByProfile[profileId].orEmpty()
        val updated = SiteDomainRules.withState(current, domain, marked, SiteDomainRules.MAX_PER_PROFILE)
        if (updated == current) return false
        if (updated.isEmpty()) domainsByProfile.remove(profileId) else domainsByProfile[profileId] = updated
        if (!isPrivate) save(permanent.toMap())
        return true
    }

    /** After the workspaces load: marks of a workspace that no longer exists are dropped. */
    fun keepProfiles(profileIds: Set<String>) {
        permanent.keys.retainAll(profileIds)
        save(permanent.toMap())
    }

    fun forgetProfile(profileId: String) {
        if (permanent.remove(profileId) != null) save(permanent.toMap())
        temporary.remove(profileId)
    }

    /** The last private tab closed. */
    fun clearPrivate() {
        temporary.clear()
    }

    /** Browsing data was cleared. */
    fun clearAll() {
        temporary.clear()
        permanent.clear()
        save(emptyMap())
    }
}
