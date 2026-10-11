/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.google.android.gms.fido.fido2.api.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.fido.common.Transport;
import java.util.List;

/** Vola stub; see {@link PublicKeyCredentialCreationOptions}. First call on the sign-in fallback. */
public class PublicKeyCredentialDescriptor {
    public PublicKeyCredentialDescriptor(
            @NonNull final String type,
            @NonNull final byte[] id,
            @Nullable final List<Transport> transports) {
        throw new UnsupportedOperationException("Google Play services FIDO is not available in Vola");
    }
}
