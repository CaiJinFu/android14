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

package com.android.server.wm;

import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.annotation.SystemApi;
import android.app.ActivityOptions;
import android.car.builtin.util.Slogf;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link CarActivityInterceptorUpdatable}.
 *
 * @hide
 */
@RequiresApi(UPSIDE_DOWN_CAKE)
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class CarActivityInterceptorUpdatableImpl implements CarActivityInterceptorUpdatable {
    public static final String TAG = CarActivityInterceptorUpdatableImpl.class.getSimpleName();

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final ArrayMap<ComponentName, IBinder> mActivityToRootTaskMap = new ArrayMap<>();
    @GuardedBy("mLock")
    private final Set<IBinder> mKnownRootTasks = new ArraySet<>();
    private final CarActivityInterceptorInterface mBuiltIn;

    public CarActivityInterceptorUpdatableImpl(CarActivityInterceptorInterface builtInInterface) {
        mBuiltIn = builtInInterface;
    }

    @Override
    public ActivityInterceptResultWrapper onInterceptActivityLaunch(
            ActivityInterceptorInfoWrapper info) {
        if (info.getIntent() == null) {
            return null;
        }
        ComponentName componentName = info.getIntent().getComponent();

        synchronized (mLock) {
            int keyIndex = mActivityToRootTaskMap.indexOfKey(componentName);
            if (keyIndex >= 0) {
                IBinder rootTaskToken = mActivityToRootTaskMap.valueAt(keyIndex);
                if (!isRootTaskUserSameAsActivityUser(rootTaskToken, info)) {
                    return null;
                }

                ActivityOptionsWrapper optionsWrapper = info.getCheckedOptions();
                if (optionsWrapper == null) {
                    optionsWrapper = ActivityOptionsWrapper.create(ActivityOptions.makeBasic());
                }
                optionsWrapper.setLaunchRootTask(rootTaskToken);
                return ActivityInterceptResultWrapper.create(info.getIntent(),
                        optionsWrapper.getOptions());
            }
        }
        return null;
    }

    private boolean isRootTaskUserSameAsActivityUser(IBinder rootTaskToken,
            ActivityInterceptorInfoWrapper activityInterceptorInfoWrapper) {
        TaskWrapper rootTask = TaskWrapper.createFromToken(rootTaskToken);
        int userIdFromActivity = activityInterceptorInfoWrapper.getUserId();
        int userIdFromRootTask = mBuiltIn.getUserAssignedToDisplay(rootTask
                .getTaskDisplayArea().getDisplay().getDisplayId());
        if (userIdFromActivity == userIdFromRootTask) {
            return true;
        }
        Slogf.w(TAG, "The user id of launched activity (%d) doesn't match the "
                + "user id which the display (which the root task is added in) is "
                + "assigned to (%d).", userIdFromActivity, userIdFromRootTask);
        return false;
    }

    /**
     * Sets the given {@code activities} to be persistent on the root task corresponding to the
     * given {@code rootTaskToken}.
     * <p>
     * If {@code rootTaskToken} is {@code null}, then the earlier root task associations of the
     * given {@code activities} will be removed.
     *
     * @param activities    the list of activities which have to be persisted.
     * @param rootTaskToken the binder token of the root task which the activities have to be
     *                      persisted on.
     */
    public void setPersistentActivityOnRootTask(@NonNull List<ComponentName> activities,
            IBinder rootTaskToken) {
        synchronized (mLock) {
            if (rootTaskToken == null) {
                int activitiesNum = activities.size();
                for (int i = 0; i < activitiesNum; i++) {
                    mActivityToRootTaskMap.remove(activities.get(i));
                }
                return;
            }

            int activitiesNum = activities.size();
            for (int i = 0; i < activitiesNum; i++) {
                mActivityToRootTaskMap.put(activities.get(i), rootTaskToken);
            }
            if (!mKnownRootTasks.contains(rootTaskToken)) {
                // Seeing the token for the first time, set the listener
                removeRootTaskTokenOnDeath(rootTaskToken);
                mKnownRootTasks.add(rootTaskToken);
            }
        }
    }

    private void removeRootTaskTokenOnDeath(IBinder rootTaskToken) {
        try {
            rootTaskToken.linkToDeath(() -> removeRootTaskToken(rootTaskToken), /* flags= */ 0);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeRootTaskToken(IBinder rootTaskToken) {
        synchronized (mLock) {
            mKnownRootTasks.remove(rootTaskToken);
            // remove all the persistent activities for this root task token from the map,
            // because the root task itself is removed.
            Iterator<Map.Entry<ComponentName, IBinder>> iterator =
                    mActivityToRootTaskMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ComponentName, IBinder> entry = iterator.next();
                if (entry.getValue().equals(rootTaskToken)) {
                    iterator.remove();
                }
            }
        }
    }

    @VisibleForTesting
    public Map<ComponentName, IBinder> getActivityToRootTaskMap() {
        synchronized (mLock) {
            return mActivityToRootTaskMap;
        }
    }
}
