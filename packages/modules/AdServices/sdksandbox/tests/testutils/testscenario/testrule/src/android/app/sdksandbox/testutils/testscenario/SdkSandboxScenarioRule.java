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

package android.app.sdksandbox.testutils.testscenario;

import static android.app.sdksandbox.SdkSandboxManager.EXTRA_DISPLAY_ID;
import static android.app.sdksandbox.SdkSandboxManager.EXTRA_HEIGHT_IN_PIXELS;
import static android.app.sdksandbox.SdkSandboxManager.EXTRA_HOST_TOKEN;
import static android.app.sdksandbox.SdkSandboxManager.EXTRA_WIDTH_IN_PIXELS;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.app.sdksandbox.RequestSurfacePackageException;
import android.app.sdksandbox.SandboxedSdk;
import android.app.sdksandbox.SdkSandboxManager;
import android.app.sdksandbox.testutils.FakeLoadSdkCallback;
import android.app.sdksandbox.testutils.FakeRequestSurfacePackageCallback;
import android.app.sdksandbox.testutils.WaitableCountDownLatch;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This rule is used to invoke tests inside SDKs. It loads a given Sdk, calls for a test to be
 * executed inside given Sdk and unloads the Sdk once the execution is finished.
 * assertSdkTestRunPasses() contains the logic to trigger an in-SDK test and retrieve its results,
 * while {@link SdkSandboxTestScenarioRunner} handles the Sdk-side logic for test execution.
 */
public class SdkSandboxScenarioRule implements TestRule {
    // This flag is used internally for behaviors that are
    // enabled by default.
    private static final int ENABLE_ALWAYS = 0x1;
    // Execute "Before" and "After" annotations around tests.
    public static final int ENABLE_LIFE_CYCLE_ANNOTATIONS = 0x2;

    // Use flags when you want to make a new behavior available
    // to preserve backwards compatibility.
    @IntDef({ENABLE_ALWAYS, ENABLE_LIFE_CYCLE_ANNOTATIONS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RuleOptions {}

    // We need to allow a fair amount of time to time out since we might
    // want to execute fairly large tests.
    private static final int TEST_TIMEOUT_S = 60;
    private final String mSdkName;
    private ISdkSandboxTestExecutor mTestExecutor;
    private final Bundle mTestInstanceSetupParams;
    private @Nullable IBinder mBinder;
    private final int mFlags;

    public SdkSandboxScenarioRule(String sdkName) {
        this(sdkName, null, null, ENABLE_ALWAYS);
    }

    public SdkSandboxScenarioRule(
            String sdkName, Bundle testInstanceSetupParams, @Nullable IBinder customInterface) {
        this(sdkName, testInstanceSetupParams, customInterface, ENABLE_ALWAYS);
    }

    public SdkSandboxScenarioRule(
            String sdkName,
            Bundle testInstanceSetupParams,
            @Nullable IBinder customInterface,
            @RuleOptions int flags) {
        mSdkName = sdkName;
        mTestInstanceSetupParams = testInstanceSetupParams;
        mBinder = customInterface;
        // The always enable flag is added to the flags
        // so that we have a way to indicate when a behavior
        // is always enabled by default.
        mFlags = flags | ENABLE_ALWAYS;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        // This statement would wrap around every test, similar to @Before and @After
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try (ActivityScenario scenario =
                        ActivityScenario.launch(SdkSandboxCtsActivity.class)) {
                    final Context context =
                            InstrumentationRegistry.getInstrumentation().getContext();
                    SdkSandboxManager sdkSandboxManager =
                            context.getSystemService(SdkSandboxManager.class);

                    try {
                        final SandboxedSdk sdk = getLoadedSdk(sdkSandboxManager);
                        assertThat(scenario.getState()).isEqualTo(Lifecycle.State.RESUMED);
                        setView(scenario, sdkSandboxManager);
                        mTestExecutor =
                                ISdkSandboxTestExecutor.Stub.asInterface(sdk.getInterface());

                        Throwable testFailure =
                                tryDoWhen(
                                        ENABLE_LIFE_CYCLE_ANNOTATIONS,
                                        () -> {
                                            final List<String> beforeMethods =
                                                    mTestExecutor.retrieveAnnotatedMethods(
                                                            Before.class.getCanonicalName());
                                            for (final String before : beforeMethods) {
                                                runSdkMethod(before, new Bundle());
                                            }
                                        });

                        // If the before methods failed, we should not run our tests.
                        if (testFailure == null) {
                            testFailure =
                                    tryDoWhen(
                                            ENABLE_ALWAYS,
                                            () -> {
                                                base.evaluate();
                                            });
                        }

                        // Even if "before methods" or tests fail, we are still expected to
                        // run "after methods" for clean up.
                        Throwable afterFailure =
                                tryDoWhen(
                                        ENABLE_LIFE_CYCLE_ANNOTATIONS,
                                        () -> {
                                            final List<String> afterMethods =
                                                    mTestExecutor.retrieveAnnotatedMethods(
                                                            After.class.getCanonicalName());
                                            for (final String after : afterMethods) {
                                                runSdkMethod(after, new Bundle());
                                            }
                                        });

                        if (testFailure != null) {
                            throw testFailure;
                        } else if (afterFailure != null) {
                            throw afterFailure;
                        }
                    } finally {
                        sdkSandboxManager.unloadSdk(mSdkName);
                    }
                }
            }
        };
    }

