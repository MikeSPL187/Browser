package dev.sk2andy.materialbrowser.browser.sync.webdav

import dev.sk2andy.materialbrowser.browser.transfer.AndroidLocalTransferCrypto
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncCrypto
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncFormat
import dev.sk2andy.materialbrowser.shared.transfer.LocalTransferCrypto
import dev.sk2andy.materialbrowser.sync.AndroidArgon2RecoveryKeyDeriver
import dev.sk2andy.materialbrowser.sync.SyncBase64
import dev.sk2andy.materialbrowser.sync.SyncRecoveryKdf

/**
 * [WebDavSyncCrypto] on the platform: the local transfer's `javax.crypto` primitives and the
 * Argon2id the Candy sync already uses, at the WebDAV sync's fixed cost.
 */
internal object AndroidWebDavSyncCrypto : WebDavSyncCrypto, LocalTransferCrypto by AndroidLocalTransferCrypto {
    private val deriver = AndroidArgon2RecoveryKeyDeriver()

    override fun argon2id(phrase: ByteArray, salt: ByteArray): ByteArray = deriver.derive(
        phrase,
        SyncRecoveryKdf(
            salt = SyncBase64.encode(salt),
            memoryKiB = WebDavSyncFormat.KDF_MEMORY_KIB,
            iterations = WebDavSyncFormat.KDF_ITERATIONS,
            parallelism = WebDavSyncFormat.KDF_PARALLELISM,
        ),
    )
}
