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

package android.car.media;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.car.CarOccupantZoneManager;
import android.car.annotation.ApiRequirements;

/**
 * Interface to listen for audio media requests in primary zone
 *
 * @hide
 */
@ApiRequirements(minCarVersion = ApiRequirements.CarVersion.UPSIDE_DOWN_CAKE_0,
        minPlatformVersion = ApiRequirements.PlatformVersion.UPSIDE_DOWN_CAKE_0)
@SystemApi
public interface PrimaryZoneMediaAudioRequestCallback {

    /**
     * Called on request for media for occupant in primary zone
     *
     * @param info Occupant zone information that should be shared in car primary zone
     * @param requestId Unique id of the request that can be used to enable the audio sharing
     */
    @ApiRequirements(minCarVersion = ApiRequirements.CarVersion.UPSIDE_DOWN_CAKE_0,
            minPlatformVersion = ApiRequirements.PlatformVersion.UPSIDE_DOWN_CAKE_0)
    void onRequestMediaOnPrimaryZone(@NonNull CarOccupantZoneManager.OccupantZoneInfo info,
            long requestId);

    /**
     * Called on media request status changes
     *
     * @param info Occupant zone of the original request source
     * @param requestId Request id whose status has changed
     * @param status New status of the request, can be any of:
     *                {@link android.car.media.CarAudioManager#AUDIO_REQUEST_STATUS_APPROVED},
     *                {@link android.car.media.CarAudioManager#AUDIO_REQUEST_STATUS_REJECTED},
     *                {@link android.car.media.CarAudioManager#AUDIO_REQUEST_STATUS_CANCELLED},
     *                {@link android.car.media.CarAudioManager#AUDIO_REQUEST_STATUS_STOPPED}
     */
    @ApiRequirements(minCarVersion = ApiRequirements.CarVersion.UPSIDE_DOWN_CAKE_0,
            minPlatformVersion = ApiRequirements.PlatformVersion.UPSIDE_DOWN_CAKE_0)
    void onMediaAudioRequestStatusChanged(@NonNull CarOccupantZoneManager.OccupantZoneInfo info,
            long requestId, @CarAudioManager.MediaAudioRequestStatus int status);
}