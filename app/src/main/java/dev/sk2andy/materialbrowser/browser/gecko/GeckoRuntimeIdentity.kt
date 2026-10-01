package dev.sk2andy.materialbrowser.browser.gecko

import org.mozilla.geckoview.BuildConfig as GeckoBuildConfig

internal fun currentGeckoIdentity(): String =
    "org.mozilla.geckoview@${GeckoBuildConfig.MOZ_APP_VERSION}"
