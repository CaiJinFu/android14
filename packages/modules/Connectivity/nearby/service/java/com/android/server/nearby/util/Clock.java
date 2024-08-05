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

package com.android.server.nearby.util;

import android.os.SystemClock;

/** Wrapper interface for time operations. Allows replacement of clock operations for testing. */
public interface Clock {

    /**
     * Get the current time of the clock in milliseconds.
     *
     * @return Current time in milliseconds.
     */
    long currentTimeMillis();

    /**
     * Returns milliseconds since boot, including time spent in sleep.
     *
     * @return Current time since boot in milliseconds.
     */
    long elapsedRealtime();

    /**
     * Returns the current timestamp of the most precise timer available on the local system, in
     * nanoseconds.
     *
     * @return Current time in nanoseconds.
     */
    long nanoTime();

    /**
     * Returns the time spent in the current thread, in milliseconds
     *
     * @return Thread time in milliseconds.
     */
    @SuppressWarnings("StaticOrDefaultInterfaceMethod")
    default long currentThreadTimeMillis() {
        return SystemClock.currentThreadTimeMillis();
    }
}
