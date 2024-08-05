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

package com.android.permissioncontroller.role.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.permissioncontroller.role.ui.TwoTargetPreference;
import com.android.permissioncontroller.role.ui.behavior.RoleUiBehavior;
import com.android.role.controller.model.Role;

/**
 * Utility methods for Role UI behavior
 */
public final class RoleUiBehaviorUtils {

    private static final String LOG_TAG = RoleUiBehaviorUtils.class.getSimpleName();

    /**
     * Get the role ui behavior if available
     */
    @Nullable
    private static RoleUiBehavior getUiBehavior(@NonNull Role role) {
        String uiBehaviorName = role.getUiBehaviorName();
        if (uiBehaviorName == null) {
            return null;
        }
        RoleUiBehavior uiBehavior;
        String uiBehaviorClassName = RoleUiBehavior.class.getPackage().getName() + '.'
                + uiBehaviorName;
        try {
            uiBehavior = (RoleUiBehavior) Class.forName(uiBehaviorClassName).newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            Log.e(LOG_TAG, "Unable to instantiate UI behavior: " + uiBehaviorClassName, e);
            return null;
        }
        return uiBehavior;
    }

    /**
     * @see RoleUiBehavior#isVisibleAsUser
     */
    public static boolean isVisibleAsUser(@NonNull Role role, @NonNull UserHandle user,
            @NonNull Context context) {
        RoleUiBehavior uiBehavior = getUiBehavior(role);
        if (uiBehavior == null) {
            return role.isVisible();
        }
        return role.isVisible() && uiBehavior.isVisibleAsUser(role, user, context);
    }

    /**
     * Check whether this role should be visible to user, for current user.
     *
     * @param context the `Context` to retrieve system services
     *
     * @return whether this role should be visible to user.
     */
    public static boolean isVisible(@NonNull Role role, @NonNull Context context) {
        return isVisibleAsUser(role, Process.myUserHandle(), context);
    }


    /**
     * @see RoleUiBehavior#getManageIntentAsUser
     */
    @Nullable
    public static Intent getManageIntentAsUser(@NonNull Role role, @NonNull UserHandle user,
            @NonNull Context context) {
        RoleUiBehavior uiBehavior = getUiBehavior(role);
        if (uiBehavior == null) {
            return null;
        }
        return uiBehavior.getManageIntentAsUser(role, user, context);
    }

    /**
     * @see RoleUiBehavior#preparePreferenceAsUser
     */
    public static void preparePreferenceAsUser(@NonNull Role role,
            @NonNull TwoTargetPreference preference, @NonNull UserHandle user,
            @NonNull Context context) {
        RoleUiBehavior uiBehavior = getUiBehavior(role);
        if (uiBehavior == null) {
            return;
        }
        uiBehavior.preparePreferenceAsUser(role, preference, user, context);
    }

    /**
     * @see RoleUiBehavior#isApplicationVisibleAsUser
     */
    public static boolean isApplicationVisibleAsUser(@NonNull Role role,
            @NonNull ApplicationInfo applicationInfo, @NonNull UserHandle user,
            @NonNull Context context) {
        RoleUiBehavior uiBehavior = getUiBehavior(role);
        if (uiBehavior == null) {
            return true;
        }
        return uiBehavior.isApplicationVisibleAsUser(role, applicationInfo, user, context);
    }

    /**
     * @see RoleUiBehavior#prepareApplicationPreferenceAsUser
     */
    public static void prepareApplicationPreferenceAsUser(@NonNull Role role,
            @NonNull Preference preference, @NonNull ApplicationInfo applicationInfo,
            @NonNull UserHandle user, @NonNull Context context) {
        RoleUiBehavior uiBehavior = getUiBehavior(role);
        if (uiBehavior == null) {
            return;
        }
        uiBehavior.prepareApplicationPreferenceAsUser(role, preference, applicationInfo, user,
                context);
    }

    /**
     * @see RoleUiBehavior#getConfirmationMessage
     */
    @Nullable
    public static CharSequence getConfirmationMessage(@NonNull Role role,
            @NonNull String packageName, @NonNull Context context) {
        RoleUiBehavior uiBehavior = getUiBehavior(role);
        if (uiBehavior == null) {
            return null;
        }
        return uiBehavior.getConfirmationMessage(role, packageName, context);
    }
}
