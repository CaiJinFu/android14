/*
 * Copyright 2016, The Android Open Source Project
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

package com.android.managedprovisioning.provisioning;

import static android.app.admin.DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import android.content.ComponentName;
import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;

import com.android.managedprovisioning.R;
import com.android.managedprovisioning.common.Utils;
import com.android.managedprovisioning.model.PackageDownloadInfo;
import com.android.managedprovisioning.model.ProvisioningParams;
import com.android.managedprovisioning.model.WifiInfo;
import com.android.managedprovisioning.task.AbstractProvisioningTask;
import com.android.managedprovisioning.task.AddWifiNetworkTask;
import com.android.managedprovisioning.task.ConnectMobileNetworkTask;
import com.android.managedprovisioning.task.DownloadPackageTask;
import com.android.managedprovisioning.task.ProvisionFullyManagedDeviceTask;
import com.android.managedprovisioning.task.VerifyAdminPackageTask;

import org.mockito.Mock;

/**
 * Unit tests for {@link DeviceOwnerProvisioningController}.
 */
public class DeviceOwnerProvisioningControllerTest extends ProvisioningControllerBaseTest {

    private static final int TEST_USER_ID = 123;
    private static final ComponentName TEST_ADMIN = new ComponentName("com.test.admin",
            "com.test.admin.AdminReceiver");

    private static final String TEST_SSID = "\"SomeSsid\"";
    private static final WifiInfo TEST_WIFI_INFO = new WifiInfo.Builder()
            .setSsid(TEST_SSID)
            .build();

    private static final String TEST_DOWNLOAD_LOCATION = "http://www.some.uri.com";
    private static final byte[] TEST_PACKAGE_CHECKSUM = new byte[] { '1', '2', '3', '4', '5' };
    private static final PackageDownloadInfo TEST_DOWNLOAD_INFO = new PackageDownloadInfo.Builder()
            .setLocation(TEST_DOWNLOAD_LOCATION)
            .setSignatureChecksum(TEST_PACKAGE_CHECKSUM)
            .build();
    private static final String TEST_ERROR_MESSAGE = "test error message";
    private static final Context mContext = InstrumentationRegistry.getTargetContext();

    @Mock
    private ProvisioningControllerCallback mCallback;

    private static final AbstractProvisioningTask TASK =
            new AddWifiNetworkTask(mContext,
                    createProvisioningParamsBuilder().build(),
                    createTaskCallback());

    @Mock
    private Utils mUtils;

    @SmallTest
    public void testRunAllTasks() throws Exception {
        // GIVEN device owner provisioning was invoked with a wifi and download info
        createController(createProvisioningParamsBuilder().build());

        // WHEN starting the test run
        mController.start(mHandler);

        // THEN the add wifi task should be run
        taskSucceeded(AddWifiNetworkTask.class);

        // THEN the download package task should be run
        taskSucceeded(DownloadPackageTask.class);

        // THEN the verify package task should be run
        taskSucceeded(VerifyAdminPackageTask.class);

        // THEN the install package tasks should be run
        tasksDownloadAndInstallDeviceOwnerPackageSucceeded(TEST_USER_ID);

        // THEN the provisioning task should be run
        taskSucceeded(ProvisionFullyManagedDeviceTask.class);

        // THEN the provisioning complete callback should have happened
        verify(mCallback).provisioningTasksCompleted();
    }

    @SmallTest
    public void testNoWifiInfo() throws Exception {
        // GIVEN device owner provisioning was invoked with a wifi and download info
        createController(createProvisioningParamsBuilder().setWifiInfo(null).build());

        // WHEN starting the test run
        mController.start(mHandler);

        // THEN the download package task should be run
        taskSucceeded(DownloadPackageTask.class);

        // THEN the verify package task should be run
        taskSucceeded(VerifyAdminPackageTask.class);

        // THEN the install package tasks should be run
        tasksDownloadAndInstallDeviceOwnerPackageSucceeded(TEST_USER_ID);

        // THEN the provisioning task should be run
        taskSucceeded(ProvisionFullyManagedDeviceTask.class);

        // THEN the provisioning complete callback should have happened
        verify(mCallback).provisioningTasksCompleted();
    }

