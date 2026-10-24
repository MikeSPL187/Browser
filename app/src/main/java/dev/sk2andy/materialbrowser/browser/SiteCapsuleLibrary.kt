package dev.sk2andy.materialbrowser.browser

import dev.sk2andy.materialbrowser.capsule.CapsuleIconColor
import dev.sk2andy.materialbrowser.capsule.CapsuleIconMode
import dev.sk2andy.materialbrowser.capsule.CapsuleIconUpdateRules
import dev.sk2andy.materialbrowser.capsule.SiteCapsule
import dev.sk2andy.materialbrowser.capsule.SiteCapsuleDraft
import dev.sk2andy.materialbrowser.capsule.SiteCapsuleRules

/**
 * The saved site capsules with their icons and home-screen shortcuts (#500): saving one renders
 * its icon and pins or updates its shortcut, deleting one disables the shortcut. Which capsule is
 * open and the workspace checks stay in the controller. [I] is the icon image
 * ([android.graphics.Bitmap] in the app).
 */
internal class SiteCapsuleLibrary<I : Any>(
    /** The capsules the screens show; shared with the controller, written only here. */
    val capsules: MutableList<SiteCapsule>,
    private val store: Store,
    private val icons: Icons<I>,
    private val shortcuts: Shortcuts<I>,
    private val renderIcon: (
        name: String,
        iconEmoji: String,
        iconColor: CapsuleIconColor,
        favicon: I?,
        customIcon: I?,
    ) -> I,
    private val sameImage: (I, I) -> Boolean,
    private val newId: () -> String,
    private val now: () -> Long,
) {
    interface Store {
        fun load(): List<SiteCapsule>

        fun save(capsules: List<SiteCapsule>)
    }

    /** The rendered icon, the site's favicon and the owner's own picture, per capsule. */
    interface Icons<I> {
        fun load(capsuleId: String): I?

        fun loadSource(capsuleId: String): I?

        fun loadCustom(capsuleId: String): I?

        fun save(capsuleId: String, icon: I): Boolean

        fun saveSource(capsuleId: String, icon: I): Boolean

        fun saveCustom(capsuleId: String, icon: I): Boolean

        fun delete(capsuleId: String)

        fun cleanup(knownCapsuleIds: Set<String>)
    }

    interface Shortcuts<I> {
        fun isPinningSupported(): Boolean

        fun requestPin(capsule: SiteCapsule, icon: I): Boolean

        fun update(capsule: SiteCapsule, icon: I): Boolean

        fun isPinned(capsule: SiteCapsule): Boolean

        /** The capsule is gone: its shortcut stays on the home screen but no longer opens. */
        fun disable(capsule: SiteCapsule)

        fun reportUsed(capsule: SiteCapsule)
    }

    val isPinningSupported: Boolean
        get() = shortcuts.isPinningSupported()

    val canCreate: Boolean
        get() = SiteCapsuleRules.canCreate(capsules.size)

    fun find(capsuleId: String): SiteCapsule? = capsules.firstOrNull { it.id == capsuleId }

    /** After the workspaces load: capsules of a workspace that no longer exists are dropped. */
    fun restore(knownProfileIds: Set<String>) {
        capsules += store.load()
            .filter { capsule -> capsule.profileId in knownProfileIds }
            .let(SiteCapsuleRules::bounded)
        store.save(capsules)
        icons.cleanup(capsules.mapTo(hashSetOf(), SiteCapsule::id))
    }

    fun reportUsed(capsule: SiteCapsule) = shortcuts.reportUsed(capsule)

    /**
     * Saves [draft], new or edited, with its icon, and pins its shortcut or updates the pinned one.
     * [isProfileValid] says whether the chosen workspace may hold a capsule.
     */
    fun upsert(
        draft: SiteCapsuleDraft,
        isProfileValid: Boolean,
        multiProfileSupported: Boolean,
        sourceFavicon: I? = null,
        customIcon: I? = null,
    ): CapsuleSaveResult {
        val existing = draft.id?.let(::find)
        if (existing == null && !SiteCapsuleRules.canCreate(capsules.size)) {
            return CapsuleSaveResult.LimitReached
        }
        if (!isProfileValid) return CapsuleSaveResult.Invalid
        val nowMillis = now()
        val proposedCapsule = if (existing == null) {
            SiteCapsuleRules.create(
                draft = draft,
                id = newId(),
                nowMillis = nowMillis,
                multiProfileSupported = multiProfileSupported,
            )
        } else {
            SiteCapsuleRules.update(
                existing = existing,
                draft = draft,
                nowMillis = nowMillis,
                multiProfileSupported = multiProfileSupported,
            )
        } ?: return CapsuleSaveResult.Invalid
        val storedIcon = icons.load(proposedCapsule.id)
        val storedSourceFavicon = icons.loadSource(proposedCapsule.id)
        val storedCustomIcon = icons.loadCustom(proposedCapsule.id)
        val customIconChanged = customIcon != null && runCatching {
            storedCustomIcon?.let { stored -> sameImage(stored, customIcon) } != true
        }.getOrDefault(true)
        if (
            customIconChanged &&
            !icons.saveCustom(proposedCapsule.id, checkNotNull(customIcon))
        ) return CapsuleSaveResult.IconSaveFailed
        val iconCustomizationChanged = existing == null ||
            existing.iconMode != proposedCapsule.iconMode ||
            existing.iconEmoji != proposedCapsule.iconEmoji ||
            existing.iconColor != proposedCapsule.iconColor
        val capsule = proposedCapsule.copy(
            iconMode = CapsuleIconUpdateRules.resolveMode(
                requestedMode = proposedCapsule.iconMode,
                hasSourceFavicon = sourceFavicon != null || storedSourceFavicon != null,
                hasCustomIcon = customIcon != null || storedCustomIcon != null,
                hasRenderedIcon = storedIcon != null,
                customizationChanged = iconCustomizationChanged,
            ),
        )
        val updated = capsules.filterNot { it.id == capsule.id } + capsule
        capsules.clear()
        capsules += SiteCapsuleRules.bounded(updated)
        store.save(capsules)
        if (sourceFavicon != null) icons.saveSource(capsule.id, sourceFavicon)
        val icon = when {
            capsule.iconMode == CapsuleIconMode.Custom -> renderIcon(
                capsule.name,
                capsule.iconEmoji,
                capsule.iconColor,
                null,
                checkNotNull(customIcon ?: storedCustomIcon),
            )
            capsule.iconMode == CapsuleIconMode.Favicon &&
                sourceFavicon == null && storedSourceFavicon == null &&
                storedIcon != null -> storedIcon
            else -> renderIcon(
                capsule.name,
                capsule.iconEmoji,
                capsule.iconColor,
                if (capsule.iconMode == CapsuleIconMode.Favicon) {
                    sourceFavicon ?: storedSourceFavicon
                } else {
                    null
                },
                null,
            )
        }
        icons.save(capsule.id, icon)
        return if (existing == null || !shortcuts.isPinned(capsule)) {
            if (!shortcuts.isPinningSupported()) {
                CapsuleSaveResult.PinningUnsupported
            } else if (shortcuts.requestPin(capsule, icon)) {
                CapsuleSaveResult.PinRequested
            } else {
                CapsuleSaveResult.PinRequestFailed
            }
        } else if (shortcuts.update(capsule, icon)) {
            CapsuleSaveResult.Updated
        } else {
            CapsuleSaveResult.UpdateFailed
        }
    }

    /** Removes [capsuleId] with its icons and disables its shortcut; false when there is none. */
    fun remove(capsuleId: String): Boolean {
        val capsule = find(capsuleId) ?: return false
        val remaining = capsules.filterNot { it.id == capsuleId }
        capsules.clear()
        capsules += remaining
        store.save(capsules)
        icons.delete(capsuleId)
        shortcuts.disable(capsule)
        return true
    }

    /**
     * [sourceProfileId] is going away: its capsules, but [excludedCapsuleId], move to
     * [fallbackProfileId] and their shortcuts follow with a fresh icon.
     */
    fun reassign(
        sourceProfileId: String,
        fallbackProfileId: String,
        excludedCapsuleId: String? = null,
    ) {
        val affected = capsules.filter {
            it.profileId == sourceProfileId && it.id != excludedCapsuleId
        }
        if (affected.isEmpty()) return
        val nowMillis = now()
        val replacements = affected.associate { capsule ->
            capsule.id to capsule.copy(
                profileId = fallbackProfileId,
                ownsDedicatedProfile = false,
                isolatedStorageRequested = false,
                updatedAtMillis = nowMillis,
            )
        }
        capsules.replaceAll { capsule -> replacements[capsule.id] ?: capsule }
        store.save(capsules)
        replacements.values.forEach { capsule ->
            val plainIcon = { renderIcon(capsule.name, capsule.iconEmoji, capsule.iconColor, null, null) }
            val icon = if (capsule.iconMode == CapsuleIconMode.ProfileFallback) {
                plainIcon()
            } else {
                icons.load(capsule.id) ?: plainIcon()
            }
            icons.save(capsule.id, icon)
            shortcuts.update(capsule, icon)
        }
    }

    fun renderedIcon(capsuleId: String): I? = icons.load(capsuleId)

    fun sourceIcon(capsuleId: String): I? = icons.loadSource(capsuleId)

    fun customIcon(capsuleId: String): I? = icons.loadCustom(capsuleId)
}
