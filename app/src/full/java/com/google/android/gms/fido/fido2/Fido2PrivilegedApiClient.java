/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.google.android.gms.fido.fido2;

import androidx.annotation.NonNull;
import com.google.android.gms.fido.fido2.api.common.FidoCredentialDetails;
import com.google.android.gms.tasks.Task;
import java.util.List;

/**
 * Vola stub; see {@link Task}. The official GeckoView build takes this privileged client. Listing
 * stored credentials throws, which GeckoView catches as "no credential in Google Play services".
 */
public class Fido2PrivilegedApiClient {
    @NonNull
    public Task<Boolean> isUserVerifyingPlatformAuthenticatorAvailable() {
        return Task.unavailable();
    }

    @NonNull
    public Task<List<FidoCredentialDetails>> getCredentialList(@NonNull final String rpId) {
        throw new UnsupportedOperationException("Google Play services FIDO is not available in Vola");
    }
}
