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

package com.android.server.connectivity.mdns;

import android.annotation.NonNull;
import android.os.Handler;

import com.android.net.module.util.SharedLog;
import com.android.server.connectivity.mdns.internal.SocketNetlinkMonitor;

/**
 * The factory class for creating the netlink monitor.
 */
public class SocketNetLinkMonitorFactory {

    /**
     * Creates a new netlink monitor.
     */
    public static AbstractSocketNetlink createNetLinkMonitor(@NonNull final Handler handler,
            @NonNull SharedLog log, @NonNull MdnsSocketProvider.NetLinkMonitorCallBack cb) {
        return new SocketNetlinkMonitor(handler, log, cb);
    }

    private SocketNetLinkMonitorFactory() {
    }

}
