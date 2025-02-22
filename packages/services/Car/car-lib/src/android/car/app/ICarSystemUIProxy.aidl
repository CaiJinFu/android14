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
package android.car.app;

import android.view.SurfaceControl;
import android.os.Bundle;
import android.app.PendingIntent;
import android.graphics.Rect;
import android.car.app.ICarTaskViewClient;
import android.car.app.ICarTaskViewHost;


/**
 * Binder API for {@link CarSystemUIProxy}
 * @hide
 */
interface ICarSystemUIProxy {
    /**
     * Creates the host side of the task view and links the provided {@code carTaskVIewClient}
     * to the same.
     *
     * @return a handle to the host side of task view.
     */
    ICarTaskViewHost createControlledCarTaskView(in ICarTaskViewClient client);
}