    public void assertSdkTestRunPasses(String testMethodName) throws Exception {
        assertSdkTestRunPasses(testMethodName, new Bundle());
    }

    public void assertSdkTestRunPasses(String testMethodName, Bundle params) throws Exception {
        runSdkMethod(testMethodName, params);
    }

    private void runSdkMethod(String methodName, Bundle params) throws Exception {
        WaitableCountDownLatch testDoneLatch = new WaitableCountDownLatch(TEST_TIMEOUT_S);
        AtomicReference<String> errorRef = new AtomicReference<>(null);

        ISdkSandboxResultCallback.Stub callback =
                new ISdkSandboxResultCallback.Stub() {
                    public void onResult() {
                        testDoneLatch.countDown();
                    }

                    public void onError(String errorMessage) {
                        if (TextUtils.isEmpty(errorMessage)) {
                            errorRef.set(
                                    String.format(
                                            "Error executing method %s in sdk: Sdk returned no"
                                                    + " stacktrace",
                                            methodName));
                        } else {
                            errorRef.set(errorMessage);
                        }
                        testDoneLatch.countDown();
                    }
                };

        assertThat(mTestExecutor).isNotNull();
        mTestExecutor.invokeMethod(methodName, params, callback);

        testDoneLatch.waitForLatch("Sdk did not return any response");

        if (errorRef.get() != null) assertWithMessage(errorRef.get()).fail();
        assertThat(true).isTrue();
    }

    /**
     * Will attempt to perform a {@link MightThrow} and return a throwable if it failed. It also
     * expects a flag to gate if the behavior should be attempted or if it should just return.
     */
    private Throwable tryDoWhen(@RuleOptions int doWhenFlag, MightThrow mightThrow) {
        try {
            if (isFlagSet(doWhenFlag)) {
                mightThrow.call();
            }
            return null;
        } catch (Throwable e) {
            return e;
        }
    }

    private SandboxedSdk getLoadedSdk(SdkSandboxManager sdkSandboxManager) throws Exception {
        final Bundle loadParams = new Bundle(2);
        loadParams.putBundle(ISdkSandboxTestExecutor.TEST_SETUP_PARAMS, mTestInstanceSetupParams);
        loadParams.putBinder(ISdkSandboxTestExecutor.TEST_AUTHOR_DEFINED_BINDER, mBinder);
        final FakeLoadSdkCallback callback = new FakeLoadSdkCallback();
        sdkSandboxManager.loadSdk(mSdkName, loadParams, Runnable::run, callback);
        try {
            callback.assertLoadSdkIsSuccessful();
        } catch (Exception e) {
            Assume.assumeTrue(
                    "Skipping test because Sdk Sandbox is disabled",
                    callback.getLoadSdkErrorCode()
                            != SdkSandboxManager.LOAD_SDK_SDK_SANDBOX_DISABLED);
            throw e;
        }
        return callback.getSandboxedSdk();
    }

    private void setView(ActivityScenario scenario, SdkSandboxManager sdkSandboxManager)
            throws Exception {
        AtomicReference<RequestSurfacePackageException> surfacePackageException =
                new AtomicReference<>(null);
        scenario.onActivity(
                activity -> {
                    final SurfaceView renderedView = activity.findViewById(R.id.rendered_view);
                    final FakeRequestSurfacePackageCallback surfacePackageCallback =
                            new FakeRequestSurfacePackageCallback();

                    Bundle params = new Bundle();
                    params.putInt(EXTRA_WIDTH_IN_PIXELS, renderedView.getWidth());
                    params.putInt(EXTRA_HEIGHT_IN_PIXELS, renderedView.getHeight());
                    params.putInt(EXTRA_DISPLAY_ID, activity.getDisplay().getDisplayId());
                    params.putBinder(EXTRA_HOST_TOKEN, renderedView.getHostToken());

                    sdkSandboxManager.requestSurfacePackage(
                            mSdkName, params, Runnable::run, surfacePackageCallback);

                    if (!surfacePackageCallback.isRequestSurfacePackageSuccessful()) {
                        surfacePackageException.set(
                                surfacePackageCallback.getSurfacePackageException());
                    } else {
                        renderedView.setChildSurfacePackage(
                                surfacePackageCallback.getSurfacePackage());
                        renderedView.setVisibility(View.VISIBLE);
                        renderedView.setZOrderOnTop(true);

                        // Keyboard events will only go to the surface view if it has focus.
                        // This needs to be done with a touch event.
                        renderedView.setFocusableInTouchMode(true);
                        renderedView.requestFocusFromTouch();
                    }
                });
        if (surfacePackageException.get() != null) {
            throw surfacePackageException.get();
        }
    }

    private boolean isFlagSet(@RuleOptions int flag) {
        return (mFlags & flag) == flag;
    }

    /**
     * Similar to Callable but uses Throwable instead. Also not generic because we don't need the
     * return types in this class.
     */
    private interface MightThrow {
        void call() throws Throwable;
    }
}
