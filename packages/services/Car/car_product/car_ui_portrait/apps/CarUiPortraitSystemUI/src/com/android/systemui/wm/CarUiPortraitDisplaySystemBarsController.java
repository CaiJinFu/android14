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

package com.android.systemui.wm;

import static android.car.drivingstate.CarDrivingStateEvent.DRIVING_STATE_MOVING;
import static android.car.drivingstate.CarDrivingStateEvent.DRIVING_STATE_UNKNOWN;
import static android.view.WindowInsets.Type.navigationBars;
import static android.view.WindowInsets.Type.statusBars;

import android.annotation.IntDef;
import android.car.Car;
import android.car.drivingstate.CarDrivingStateEvent;
import android.car.drivingstate.CarDrivingStateManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Slog;
import android.util.SparseArray;
import android.view.IWindowManager;
import android.view.WindowInsets;
import android.view.WindowInsets.Type.InsetsType;
import android.widget.Toast;

import com.android.car.ui.R;
import com.android.wm.shell.common.DisplayController;
import com.android.wm.shell.common.DisplayInsetsController;
import com.android.wm.shell.dagger.WMSingleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Controller that expands upon {@link DisplaySystemBarsController} but allows for immersive
 * mode overrides and notification in other SystemUI classes via the provided methods and callbacks.
 */
@WMSingleton
public class CarUiPortraitDisplaySystemBarsController extends DisplaySystemBarsController {
    private static final String TAG = "CarUiPortraitDisplaySystemBarsController";
    private SparseArray<CarUiPortraitPerDisplay> mCarUiPerDisplaySparseArray;

    private int mCurrentDrivingState = DRIVING_STATE_UNKNOWN;

    private boolean mIsUserSetupInProgress;

    private static final int STATE_DEFAULT = 1;
    private static final int STATE_IMMERSIVE_WITH_NAV_BAR = 2;
    private static final int STATE_IMMERSIVE_WITHOUT_NAV_BAR = 3;

    @IntDef({STATE_DEFAULT,
            STATE_IMMERSIVE_WITH_NAV_BAR,
            STATE_IMMERSIVE_WITHOUT_NAV_BAR})
    private @interface ImmersiveState {
    }

    private final CarDrivingStateManager.CarDrivingStateEventListener mDrivingStateEventListener =
            this::handleDrivingStateChange;

    public CarUiPortraitDisplaySystemBarsController(Context context,
            IWindowManager wmService,
            DisplayController displayController,
            DisplayInsetsController displayInsetsController,
            Handler mainHandler) {
        super(context, wmService, displayController, displayInsetsController, mainHandler);

        Car car = Car.createCar(context);
        if (car != null) {
            CarDrivingStateManager mDrivingStateManager =
                    (CarDrivingStateManager) car.getCarManager(Car.CAR_DRIVING_STATE_SERVICE);
            mDrivingStateManager.registerListener(mDrivingStateEventListener);
            mDrivingStateEventListener.onDrivingStateChanged(
                    mDrivingStateManager.getCurrentCarDrivingState());
        } else {
            Slog.e(TAG, "Failed to initialize car");
        }
    }

    @Override
    public void onDisplayAdded(int displayId) {
        CarUiPortraitPerDisplay pd = new CarUiPortraitPerDisplay(displayId);
        pd.register();
        if (mCarUiPerDisplaySparseArray == null) {
            mCarUiPerDisplaySparseArray = new SparseArray<>();
            BarControlPolicy.reloadFromSetting(mContext);
            BarControlPolicy.registerContentObserver(mContext, mHandler, () -> {
                int size = mCarUiPerDisplaySparseArray.size();
                for (int i = 0; i < size; i++) {
                    mCarUiPerDisplaySparseArray.valueAt(i)
                            .updateDisplayWindowRequestedVisibleTypes();
                }
            });
        }
        mCarUiPerDisplaySparseArray.put(displayId, pd);
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        CarUiPortraitPerDisplay pd = mCarUiPerDisplaySparseArray.get(displayId);
        pd.unregister();
        mCarUiPerDisplaySparseArray.remove(displayId);
    }

    /**
     * Request an immersive mode override for a particular display id. This request will override
     * the usual BarControlPolicy until the package or requested visibilities change.
     */
    public void requestImmersiveMode(int displayId, boolean immersive) {
        CarUiPortraitPerDisplay display = mCarUiPerDisplaySparseArray.get(displayId);
        if (display == null) {
            return;
        }
        display.setImmersiveMode(immersive ? STATE_IMMERSIVE_WITH_NAV_BAR : STATE_DEFAULT);
        if (!immersive) {
            display.setDelayedImmersiveModeWithoutNavBar(false);
        }
    }

