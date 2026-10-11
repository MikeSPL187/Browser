/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.google.android.gms.tasks;

import androidx.annotation.NonNull;

/** Vola stub of the Google Play services listener; see {@link Task}. */
public interface OnFailureListener {
    void onFailure(@NonNull Exception e);
}
