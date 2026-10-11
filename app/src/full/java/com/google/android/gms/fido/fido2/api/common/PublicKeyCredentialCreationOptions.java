/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.google.android.gms.fido.fido2.api.common;

/**
 * Vola stub; see {@link com.google.android.gms.tasks.Task}. Building creation options is the first
 * Google Play services call on GeckoView's fallback path for a new passkey; it throws, and GeckoView
 * turns that into a rejected promise on the page.
 */
public class PublicKeyCredentialCreationOptions {
    private PublicKeyCredentialCreationOptions() {}

    /** Throws: Vola has no Google Play services FIDO to create a passkey with. */
    public static class Builder {
        public Builder() {
            throw new UnsupportedOperationException("Google Play services FIDO is not available in Vola");
        }
    }
}
