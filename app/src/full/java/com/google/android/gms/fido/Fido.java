/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.google.android.gms.fido;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.gms.fido.fido2.Fido2ApiClient;
import com.google.android.gms.fido.fido2.Fido2PrivilegedApiClient;

/**
 * Vola stub of the Google Play services FIDO entry point; see {@link com.google.android.gms.tasks.Task}.
 * Its clients answer that no platform authenticator exists and refuse every request.
 */
public final class Fido {
    private Fido() {}

    @NonNull
    public static Fido2ApiClient getFido2ApiClient(@NonNull final Context context) {
        return new Fido2ApiClient();
    }

    @NonNull
    public static Fido2PrivilegedApiClient getFido2PrivilegedApiClient(@NonNull final Context context) {
        return new Fido2PrivilegedApiClient();
    }
}
