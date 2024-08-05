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

package com.android.devicelockcontroller.storage;

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;
import android.os.RemoteException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class GlobalParametersServiceTest extends AbstractGlobalParametersTestBase {
    private IGlobalParametersService mIGlobalParametersService;

    @Before
    public void setUp() {
        final ServiceController<GlobalParametersService> globalParametersServiceController =
                Robolectric.buildService(GlobalParametersService.class);
        final GlobalParametersService globalParametersService =
                globalParametersServiceController.create().get();
        mIGlobalParametersService =
                (IGlobalParametersService) globalParametersService.onBind(new Intent());
    }

    @Test
    public void getLockTaskAllowlist_shouldReturnExpectedAllowlist() throws RemoteException {
        assertThat(mIGlobalParametersService.getLockTaskAllowlist()).isEmpty();
        final ArrayList<String> expectedAllowlist = new ArrayList<>();
        expectedAllowlist.add(ALLOWLIST_PACKAGE_0);
        expectedAllowlist.add(ALLOWLIST_PACKAGE_1);
        mIGlobalParametersService.setLockTaskAllowlist(expectedAllowlist);
        final List<String> actualAllowlist = mIGlobalParametersService.getLockTaskAllowlist();
        assertThat(actualAllowlist).containsExactlyElementsIn(expectedAllowlist);
    }

    @Test
    public void needCheckIn_shouldReturnExpectedResult() throws RemoteException {
        assertThat(mIGlobalParametersService.needCheckIn()).isNotEqualTo(NEED_CHECK_IN);

        mIGlobalParametersService.setNeedCheckIn(NEED_CHECK_IN);

        assertThat(mIGlobalParametersService.needCheckIn()).isEqualTo(NEED_CHECK_IN);
    }

    @Test
    public void getRegisteredId_shouldReturnExpectedResult() throws RemoteException {
        assertThat(mIGlobalParametersService.getRegisteredDeviceId()).isNull();

        mIGlobalParametersService.setRegisteredDeviceId(REGISTERED_DEVICE_ID);

        assertThat(mIGlobalParametersService.getRegisteredDeviceId()).isEqualTo(
                REGISTERED_DEVICE_ID);
    }

    @Test
    public void isProvisionForced_shouldReturnExpectedResult() throws RemoteException {
        assertThat(mIGlobalParametersService.isProvisionForced()).isNotEqualTo(FORCED_PROVISION);

        mIGlobalParametersService.setProvisionForced(FORCED_PROVISION);

        assertThat(mIGlobalParametersService.isProvisionForced()).isEqualTo(FORCED_PROVISION);
    }

    @Test
    public void getEnrollmentToken_shouldReturnExpectedResult() throws RemoteException {
        assertThat(mIGlobalParametersService.getEnrollmentToken()).isNull();

        mIGlobalParametersService.setEnrollmentToken(ENROLLMENT_TOKEN);

        assertThat(mIGlobalParametersService.getEnrollmentToken()).isEqualTo(ENROLLMENT_TOKEN);
    }

    @Test
    public void getLastReceivedProvisionState_shouldReturnExpectedResult() throws RemoteException {
        assertThat(mIGlobalParametersService.getLastReceivedProvisionState()).isNotEqualTo(
                LAST_RECEIVED_PROVISION_STATE);

        mIGlobalParametersService.setLastReceivedProvisionState(LAST_RECEIVED_PROVISION_STATE);

        assertThat(mIGlobalParametersService.getLastReceivedProvisionState()).isEqualTo(
                LAST_RECEIVED_PROVISION_STATE);
    }
}
