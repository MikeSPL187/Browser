/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.google.android.gms.tasks;

import androidx.annotation.NonNull;

/**
 * Vola's stand-in for the Google Play services class of the same name (passkeys, option A).
 *
 * <p>GeckoView calls Google Play services FIDO, which Vola does not ship. These stubs carry only
 * what GeckoView executes, so its WebAuthn code fails cleanly instead of crashing Gecko with a
 * missing class (#127, H1). Nothing here talks to Google. See {@code docs/vola/research/passkeys.md}.
 */
public abstract class Task<TResult> {
    @NonNull
    public abstract Task<TResult> addOnSuccessListener(@NonNull OnSuccessListener<? super TResult> listener);

    @NonNull
    public abstract Task<TResult> addOnFailureListener(@NonNull OnFailureListener listener);

    /** A task that has already failed: failure listeners run at once, success listeners never. */
    @NonNull
    public static <TResult> Task<TResult> unavailable() {
        return new Task<TResult>() {
            @NonNull
            @Override
            public Task<TResult> addOnSuccessListener(@NonNull final OnSuccessListener<? super TResult> listener) {
                return this;
            }

            @NonNull
            @Override
            public Task<TResult> addOnFailureListener(@NonNull final OnFailureListener listener) {
                listener.onFailure(new UnsupportedOperationException("Google Play services FIDO is not available in Vola"));
                return this;
            }
        };
    }
}
