/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.adservices.ohttp;

import com.android.internal.annotations.VisibleForTesting;

/** Holds the reference to the native KEM algorithm */
public class KemNativeRef extends NativeRef {
    private KemNativeRef(ReferenceManager referenceManager) {
        super(referenceManager);
    }

    /** Returns the reference to the KEM algorithm DHKEM(X25519, HKDF-SHA256) */
    public static KemNativeRef getHpkeKemDhkemX25519HkdfSha256Reference() {
        return getHpkeKemDhkemX25519HkdfSha256Reference(OhttpJniWrapper.getInstance());
    }

    @VisibleForTesting
    static KemNativeRef getHpkeKemDhkemX25519HkdfSha256Reference(IOhttpJniWrapper ohttpJniWrapper) {
        ReferenceManager referenceManager =
                new ReferenceManager() {
                    @Override
                    public long getOrCreate() {
                        return ohttpJniWrapper.hpkeKemDhkemX25519HkdfSha256();
                    }

                    @Override
                    public void doRelease(long address) {
                        // do nothing;
                    }
                };
        return new KemNativeRef(referenceManager);
    }
}
