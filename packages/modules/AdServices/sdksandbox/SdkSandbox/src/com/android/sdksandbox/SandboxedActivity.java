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

package com.android.sdksandbox;

import static android.app.sdksandbox.SdkSandboxManager.EXTRA_SANDBOXED_ACTIVITY_HANDLER;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.sdksandbox.SdkSandboxManager;
import android.app.sdksandbox.sdkprovider.SdkSandboxActivityRegistry;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Window;

import androidx.annotation.RequiresApi;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Activity to start for SDKs running in the SDK Runtime.
 *
 * <p>It should be created when an {@link android.content.Intent} with action name ({@link
 * SdkSandboxManager#ACTION_START_SANDBOXED_ACTIVITY}) is started.
 *
 * @hide
 */
public class SandboxedActivity extends Activity {
    private static final String TAG = "SandboxedActivity";

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        notifySdkOnActivityCreation();
    }

    /**
     * Notify the SDK by calling {@link
     * android.app.sdksandbox.sdkprovider.SdkSandboxActivityHandler#onActivityCreated(Activity)}.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void notifySdkOnActivityCreation() {
        if (this.getIntent() == null
                || this.getIntent().getExtras() == null
                || this.getIntent()
                                .getExtras()
                                .getBinder(SdkSandboxManager.EXTRA_SANDBOXED_ACTIVITY_HANDLER)
                        == null) {
            Log.e(
                    TAG,
                    "Extra params of the intent are missing the IBinder value for the key"
                            + SdkSandboxManager.EXTRA_SANDBOXED_ACTIVITY_HANDLER);
            finish();
            return;
        }
        IBinder token =
                this.getIntent()
                        .getExtras()
                        .getBinder(SdkSandboxManager.EXTRA_SANDBOXED_ACTIVITY_HANDLER);
        SdkSandboxActivityRegistry registry = SdkSandboxActivityRegistry.getInstance();
        try {
            registry.notifyOnActivityCreation(token, this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start the SandboxedActivity and going to finish it: ", e);
            finish();
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    @NonNull
    String getSandboxedActivityHandlerKey() {
        return EXTRA_SANDBOXED_ACTIVITY_HANDLER;
    }
}
