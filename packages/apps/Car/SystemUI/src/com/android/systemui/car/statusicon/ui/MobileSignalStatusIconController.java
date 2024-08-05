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

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.VisibleForTesting;

import com.android.settingslib.graph.SignalDrawable;
import com.android.systemui.R;
import com.android.systemui.car.statusicon.StatusIconController;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.statusbar.connectivity.MobileDataIndicators;
import com.android.systemui.statusbar.connectivity.NetworkController;
import com.android.systemui.statusbar.connectivity.SignalCallback;

import javax.inject.Inject;

/**
 * A controller for the read-only status icon about mobile data.
 */
public class MobileSignalStatusIconController extends StatusIconController implements
        SignalCallback{

    private final Context mContext;
    private final NetworkController mNetworkController;

    private SignalDrawable mMobileSignalIconDrawable;
    private String mMobileSignalContentDescription;

    @Inject
    MobileSignalStatusIconController(
            Context context,
            @Main Resources resources,
            NetworkController networkController) {
        mContext = context;
        mNetworkController = networkController;

        mMobileSignalIconDrawable = new SignalDrawable(mContext);
        mMobileSignalContentDescription = resources.getString(R.string.status_icon_signal_mobile);
        updateStatus();

        mNetworkController.addCallback(this);
    }

    @Override
    protected void updateStatus() {
        setIconDrawableToDisplay(mMobileSignalIconDrawable);
        setIconContentDescription(mMobileSignalContentDescription);
        onStatusUpdated();
    }

    @Override
    public void setMobileDataIndicators(MobileDataIndicators mobileDataIndicators) {
        mMobileSignalIconDrawable.setLevel(mobileDataIndicators.statusIcon.icon);
        updateStatus();
    }

    @VisibleForTesting
    SignalDrawable getMobileSignalIconDrawable() {
        return mMobileSignalIconDrawable;
    }

    @Override
    protected int getId() {
        return R.id.qc_mobile_signal_status_icon;
    }
}
