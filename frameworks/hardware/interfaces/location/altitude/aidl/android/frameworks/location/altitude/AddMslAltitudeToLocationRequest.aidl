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

package android.frameworks.location.altitude;

@VintfStability
parcelable AddMslAltitudeToLocationRequest {

    /** @see android.location.Location#getLatitude() */
    double latitudeDegrees;

    /** @see android.location.Location#getLongitude() */
    double longitudeDegrees;

    /** @see android.location.Location#getAltitude() */
    double altitudeMeters;

    /** @see android.location.Location#getVerticalAccuracyMeters() */
    float verticalAccuracyMeters;
}
