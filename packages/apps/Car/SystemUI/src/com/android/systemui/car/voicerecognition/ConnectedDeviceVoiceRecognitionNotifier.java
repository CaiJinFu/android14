/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.systemui.car.voicerecognition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.CoreStartable;
import com.android.systemui.R;
import com.android.systemui.SysUIToast;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.util.concurrency.DelayableExecutor;

import javax.inject.Inject;

/**
 * Controller responsible for showing toast message when voice recognition over bluetooth device
 * getting activated.
 */
public class ConnectedDeviceVoiceRecognitionNotifier implements CoreStartable {

    private static final String TAG = "CarVoiceRecognition";
    @VisibleForTesting
    static final int INVALID_VALUE = -1;
    @VisibleForTesting
    static final int VOICE_RECOGNITION_STARTED = 1;

    // TODO(b/218911666): {@link BluetoothHeadsetClient.ACTION_AG_EVENT} is a hidden API.
    private static final String HEADSET_CLIENT_ACTION_AG_EVENT =
            "android.bluetooth.headsetclient.profile.action.AG_EVENT";
    // TODO(b/218911666): {@link BluetoothHeadsetClient.EXTRA_VOICE_RECOGNITION} is a hidden API.
    private static final String HEADSET_CLIENT_EXTRA_VOICE_RECOGNITION =
            "android.bluetooth.headsetclient.extra.VOICE_RECOGNITION";

    private final Context mContext;
    private final DelayableExecutor mExecutor;

    private final BroadcastReceiver mVoiceRecognitionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Voice recognition received an intent!");
            }
            if (intent == null
                    || intent.getAction() == null
                    || !HEADSET_CLIENT_ACTION_AG_EVENT.equals(intent.getAction())
                    || !intent.hasExtra(HEADSET_CLIENT_EXTRA_VOICE_RECOGNITION)) {
                return;
            }

            int voiceRecognitionState = intent.getIntExtra(
                    HEADSET_CLIENT_EXTRA_VOICE_RECOGNITION, INVALID_VALUE);

            if (voiceRecognitionState == VOICE_RECOGNITION_STARTED) {
                showToastMessage();
            }
        }
    };

    private void showToastMessage() {
        mExecutor.execute(() -> SysUIToast.makeText(mContext, R.string.voice_recognition_toast,
                Toast.LENGTH_LONG).show());
    }

    @Inject
    public ConnectedDeviceVoiceRecognitionNotifier(
            Context context,
            @Main DelayableExecutor mainExecutor
    ) {
        mContext = context;
        mExecutor = mainExecutor;
    }

    @Override
    public void start() {
    }

    @Override
    public void onBootCompleted() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(HEADSET_CLIENT_ACTION_AG_EVENT);
        mContext.registerReceiverAsUser(mVoiceRecognitionReceiver, UserHandle.ALL, filter,
                /* broadcastPermission= */ null, /* scheduler= */ null);
    }
}
