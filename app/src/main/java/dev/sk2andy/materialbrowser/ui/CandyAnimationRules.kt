package dev.sk2andy.materialbrowser.ui

internal object CandyAnimationRules {
    fun startupAnimationEnabled(
        animationsEnabled: Boolean,
        startupAnimationEnabled: Boolean,
    ): Boolean = animationsEnabled && startupAnimationEnabled
}