    @SmallTest
    public void testNoDownloadInfo() throws Exception {
        // GIVEN device owner provisioning was invoked with a wifi and download info
        createController(
                createProvisioningParamsBuilder().setDeviceAdminDownloadInfo(null).build());

        // WHEN starting the test run
        mController.start(mHandler);

        // THEN the add wifi task should be run
        taskSucceeded(AddWifiNetworkTask.class);

        // THEN the provisioning task should be run
        taskSucceeded(ProvisionFullyManagedDeviceTask.class);

        // THEN the provisioning complete callback should have happened
        verify(mCallback).provisioningTasksCompleted();
    }

    @SmallTest
    public void testErrorAddWifiTask() throws Exception {
        // GIVEN device owner provisioning was invoked with a wifi and download info
        createController(createProvisioningParamsBuilder().build());

        // WHEN starting the test run
        mController.start(mHandler);

        // THEN the add wifi task should be run
        AbstractProvisioningTask task = verifyTaskRun(AddWifiNetworkTask.class);

        // WHEN the task causes an error
        mController.onError(task, 0, /* errorMessage= */ null);

        // THEN the onError callback should have been called without factory reset being required
        verify(mCallback).error(eq(R.string.cant_set_up_device), anyString(), eq(false));
    }

    @SmallTest
    public void testErrorDownloadAppTask() throws Exception {
        // GIVEN device owner provisioning was invoked with a wifi and download info
        createController(createProvisioningParamsBuilder().build());

        // WHEN starting the test run
        mController.start(mHandler);

        // THEN the add wifi task should be run
        taskSucceeded(AddWifiNetworkTask.class);

        // THEN the download package task should be run
        AbstractProvisioningTask task = verifyTaskRun(DownloadPackageTask.class);

        // WHEN the task causes an error
        mController.onError(task, 0, /* errorMessage= */ null);

        // THEN the onError callback should have been called with factory reset being required
        verify(mCallback).error(anyInt(), anyString(), eq(true));
    }

    @SmallTest
    public void testErrorWithStringMessage() {
        createController(createProvisioningParamsBuilder().build());
        mController.start(mHandler);

        mController.onError(TASK, /* errorCode= */ 0, TEST_ERROR_MESSAGE);

        verify(mCallback).error(anyInt(), eq(TEST_ERROR_MESSAGE), eq(false));
    }

    @SmallTest
    public void testStart_useMobileDataTrueAndNoWifiInfo_runsConnectMobileNetworkTask()
            throws Exception {
        createController(
                createProvisioningParamsBuilder().setWifiInfo(null).setUseMobileData(true).build());
        mController.start(mHandler);
        taskSucceeded(ConnectMobileNetworkTask.class);
    }

    @SmallTest
    public void testStart_useMobileDataTrueAndWifiInfo_runsAddWifiNetworkTask()
            throws Exception {
        createController(
                createProvisioningParamsBuilder()
                        .setWifiInfo(TEST_WIFI_INFO)
                        .setUseMobileData(true)
                        .build());
        mController.start(mHandler);
        taskSucceeded(AddWifiNetworkTask.class);
    }

    private void createController(ProvisioningParams params) {
        mController = DeviceOwnerProvisioningController.createInstance(
                getContext(),
                params,
                TEST_USER_ID,
                mCallback,
                mUtils);
    }

    private static ProvisioningParams.Builder createProvisioningParamsBuilder() {
        return new ProvisioningParams.Builder()
                .setDeviceAdminComponentName(TEST_ADMIN)
                .setProvisioningAction(ACTION_PROVISION_MANAGED_DEVICE)
                .setWifiInfo(TEST_WIFI_INFO)
                .setDeviceAdminDownloadInfo(TEST_DOWNLOAD_INFO);
    }

    private static AbstractProvisioningTask.Callback createTaskCallback() {
        return new AbstractProvisioningTask.Callback() {
            @Override
            public void onSuccess(AbstractProvisioningTask task) {

            }

            @Override
            public void onError(AbstractProvisioningTask task, int errorCode,
                    String errorMessage) {

            }
        };
    }
}
