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

package com.android.permissioncontroller.safetycenter.service;

import android.app.job.JobService;
import android.provider.DeviceConfig;

import java.time.Duration;

/** A class so that the Safety Center {@link JobService} can access {@link DeviceConfig} flags. */
public class SafetyCenterJobServiceFlags {
    private static final Duration DEFAULT_PERIODIC_BACKGROUND_REFRESH_INTERVAL = Duration.ofDays(1);
    private static final String PROPERTY_BACKGROUND_REFRESH_IS_ENABLED =
            "safety_center_background_refresh_is_enabled";
    private static final String PROPERTY_BACKGROUND_REFRESH_REQUIRES_CHARGING =
            "safety_center_background_requires_charging";
    private static final String PROPERTY_PERIODIC_BACKGROUND_REFRESH_INTERVAL_MILLIS =
            "safety_center_periodic_background_interval_millis";

    /** Returns whether background refreshes should be enabled. */
    static boolean areBackgroundRefreshesEnabled() {
        return DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_PRIVACY, PROPERTY_BACKGROUND_REFRESH_IS_ENABLED, true);
    }

    /**
     * Returns the interval that should be used when scheduling periodic background refresh jobs.
     */
    static Duration getPeriodicBackgroundRefreshInterval() {
        return Duration.ofMillis(
                DeviceConfig.getLong(
                        DeviceConfig.NAMESPACE_PRIVACY,
                        PROPERTY_PERIODIC_BACKGROUND_REFRESH_INTERVAL_MILLIS,
                        DEFAULT_PERIODIC_BACKGROUND_REFRESH_INTERVAL.toMillis()));
    }

    /**
     * Returns whether we should constrain background refresh jobs to only run when the device is
     * charging.
     */
    static boolean getBackgroundRefreshRequiresCharging() {
        return DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_PRIVACY,
                PROPERTY_BACKGROUND_REFRESH_REQUIRES_CHARGING,
                true);
    }
}
