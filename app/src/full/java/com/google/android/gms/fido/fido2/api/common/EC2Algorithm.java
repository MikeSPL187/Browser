/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.google.android.gms.fido.fido2.api.common;

/**
 * Vola stub; see {@link com.google.android.gms.tasks.Task}. GeckoView lists these when its WebAuthn
 * class loads, so they must exist for Gecko to start the API at all.
 */
public enum EC2Algorithm implements Algorithm {
    ED256(-260),
    ED512(-261),
    ES256(-7),
    ES384(-35),
    ES512(-36);

    private final int algoValue;

    EC2Algorithm(final int algoValue) {
        this.algoValue = algoValue;
    }

    @Override
    public int getAlgoValue() {
        return algoValue;
    }
}
