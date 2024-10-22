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

package android.car.builtin.view;

import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.annotation.SystemApi;
import android.car.builtin.annotation.AddedIn;
import android.car.builtin.annotation.PlatformVersion;
import android.os.Build;
import android.view.InputEvent;

/**
 * Provides access to {@code android.view.InputEvent} calls.
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class InputEventHelper {

    private InputEventHelper() {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the display id for the input event passed as argument.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @AddedIn(PlatformVersion.UPSIDE_DOWN_CAKE_0)
    public static void setDisplayId(@NonNull InputEvent inputEvent, int newDisplayId) {
        inputEvent.setDisplayId(newDisplayId);
    }
}
