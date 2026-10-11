/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.google.android.gms.fido.fido2.api.common;

import androidx.annotation.NonNull;

/** Vola stub; see {@link com.google.android.gms.tasks.Task}. */
public enum PublicKeyCredentialType {
    PUBLIC_KEY("public-key");

    private final String value;

    PublicKeyCredentialType(final String value) {
        this.value = value;
    }

    @NonNull
    @Override
    public String toString() {
        return value;
    }
}
