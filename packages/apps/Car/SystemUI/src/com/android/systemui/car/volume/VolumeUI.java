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

package com.android.systemui.car.volume;

import static android.car.media.CarAudioManager.INVALID_AUDIO_ZONE;

import android.car.Car;
import android.car.CarOccupantZoneManager;
import android.car.media.CarAudioManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

import com.android.systemui.CoreStartable;
import com.android.systemui.R;
import com.android.systemui.car.CarServiceProvider;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.volume.VolumeDialogComponent;

import dagger.Lazy;

import java.io.PrintWriter;

import javax.inject.Inject;

/** The entry point for controlling the volume ui in cars. */
@SysUISingleton
public class VolumeUI implements CoreStartable {

    private static final String TAG = "VolumeUI";
    private final Resources mResources;
    private final Handler mMainHandler;
    private final CarServiceProvider mCarServiceProvider;
    private final Lazy<VolumeDialogComponent> mVolumeDialogComponentLazy;
    private final UserTracker mUserTracker;
    private int mAudioZoneId = INVALID_AUDIO_ZONE;

    private final CarAudioManager.CarVolumeCallback mVolumeChangeCallback =
            new CarAudioManager.CarVolumeCallback() {
                @Override
                public void onGroupVolumeChanged(int zoneId, int groupId, int flags) {
                    initVolumeDialogComponent(zoneId, flags);
                }

                @Override
                public void onMasterMuteChanged(int zoneId, int flags) {
                    initVolumeDialogComponent(zoneId, flags);
                }

                private void initVolumeDialogComponent(int zoneId, int flags) {
                    if (mAudioZoneId != zoneId || (flags & AudioManager.FLAG_SHOW_UI) == 0) {
                        // only initialize for current audio zone when show requested
                        return;
                    }
                    if (mVolumeDialogComponent == null) {
                        mMainHandler.post(() -> {
                            mVolumeDialogComponent = mVolumeDialogComponentLazy.get();
                            mVolumeDialogComponent.register();
                        });
                        mCarAudioManager.unregisterCarVolumeCallback(mVolumeChangeCallback);
                    }
                }
            };

    private boolean mEnabled;
    private CarAudioManager mCarAudioManager;
    private VolumeDialogComponent mVolumeDialogComponent;

    @Inject
    public VolumeUI(
            @Main Resources resources,
            @Main Handler mainHandler,
            CarServiceProvider carServiceProvider,
            Lazy<VolumeDialogComponent> volumeDialogComponentLazy,
            UserTracker userTracker
    ) {
        mResources = resources;
        mMainHandler = mainHandler;
        mCarServiceProvider = carServiceProvider;
        mVolumeDialogComponentLazy = volumeDialogComponentLazy;
        mUserTracker = userTracker;
    }

    @Override
    public void start() {
        boolean enableVolumeUi = mResources.getBoolean(R.bool.enable_volume_ui);
        mEnabled = enableVolumeUi;
        if (!mEnabled) return;

        mCarServiceProvider.addListener(car -> {
            if (mCarAudioManager != null) {
                return;
            }

            CarOccupantZoneManager carOccupantZoneManager =
                    (CarOccupantZoneManager) car.getCarManager(Car.CAR_OCCUPANT_ZONE_SERVICE);
            if (carOccupantZoneManager != null) {
                CarOccupantZoneManager.OccupantZoneInfo info =
                        carOccupantZoneManager.getOccupantZoneForUser(mUserTracker.getUserHandle());
                if (info != null) {
                    mAudioZoneId = carOccupantZoneManager.getAudioZoneIdForOccupant(info);
                }
            }

            if (mAudioZoneId == INVALID_AUDIO_ZONE) {
                return;
            }

            mCarAudioManager = (CarAudioManager) car.getCarManager(Car.AUDIO_SERVICE);
            Log.d(TAG, "Registering mVolumeChangeCallback.");
            // This volume call back is never unregistered because CarStatusBar is
            // never destroyed.
            mCarAudioManager.registerCarVolumeCallback(mVolumeChangeCallback);
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (!mEnabled) return;
        if (mVolumeDialogComponent != null) {
            mVolumeDialogComponent.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void dump(PrintWriter pw, String[] args) {
        pw.print("mEnabled="); pw.println(mEnabled);
        if (!mEnabled) return;
        if (mVolumeDialogComponent != null) {
            mVolumeDialogComponent.dump(pw, args);
        }
    }
}