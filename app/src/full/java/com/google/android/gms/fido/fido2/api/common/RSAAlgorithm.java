/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.google.android.gms.fido.fido2.api.common;

/** Vola stub; see {@link EC2Algorithm}. */
public enum RSAAlgorithm implements Algorithm {
    RS256(-257),
    RS384(-258),
    RS512(-259),
    PS256(-37),
    PS384(-38),
    PS512(-39);

    private final int algoValue;

    RSAAlgorithm(final int algoValue) {
        this.algoValue = algoValue;
    }

    @Override
    public int getAlgoValue() {
        return algoValue;
    }
}