    /**
     * Request an immersive mode override for a particular display id specifically for setup wizard.
     * This request will override the usual BarControlPolicy and will persist until explicitly
     * revoked.
     */
    public void requestImmersiveModeForSUW(int displayId, boolean immersive) {
        CarUiPortraitPerDisplay display = mCarUiPerDisplaySparseArray.get(displayId);
        if (display == null) {
            return;
        }
        mIsUserSetupInProgress = immersive;
        display.setImmersiveMode(immersive ? STATE_IMMERSIVE_WITHOUT_NAV_BAR : STATE_DEFAULT);
    }

    /**
     * Register an immersive mode callback for a particular display.
     */
    public void registerCallback(int displayId, Callback callback) {
        CarUiPortraitPerDisplay display = mCarUiPerDisplaySparseArray.get(displayId);
        if (display == null) {
            return;
        }
        display.addCallbackForDisplay(callback);
    }

    /**
     * Unregister an immersive mode callback for a particular display.
     */
    public void unregisterCallback(int displayId, Callback callback) {
        CarUiPortraitPerDisplay display = mCarUiPerDisplaySparseArray.get(displayId);
        if (display == null) {
            return;
        }
        display.removeCallbackForDisplay(callback);
    }

    private void handleDrivingStateChange(CarDrivingStateEvent event) {
        mCurrentDrivingState = event.eventValue;
        if (mCarUiPerDisplaySparseArray != null) {
            for (int i = 0; i < mCarUiPerDisplaySparseArray.size(); i++) {
                mCarUiPerDisplaySparseArray.valueAt(i).onDrivingStateChanged();
            }
        }
    }

    class CarUiPortraitPerDisplay extends DisplaySystemBarsController.PerDisplay {
        private final int[] mDefaultVisibilities =
                new int[]{WindowInsets.Type.systemBars(), 0};
        private final int[] mFullImmersiveVisibilities =
                new int[]{0, WindowInsets.Type.systemBars()};
        // Only hide statusBars
        private final int[] mImmersiveWithNavBarVisibilities = new int[]{
                WindowInsets.Type.navigationBars() | WindowInsets.Type.captionBar()
                        | WindowInsets.Type.systemOverlays(),
                WindowInsets.Type.statusBars()
        };
        private final List<Callback> mCallbacks = new ArrayList<>();
        private @InsetsType int mWindowRequestedVisibleTypes = WindowInsets.Type.defaultVisible();
        private @InsetsType int mAppRequestedVisibleTypes = WindowInsets.Type.defaultVisible();
        private boolean mImmersiveWithNavBar = false;
        private boolean mFullImmersive = false;

        @ImmersiveState
        private int mImmersiveState = STATE_DEFAULT;

        private static final int HIDE_NAVIGATION_BAR_DELAY_IN_MILLIS = 10000;

        private final Runnable mDelayedImmersiveModeWithNavBarRunnable;

        CarUiPortraitPerDisplay(int displayId) {
            super(displayId);

            mDelayedImmersiveModeWithNavBarRunnable = () -> {
                setImmersiveMode(STATE_IMMERSIVE_WITHOUT_NAV_BAR);
                notifyOnImmersiveStateChanged(/* hideNavBar = */ false);
            };
        }

        @Override
        public void topFocusedWindowChanged(ComponentName component,
                @InsetsType int requestedVisibleTypes) {
            if (mIsUserSetupInProgress) {
                Slog.d(TAG, "Don't change system bar visibility when SUW is in progress");
                return;
            }

            boolean requestedVisibilitiesChanged = false;

            if (mWindowRequestedVisibleTypes != requestedVisibleTypes) {
                mWindowRequestedVisibleTypes = requestedVisibleTypes;
                boolean immersive =
                        (mWindowRequestedVisibleTypes & (statusBars() | navigationBars())) == 0;
                notifyOnImmersiveRequestedChanged(component, immersive);
                if (!immersive) {
                    mImmersiveState = STATE_DEFAULT;
                    requestedVisibilitiesChanged = true;
                }
            }
            String packageName = component != null ? component.getPackageName() : null;
            if (Objects.equals(mPackageName, packageName) && !requestedVisibilitiesChanged) {
                return;
            }
            mPackageName = packageName;
            mImmersiveState = STATE_DEFAULT; // reset override when changing application
            updateDisplayWindowRequestedVisibleTypes();
        }

