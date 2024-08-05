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
package android.sdksandbox.test.scenario.sampleclient;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static com.google.common.truth.Truth.assertThat;

import android.os.SystemClock;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import java.io.IOException;

/** Helper class for Sdk Sandbox e2e perf tests. */
public class SdkSandboxTestHelper {
    private static final UiDevice sUiDevice = UiDevice.getInstance(getInstrumentation());

    private static final long UI_NAVIGATION_WAIT_MS = 5000;
    private static final long UI_WAIT_LOADSDK_MS = 500;
    private static final long UI_RETRIES_WAIT_LOADSDK = 10;
    private static final String SANDBOX_TEST_CLIENT_APP = "com.android.sdksandboxclient";
    private static final String LOAD_BUTTON = "load_code_button";
    private static final String RENDER_BUTTON = "request_surface_button";

    /** Open sandbox client test app using shell command line. */
    public void openClientApp() throws Exception {
        sUiDevice.executeShellCommand(
                "am start " + SANDBOX_TEST_CLIENT_APP + "/" + ".MainActivity");
    }

    /** Load sdk on sandbox client test app by clicking the loadSdk button. */
    public void loadSandboxSdk() {
        if (getLoadSdkButton() != null) {
            getLoadSdkButton().click();
        } else {
            throw new RuntimeException("Did not find 'Load SDKs' button.");
        }

        int retries = 0;
        boolean sdkLoaded = false;
        // wait until loadSdk.
        while (!sdkLoaded && retries < UI_RETRIES_WAIT_LOADSDK) {
            SystemClock.sleep(UI_WAIT_LOADSDK_MS);
            if (getLoadSdkButton().getText().equals("Unload SDKs")) {
                sdkLoaded = true;
            }
            retries++;
        }

        assertThat(getLoadSdkButton().getText()).isEqualTo("Unload SDKs");
    }

    /** Remote render ad on sandbox client test app by clicking request surface button. */
    public void remoteRenderAd() {
        if (getRequestSurfaceButton() != null) {
            getRequestSurfaceButton().click();
        } else {
            throw new RuntimeException("Did not find 'Load Surface Package' button.");
        }
    }

    public static void closeClientApp() throws IOException {
        sUiDevice.executeShellCommand("am force-stop " + SANDBOX_TEST_CLIENT_APP);
    }

    private UiObject2 getLoadSdkButton() {
        return sUiDevice.wait(
                Until.findObject(By.res(SANDBOX_TEST_CLIENT_APP, LOAD_BUTTON)),
                UI_NAVIGATION_WAIT_MS);
    }

    private UiObject2 getRequestSurfaceButton() {
        return sUiDevice.wait(
                Until.findObject(By.res(SANDBOX_TEST_CLIENT_APP, RENDER_BUTTON)),
                UI_NAVIGATION_WAIT_MS);
    }
}
