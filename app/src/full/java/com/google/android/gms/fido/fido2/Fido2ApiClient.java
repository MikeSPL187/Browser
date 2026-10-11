/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.google.android.gms.fido.fido2;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;

/** Vola stub; see {@link Task}. No platform authenticator through Google Play services. */
public class Fido2ApiClient {
    @NonNull
    public Task<Boolean> isUserVerifyingPlatformAuthenticatorAvailable() {
        return Task.unavailable();
    }
}
