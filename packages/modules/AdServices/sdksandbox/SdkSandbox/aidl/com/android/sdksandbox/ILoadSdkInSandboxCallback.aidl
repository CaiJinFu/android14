/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.sdksandbox;

import android.os.Bundle;

import com.android.sdksandbox.ISdkSandboxManagerToSdkSandboxCallback;
import android.app.sdksandbox.SandboxedSdk;
import android.app.sdksandbox.LoadSdkException;
import com.android.sdksandbox.SandboxLatencyInfo;

/** @hide */
oneway interface ILoadSdkInSandboxCallback {
    const int LOAD_SDK_ALREADY_LOADED = 1;
    const int LOAD_SDK_PROVIDER_INIT_ERROR = 2;
    const int LOAD_SDK_NOT_FOUND = 3;
    const int LOAD_SDK_INSTANTIATION_ERROR = 4;
    const int LOAD_SDK_SDK_DEFINED_ERROR = 5;
    const int LOAD_SDK_INTERNAL_ERROR = 6;

    void onLoadSdkSuccess(in SandboxedSdk sandboxedSdk, in ISdkSandboxManagerToSdkSandboxCallback callback, in SandboxLatencyInfo sandboxLatencyInfo);
    void onLoadSdkError(in LoadSdkException exception, in SandboxLatencyInfo sandboxLatencyInfo);
}
