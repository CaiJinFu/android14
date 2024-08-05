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

package com.android.federatedcompute.services;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;

import android.content.Intent;
import android.os.IBinder;

import androidx.test.core.app.ApplicationProvider;

import com.android.dx.mockito.inline.extended.ExtendedMockito;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;

public final class FederatedComputeManagingServiceImplTest {
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testBindableFederatedComputeService() {
        MockitoSession session = ExtendedMockito.mockitoSession().startMocking();
        try {
            FederatedComputeManagingServiceImpl spyFcpService =
                    spy(new FederatedComputeManagingServiceImpl());
            spyFcpService.onCreate();
            Intent intent =
                    new Intent(
                            ApplicationProvider.getApplicationContext(),
                            FederatedComputeManagingServiceImpl.class);
            IBinder binder = spyFcpService.onBind(intent);
            assertNotNull(binder);
        } finally {
            session.finishMocking();
        }
    }
}
