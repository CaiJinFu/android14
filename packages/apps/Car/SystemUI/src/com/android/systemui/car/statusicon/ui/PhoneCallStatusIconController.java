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

package com.android.systemui.car.statusicon.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;

import com.android.systemui.R;
import com.android.systemui.car.statusicon.StatusIconController;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.settings.UserTracker;

import javax.inject.Inject;

/**
 * A controller for the read-only icon that shows phone call active status.
 */
public class PhoneCallStatusIconController extends StatusIconController {

    private static final String TAG = PhoneCallStatusIconController.class.getSimpleName();

    private final TelecomManager mTelecomManager;

    final BroadcastReceiver mPhoneStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
                updateStatus();
            }
        }
    };

    private final UserTracker.Callback mUserChangedCallback = new UserTracker.Callback() {
        @Override
        public void onUserChanged(int newUser, @NonNull Context userContext) {
            updateStatus();
        }
    };

    @Inject
    PhoneCallStatusIconController(
            Context context,
            @Main Resources resources,
            UserTracker userTracker) {
        mTelecomManager = context.getSystemService(TelecomManager.class);
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        context.registerReceiverForAllUsers(mPhoneStateChangeReceiver,
                filter,  /* broadcastPermission= */ null, /* scheduler= */ null);
        userTracker.addCallback(mUserChangedCallback, context.getMainExecutor());
        setIconDrawableToDisplay(resources.getDrawable(R.drawable.ic_phone, context.getTheme()));
        updateStatus();
    }

    @Override
    protected void updateStatus() {
        setIconVisibility(mTelecomManager.isInCall());
        onStatusUpdated();
    }

    @Override
    protected int getId() {
        return R.id.qc_phone_call_status_icon;
    }
}
