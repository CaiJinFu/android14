/**
 * Copyright (c) 2023, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.devicelockcontroller.storage;

/**
 * Binder interface to access global parameters.
 * {@hide}
 */
interface IGlobalParametersService {
    void clear();
    List<String> getLockTaskAllowlist();
    void setLockTaskAllowlist(in List<String> allowlist);
    boolean needCheckIn();
    void setNeedCheckIn(boolean needCheckIn);
    String getRegisteredDeviceId();
    void setRegisteredDeviceId(String registeredDeviceId);
    boolean isProvisionForced();
    void setProvisionForced(boolean isForced);
    String getEnrollmentToken();
    void setEnrollmentToken(String token);
    String getKioskSignature();
    void setKioskSignature(String signature);
    int getLastReceivedProvisionState();
    void setLastReceivedProvisionState(int provisionState);
}