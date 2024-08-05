
/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.core.uwb.backend;

import androidx.core.uwb.backend.IRangingSessionCallback;
import androidx.core.uwb.backend.RangingCapabilities;
import androidx.core.uwb.backend.RangingParameters;
import androidx.core.uwb.backend.UwbAddress;
import androidx.core.uwb.backend.UwbComplexChannel;

/** Gms Reference: com.google.android.gms.nearby.uwb.UwbClient */
interface IUwbClient {
    boolean isAvailable();
    RangingCapabilities getRangingCapabilities();
    UwbAddress getLocalAddress();
    UwbComplexChannel getComplexChannel();
    void startRanging(in RangingParameters parameters, in IRangingSessionCallback callback);
    void stopRanging(in IRangingSessionCallback callback);
    void addControlee(in UwbAddress address);
    void removeControlee(in UwbAddress address);
}
