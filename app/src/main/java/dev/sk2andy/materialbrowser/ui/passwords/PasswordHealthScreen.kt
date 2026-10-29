package dev.sk2andy.materialbrowser.ui.passwords

import dev.sk2andy.materialbrowser.shared.credentials.VaultLogin

/** Where the leak check is: off (the default), running, done or failed. */
internal enum class LeakCheckStatus { Off, Checking, Done, Failed }

private fun previewLogin(id: String, origin: String, username: String, password: String) = VaultLogin(
    id = id,
    origin = origin,
    formActionOrigin = null,
    httpRealm = null,
    username = username,
    password = password,
    createdAtMillis = 0,
    updatedAtMillis = 0,
    lastUsedAtMillis = null,
    timesUsed = 0,
)
