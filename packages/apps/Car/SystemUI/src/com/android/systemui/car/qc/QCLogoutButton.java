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

package com.android.systemui.car.qc;

import static android.os.UserHandle.USER_NULL;
import static android.view.WindowInsets.Type.statusBars;

import static com.android.systemui.car.users.CarSystemUIUserUtil.getCurrentUserHandle;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.IActivityManager;
import android.car.Car;
import android.car.CarOccupantZoneManager;
import android.car.app.CarActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.systemui.R;

import java.util.List;

/**
 * One of {@link QCFooterButtonView} for quick control panels, which logs out the user.
 */

public class QCLogoutButton extends QCFooterButtonView {
    private static final String TAG = QCUserPickerButton.class.getSimpleName();

    private CarActivityManager mCarActivityManager;
    private CarOccupantZoneManager mCarOccupantZoneManager;

    private float mConfirmLogoutDialogDimValue = -1.0f;

    private final DialogInterface.OnClickListener mOnDialogClickListener = (dialog, which) -> {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            dialog.dismiss();
            logoutUser();
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            dialog.dismiss();
        }
    };

    public QCLogoutButton(Context context) {
        this(context, null);
    }

    public QCLogoutButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QCLogoutButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public QCLogoutButton(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mCarServiceLifecycleListener = (car, ready) -> {
            if (!ready) {
                return;
            }
            mCarActivityManager = (CarActivityManager) car.getCarManager(
                    Car.CAR_ACTIVITY_SERVICE);
            mCarOccupantZoneManager = car.getCarManager(CarOccupantZoneManager.class);
        };

        Car.createCar(getContext(), /* handler= */ null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER,
                mCarServiceLifecycleListener);

        mOnClickListener = v -> showDialog();
        setOnClickListener(mOnClickListener);
    }

    private void showDialog() {
        Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        getContext().sendBroadcastAsUser(intent, getCurrentUserHandle(getContext(), mUserTracker));
        AlertDialog dialog = createDialog();

        // Sets window flags for the SysUI dialog
        applyCarSysUIDialogFlags(dialog);
        dialog.show();
    }

    @VisibleForTesting
    AlertDialog createDialog() {
        Context context = getContext().createWindowContext(getContext().getDisplay(),
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL, null);
        return new AlertDialog.Builder(context,
                com.android.internal.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.user_logout_title)
                .setMessage(R.string.user_logout_message)
                .setNegativeButton(android.R.string.cancel, mOnDialogClickListener)
                .setPositiveButton(R.string.user_logout, mOnDialogClickListener)
                .create();
    }

    private void applyCarSysUIDialogFlags(AlertDialog dialog) {
        final Window window = dialog.getWindow();
        window.setType(WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        window.getAttributes().setFitInsetsTypes(
                window.getAttributes().getFitInsetsTypes() & ~statusBars());
        if (mConfirmLogoutDialogDimValue < 0) {
            mConfirmLogoutDialogDimValue = getContext().getResources().getFloat(
                    R.dimen.confirm_logout_dialog_dim);
        }
        window.setDimAmount(mConfirmLogoutDialogDimValue);
    }

    private void logoutUser() {
        Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        getContext().sendBroadcastAsUser(intent, getCurrentUserHandle(getContext(), mUserTracker));

        if (mUserTracker != null) {
            int userId = mUserTracker.getUserId();
            if (userId != USER_NULL) {
                int displayId = getContext().getDisplayId();
                CarOccupantZoneManager.OccupantZoneInfo zoneInfo = getOccupantZoneForDisplayId(
                        displayId);
                if (zoneInfo == null) {
                    Log.e(TAG,
                            "Cannot find occupant zone info associated with display " + displayId);
                    return;
                }

                // Unassign the user from the occupant zone.
                // TODO(b/253264316): See if we can move it to CarUserService.
                int result = mCarOccupantZoneManager.unassignOccupantZone(zoneInfo);
                if (result != android.car.CarOccupantZoneManager.USER_ASSIGNMENT_RESULT_OK) {
                    Log.e(TAG, "failed to unassign user " + userId + " from occupant zone "
                            + zoneInfo.zoneId);
                    return;
                }

                IActivityManager am = ActivityManager.getService();
                try {
                    // Use stopUserWithDelayedLocking instead of stopUser
                    // to make the call more efficient.
                    am.stopUserWithDelayedLocking(userId, /* force= */ false, /* callback= */ null);
                } catch (RemoteException e) {
                    Log.e(TAG, "Cannot stop user " + userId);
                    return;
                }
                openUserPicker();
            }
        }
    }

    // TODO(b/248608281): Use API from CarOccupantZoneManager for convenience.
    @Nullable
    private CarOccupantZoneManager.OccupantZoneInfo getOccupantZoneForDisplayId(int displayId) {
        List<CarOccupantZoneManager.OccupantZoneInfo> occupantZoneInfos =
                mCarOccupantZoneManager.getAllOccupantZones();
        for (int index = 0; index < occupantZoneInfos.size(); index++) {
            CarOccupantZoneManager.OccupantZoneInfo occupantZoneInfo = occupantZoneInfos.get(index);
            List<Display> displays = mCarOccupantZoneManager.getAllDisplaysForOccupant(
                    occupantZoneInfo);
            for (int displayIndex = 0; displayIndex < displays.size(); displayIndex++) {
                if (displays.get(displayIndex).getDisplayId() == displayId) {
                    return occupantZoneInfo;
                }
            }
        }
        return null;
    }

    private void openUserPicker() {
        if (mCarActivityManager != null) {
            mCarActivityManager.startUserPickerOnDisplay(getContext().getDisplayId());
        }
    }

    @VisibleForTesting
    protected DialogInterface.OnClickListener getOnDialogClickListener() {
        return mOnDialogClickListener;
    }
}
