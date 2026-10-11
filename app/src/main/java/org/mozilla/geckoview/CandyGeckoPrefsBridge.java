package org.mozilla.geckoview;

import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

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
    /** One preference object per name and settings, so a runtime pref is registered only once. */
    private static final Map<GeckoRuntimeSettings, Map<String, RuntimeSettings.Pref<String>>>
            sRuntimeStringPrefs = new WeakHashMap<>();

    private CandyGeckoPrefsBridge() {}

    /**
     * Sets {@code name} to {@code value} on the runtime that uses {@code settings}, now if it runs and
     * at its start otherwise. For preferences that change while Vola runs.
     */
    public static synchronized void commitRuntimePref(
            @NonNull final GeckoRuntimeSettings settings,
            @NonNull final String name,
            @NonNull final String value) {
        Map<String, RuntimeSettings.Pref<String>> prefs = sRuntimeStringPrefs.get(settings);
        if (prefs == null) {
            prefs = new HashMap<>();
            sRuntimeStringPrefs.put(settings, prefs);
        }
        RuntimeSettings.Pref<String> pref = prefs.get(name);
        if (pref == null) {
            pref = settings.new PrefWithoutDefault<String>(name);
            prefs.put(name, pref);
        }
        pref.commit(value);
    }

    /** Sets {@code name} to {@code value} for the runtime about to be created with {@code settings}. */
    public static void setStartupPref(
            @NonNull final GeckoRuntimeSettings settings,
            @NonNull final String name,
            final boolean value) {
        settings.new Pref<Boolean>(name, value);
    }
}
