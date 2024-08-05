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

package com.android.adservices.errorlogging;

import android.annotation.NonNull;

/** Interface for Adservices error logger. */
public interface AdServicesErrorLogger {

    /**
     * Creates value {@link AdServicesErrorStats} object and logs AdServices error/exceptions if
     * flag enabled.
     */
    void logError(
            int errorCode, int ppapiName, @NonNull String className, @NonNull String methodName);

    /**
     * Creates value {@link AdServicesErrorStats} object that contains exception information and
     * logs AdServices error/exceptions if flag enabled.
     */
    void logErrorWithExceptionInfo(@NonNull Throwable tr, int errorCode, int ppapiName);
}