        @Override
        protected void updateDisplayWindowRequestedVisibleTypes() {
            if (mPackageName == null) {
                return;
            }

            int[] barVisibilities = BarControlPolicy.getBarVisibilities(mPackageName);
            //TODO(b/260948168): Check with UX on how to deal with activity resize when changing
            // between STATE_IMMERSIVE_WITHOUT_NAV_BAR and STATE_IMMERSIVE_WITH_NAV_BAR
            switch (mImmersiveState) {
                case STATE_DEFAULT: {
                    Slog.d(TAG, "Update barVisibilities to STATE_DEFAULT");
                    barVisibilities = mDefaultVisibilities;
                    break;
                }
                case STATE_IMMERSIVE_WITHOUT_NAV_BAR: {
                    Slog.d(TAG, "Update barVisibilities to STATE_IMMERSIVE_WITHOUT_NAV_BAR");
                    barVisibilities = mFullImmersiveVisibilities;
                    break;
                }
                case STATE_IMMERSIVE_WITH_NAV_BAR: {
                    Slog.d(TAG, "Update barVisibilities to STATE_IMMERSIVE_WITH_NAV_BAR");
                    barVisibilities = mImmersiveWithNavBarVisibilities;
                    break;
                }
            }

            updateRequestedVisibleTypes(barVisibilities[0], /* visible= */ true);
            updateRequestedVisibleTypes(barVisibilities[1], /* visible= */ false);

            // Return if the requested visibility is already applied.
            if (mAppRequestedVisibleTypes == mRequestedVisibleTypes) {
                return;
            }
            mAppRequestedVisibleTypes = mRequestedVisibleTypes;

            showInsets(barVisibilities[0], /* fromIme= */ false, /* statsToken= */ null);
            hideInsets(barVisibilities[1], /* fromIme= */ false, /* statsToken= */ null);

            try {
                mWmService.updateDisplayWindowRequestedVisibleTypes(mDisplayId,
                        mRequestedVisibleTypes);
            } catch (RemoteException e) {
                Slog.w(TAG, "Unable to update window manager service.");
            }
        }

        void setImmersiveMode(@ImmersiveState int state) {
            if (mImmersiveState == state) {
                return;
            }
            mImmersiveState = state;

            updateDisplayWindowRequestedVisibleTypes();

            setDelayedImmersiveModeWithoutNavBar(state == STATE_IMMERSIVE_WITH_NAV_BAR);
        }

        /**
         * Hide both navigation bar and status bar after HIDE_NAVIGATION_BAR_DELAY_IN_MILLIS with
         * {@code mDelayedImmersiveModeWithNavBarRunnable} if enabled, otherwise cancel the
         * registered {@code mDelayedImmersiveModeWithNavBarRunnable},
         */
        public void setDelayedImmersiveModeWithoutNavBar(boolean enable) {
            if (enable) {
                Slog.d(TAG, "Hide Nav bar after 10 sec");
                mHandler.postDelayed(mDelayedImmersiveModeWithNavBarRunnable,
                        HIDE_NAVIGATION_BAR_DELAY_IN_MILLIS);
            } else {
                Slog.d(TAG, "Cancel full immersive mode cancelled");
                mHandler.removeCallbacks(mDelayedImmersiveModeWithNavBarRunnable);
            }
        }

        void addCallbackForDisplay(Callback callback) {
            if (mCallbacks.contains(callback)) return;
            mCallbacks.add(callback);
        }

        void removeCallbackForDisplay(Callback callback) {
            mCallbacks.remove(callback);
        }

        void notifyOnImmersiveStateChanged(boolean hideNavBar) {
            for (Callback callback : mCallbacks) {
                callback.onImmersiveStateChanged(hideNavBar);
            }
        }

        void notifyOnImmersiveRequestedChanged(ComponentName component, boolean requested) {
            if (requested && mCurrentDrivingState == DRIVING_STATE_MOVING) {
                // Show toast when app requests immersive mode while driving.
                Toast.makeText(mContext,
                        R.string.car_ui_restricted_while_driving, Toast.LENGTH_LONG).show();
                return;
            }
            for (Callback callback : mCallbacks) {
                callback.onImmersiveRequestedChanged(component, requested);
            }
        }

        void onDrivingStateChanged() {
            if (mImmersiveState == STATE_DEFAULT || mCurrentDrivingState != DRIVING_STATE_MOVING) {
                return;
            }
            mImmersiveState = STATE_DEFAULT;
            updateDisplayWindowRequestedVisibleTypes();
            notifyOnImmersiveRequestedChanged(null, false);
            // Show toast when drive state changes to driving while immersive mode is on.
            Toast.makeText(mContext,
                    R.string.car_ui_restricted_while_driving, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Callback for notifying changes to the immersive and immersive request states.
     */
    public interface Callback {
        /**
         * Callback triggered when the current package's requested visibilities change has caused
         * an immersive request change.
         */
        void onImmersiveRequestedChanged(ComponentName component, boolean requested);

        /**
         * Callback triggered when the immersive state changes.
         */
        void onImmersiveStateChanged(boolean hideNavBar);
    }
}
