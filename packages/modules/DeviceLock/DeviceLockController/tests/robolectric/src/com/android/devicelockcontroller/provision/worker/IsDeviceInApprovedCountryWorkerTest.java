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

package com.android.devicelockcontroller.provision.worker;

import static com.android.devicelockcontroller.provision.worker.IsDeviceInApprovedCountryWorker.KEY_IS_IN_APPROVED_COUNTRY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.ListenableWorker.Result;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import androidx.work.testing.TestWorkerBuilder;

import com.android.devicelockcontroller.provision.grpc.DeviceCheckInClient;
import com.android.devicelockcontroller.provision.grpc.IsDeviceInApprovedCountryGrpcResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RunWith(RobolectricTestRunner.class)
public final class IsDeviceInApprovedCountryWorkerTest {
    @Rule
    public final MockitoRule mMocks = MockitoJUnit.rule();
    @Mock
    private DeviceCheckInClient mClient;
    @Mock
    private IsDeviceInApprovedCountryGrpcResponse mResponse;
    private IsDeviceInApprovedCountryWorker mWorker;

    @Before
    public void setUp() throws Exception {
        final Context context = ApplicationProvider.getApplicationContext();
        final Executor executor = Executors.newSingleThreadExecutor();
        when(mClient.isDeviceInApprovedCountry(any())).thenReturn(mResponse);
        mWorker = TestWorkerBuilder.from(
                        context, IsDeviceInApprovedCountryWorker.class, executor)
                .setWorkerFactory(
                        new WorkerFactory() {
                            @Override
                            public ListenableWorker createWorker(
                                    @NonNull Context context, @NonNull String workerClassName,
                                    @NonNull WorkerParameters workerParameters) {
                                return workerClassName.equals(
                                        IsDeviceInApprovedCountryWorker.class.getName())
                                        ? new IsDeviceInApprovedCountryWorker(
                                        context, workerParameters, mClient)
                                        : null;
                            }
                        }).build();
    }

    @Test
    public void doWork_responseIsSuccessful_isInApprovedCountry_correctResult() {
        when(mResponse.isSuccessful()).thenReturn(true);
        when(mResponse.isDeviceInApprovedCountry()).thenReturn(true);
        Result expected = Result.success(
                new Data.Builder().putBoolean(KEY_IS_IN_APPROVED_COUNTRY, true).build());

        Result actual = mWorker.doWork();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void doWork_responseIsSuccessful_isNotInApprovedCountry_correctResult() {
        when(mResponse.isSuccessful()).thenReturn(true);
        when(mResponse.isDeviceInApprovedCountry()).thenReturn(false);
        Result expected = Result.success(
                new Data.Builder().putBoolean(KEY_IS_IN_APPROVED_COUNTRY, false).build());

        Result actual = mWorker.doWork();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void doWork_responseIsNotSuccessful_failureResult() {
        when(mResponse.isSuccessful()).thenReturn(false);
        when(mResponse.isDeviceInApprovedCountry()).thenReturn(false);
        Result expected = Result.failure();

        Result actual = mWorker.doWork();

        assertThat(actual).isEqualTo(expected);
    }
}
