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

package android.car.app;

import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

import android.annotation.MainThread;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.app.Activity;
import android.app.Application;
import android.car.builtin.input.InputManagerHelper;
import android.car.builtin.view.ViewHelper;
import android.car.builtin.window.WindowManagerHelper;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.List;


/**
 * This class is responsible to intercept the swipe gestures & long press over {@link
 * ControlledRemoteCarTaskView}.
 *
 * <ul>
 *   <li>The gesture interception will only occur when the corresponding {@link
 *       ControlledRemoteCarTaskViewConfig#mShouldCaptureGestures} is set.
 *   <li>The long press interception will only occur when the corresponding {@link
 *       ControlledRemoteCarTaskViewConfig#mShouldCaptureLongPress} is set.
 * </ul>
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
final class CarTaskViewInputInterceptor {

    private static final String TAG = "CarTaskViewInput";
    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    private static final Rect sTmpBounds = new Rect();

    private final Activity mHostActivity;
    private final InputManager mInputManager;
    private final WindowManager mWm;
    private final GestureDetector mGestureDetector =
            new GestureDetector(new TaskViewGestureListener());
    private final Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks =
            new ActivityLifecycleHandler();
    private final CarTaskViewController mTaskViewController;

    private View mSpyWindow;
    private boolean mInitialized = false;

    CarTaskViewInputInterceptor(Activity hostActivity, CarTaskViewController taskViewController) {
        mHostActivity = hostActivity;
        mInputManager = hostActivity.getSystemService(InputManager.class);
        mTaskViewController = taskViewController;
        mWm = mHostActivity.getSystemService(WindowManager.class);
    }

    private static boolean isIn(MotionEvent event, RemoteCarTaskView taskView) {
        ViewHelper.getBoundsOnScreen(taskView, sTmpBounds);
        return sTmpBounds.contains((int) event.getX(), (int) event.getY());
    }

    /** Initializes & starts intercepting gestures. Does nothing if already initialized. */
    @MainThread
    void init() {
        if (mInitialized) {
            Log.w(TAG, "Already initialized");
            return;
        }
        mInitialized = true;
        mHostActivity.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
        startInterceptingGestures();
    }

    /**
     * Releases the held resources and stops intercepting gestures. Does nothing if already
     * released.
     */
    @MainThread
    void release() {
        if (!mInitialized) {
            Log.w(TAG, "Failed to release as it is not initialized");
            return;
        }
        mInitialized = false;
        mHostActivity.unregisterActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
        stopInterceptingGestures();
    }

    private void startInterceptingGestures() {
        if (DBG) {
            Log.d(TAG, "Start intercepting gestures");
        }
        if (mSpyWindow != null) {
            Log.d(TAG, "Already intercepting gestures");
            return;
        }
        createAndAddSpyWindow();
    }

    private void stopInterceptingGestures() {
        if (DBG) {
            Log.d(TAG, "Stop intercepting gestures");
        }
        if (mSpyWindow == null) {
            Log.d(TAG, "Already not intercepting gestures");
            return;
        }
        removeSpyWindow();
    }

    private void createAndAddSpyWindow() {
        mSpyWindow = new GestureSpyView(mHostActivity);
        WindowManager.LayoutParams p =
                new WindowManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                | FLAG_NOT_FOCUSABLE
                                | FLAG_LAYOUT_IN_SCREEN,
                        // LAYOUT_IN_SCREEN required so that event coordinate system matches the
                        // taskview.getBoundsOnScreen coordinate system
                        PixelFormat.TRANSLUCENT);

        WindowManagerHelper.setInputFeatureSpy(p);
        WindowManagerHelper.setTrustedOverlay(p);
        mWm.addView(mSpyWindow, p);
    }

    private void removeSpyWindow() {
        if (mSpyWindow == null) {
            Log.e(TAG, "Spy window is not present");
            return;
        }
        mWm.removeView(mSpyWindow);
        mSpyWindow = null;
    }

    private final class GestureSpyView extends View {
        private boolean mConsumingCurrentEventStream = false;
        private boolean mActionDownInsideTaskView = false;
        private float mTouchDownX;
        private float mTouchDownY;

        GestureSpyView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            boolean justToggled = false;
            mGestureDetector.onTouchEvent(event);

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mActionDownInsideTaskView = false;

                List<ControlledRemoteCarTaskView> taskViewList =
                        mTaskViewController.getControlledRemoteCarTaskViews();
                for (ControlledRemoteCarTaskView tv : taskViewList) {
                    if (tv.getConfig().mShouldCaptureGestures && isIn(event, tv)) {
                        mTouchDownX = event.getX();
                        mTouchDownY = event.getY();
                        mActionDownInsideTaskView = true;
                        break;
                    }
                }

                // Stop consuming immediately on ACTION_DOWN
                mConsumingCurrentEventStream = false;
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (!mConsumingCurrentEventStream && mActionDownInsideTaskView
                        && Float.compare(mTouchDownX, event.getX()) != 0
                        && Float.compare(mTouchDownY, event.getY()) != 0) {
                    // Start consuming on ACTION_MOVE when ACTION_DOWN happened inside TaskView
                    mConsumingCurrentEventStream = true;
                    justToggled = true;
                }

                // Handling the events
                if (mConsumingCurrentEventStream) {
                    // Disable the propagation when consuming events.
                    InputManagerHelper.pilferPointers(mInputManager, this);

                    if (justToggled) {
                        // When just toggled from DOWN to MOVE, dispatch a DOWN event as DOWN event
                        // is meant to be the first event in an event stream.
                        MotionEvent cloneEvent = MotionEvent.obtain(event);
                        cloneEvent.setAction(MotionEvent.ACTION_DOWN);
                        CarTaskViewInputInterceptor.this.mHostActivity.dispatchTouchEvent(
                                cloneEvent);
                        cloneEvent.recycle();
                    }
                    CarTaskViewInputInterceptor.this.mHostActivity.dispatchTouchEvent(event);
                }
            }

            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Handling the events
                if (mConsumingCurrentEventStream) {
                    // Disable the propagation when handling manually.
                    InputManagerHelper.pilferPointers(mInputManager, this);
                    CarTaskViewInputInterceptor.this.mHostActivity.dispatchTouchEvent(event);
                }
                mConsumingCurrentEventStream = false;
            }
            return false;
        }
    }

    private final class TaskViewGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            List<ControlledRemoteCarTaskView> taskViewList =
                    mTaskViewController.getControlledRemoteCarTaskViews();
            for (ControlledRemoteCarTaskView tv : taskViewList) {
                if (tv.getConfig().mShouldCaptureLongPress && isIn(e, tv)) {
                    if (DBG) {
                        Log.d(TAG, "Long press captured for taskView: " + tv);
                    }
                    InputManagerHelper.pilferPointers(mInputManager, mSpyWindow);
                    tv.performLongClick();
                    return;
                }
            }
            if (DBG) {
                Log.d(TAG, "Long press not captured");
            }
        }
    }

    private final class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(
                @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            if (!mInitialized) {
                return;
            }
            startInterceptingGestures();
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            if (!mInitialized) {
                return;
            }
            stopInterceptingGestures();
        }

        @Override
        public void onActivitySaveInstanceState(
                @NonNull Activity activity, @NonNull Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
        }
    }
}
