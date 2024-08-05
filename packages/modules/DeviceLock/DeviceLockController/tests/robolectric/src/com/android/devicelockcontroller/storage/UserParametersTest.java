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

package com.android.devicelockcontroller.storage;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.devicelockcontroller.policy.DeviceStateController.DeviceState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class UserParametersTest {
    private Context mContext;
    private static final String PACKAGE_OVERRIDING_HOME = "com.home.package";


    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void getDeviceState_shouldReturnExpectedCurrentDeviceState() {
        assertThat(UserParameters.getDeviceState(mContext)).isEqualTo(DeviceState.UNPROVISIONED);
        UserParameters.setDeviceState(mContext, DeviceState.SETUP_SUCCEEDED);
        assertThat(UserParameters.getDeviceState(mContext)).isEqualTo(DeviceState.SETUP_SUCCEEDED);
    }

    @Test
    public void getPackageOverridingHome_shouldReturnExpectedOverridingHomePackage() {
        assertThat(UserParameters.getPackageOverridingHome(mContext)).isNull();
        UserParameters.setPackageOverridingHome(mContext, PACKAGE_OVERRIDING_HOME);
        assertThat(UserParameters.getPackageOverridingHome(mContext))
                .isEqualTo(PACKAGE_OVERRIDING_HOME);
    }
}
