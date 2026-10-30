package dev.sk2andy.materialbrowser.ui.passwords

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import dev.sk2andy.materialbrowser.ui.theme.v5.V5Motion

/** How one page of the passwords gives way to the next. */
internal enum class PasswordsTransition {
    /** The vault just opened: the lock grows and fades, the passwords rise in under it. */
    Unlock,

    /** The vault closed over an open page: the reverse of [Unlock]. */
    Lock,

    /** Any other step: a cross-fade, as the page menu's pages. */
    Step,
}

/**
 * The [ContentTransform] for [transition]. Without system animations every step is the 150 ms
 * fade of [V5Motion.reducedFade]: nothing moves or scales.
 */
internal fun passwordsTransform(transition: PasswordsTransition, animated: Boolean): ContentTransform {
    if (!animated) return fadeIn(V5Motion.reducedFade()) togetherWith fadeOut(V5Motion.reducedFade())
    return when (transition) {
        PasswordsTransition.Unlock ->
            (fadeIn(V5Motion.effects.spec()) + scaleIn(V5Motion.vaultUnlock.spec(), V5Motion.VAULT_UNLOCK_START_SCALE))
                .togetherWith(
                    fadeOut(V5Motion.effectsFast.spec()) +
                        scaleOut(V5Motion.vaultUnlock.spec(), V5Motion.VAULT_UNLOCK_LOCK_SCALE),
                )
        PasswordsTransition.Lock ->
            (fadeIn(V5Motion.effects.spec()) + scaleIn(V5Motion.vaultUnlock.spec(), V5Motion.VAULT_UNLOCK_LOCK_SCALE))
                .togetherWith(
                    fadeOut(V5Motion.effectsFast.spec()) +
                        scaleOut(V5Motion.vaultUnlock.spec(), V5Motion.VAULT_UNLOCK_START_SCALE),
                )
        PasswordsTransition.Step -> fadeIn(V5Motion.effects.spec()) togetherWith fadeOut(V5Motion.effects.spec())
    }
}
