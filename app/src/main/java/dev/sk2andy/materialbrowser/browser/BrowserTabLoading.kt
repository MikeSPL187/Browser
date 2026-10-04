package dev.sk2andy.materialbrowser.browser

/** The tab as a new load of [url] starts: loading from zero, with the last failure cleared. */
internal fun BrowserTab.startingLoad(url: String = this.url): BrowserTab = copy(
    url = url,
    isLoading = true,
    progress = 0,
    error = null,
    failureKind = null,
    httpStatusCode = null,
)
