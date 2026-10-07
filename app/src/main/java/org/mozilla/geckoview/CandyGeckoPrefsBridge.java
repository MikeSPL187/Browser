package org.mozilla.geckoview;

import androidx.annotation.NonNull;

/**
 * Sets Gecko preferences that GeckoView has no public setter for.
 *
 * <p>{@link RuntimeSettings.Pref} is package-private; a preference created on the settings before
 * {@link GeckoRuntime#create} reaches Gecko with the other startup preferences. Keeping this bridge
 * in GeckoView's package gives a compile-checked path without reflection or a configuration file,
 * which GeckoView reads through SnakeYAML reflection that minification can break. Remove a
 * preference here when GeckoView exposes a public setter for it.
 */
public final class CandyGeckoPrefsBridge {
    private CandyGeckoPrefsBridge() {}

    /** Sets {@code name} to {@code value} for the runtime about to be created with {@code settings}. */
    public static void setStartupPref(
            @NonNull final GeckoRuntimeSettings settings,
            @NonNull final String name,
            final boolean value) {
        settings.new Pref<Boolean>(name, value);
    }
}
