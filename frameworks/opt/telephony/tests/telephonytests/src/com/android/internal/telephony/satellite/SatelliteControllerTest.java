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

package com.android.internal.telephony.satellite;

import static android.telephony.satellite.SatelliteManager.KEY_DEMO_MODE_ENABLED;
import static android.telephony.satellite.SatelliteManager.KEY_SATELLITE_CAPABILITIES;
import static android.telephony.satellite.SatelliteManager.KEY_SATELLITE_COMMUNICATION_ALLOWED;
import static android.telephony.satellite.SatelliteManager.KEY_SATELLITE_ENABLED;
import static android.telephony.satellite.SatelliteManager.KEY_SATELLITE_NEXT_VISIBILITY;
import static android.telephony.satellite.SatelliteManager.KEY_SATELLITE_PROVISIONED;
import static android.telephony.satellite.SatelliteManager.KEY_SATELLITE_SUPPORTED;
import static android.telephony.satellite.SatelliteManager.NT_RADIO_TECHNOLOGY_EMTC_NTN;
import static android.telephony.satellite.SatelliteManager.NT_RADIO_TECHNOLOGY_NR_NTN;
import static android.telephony.satellite.SatelliteManager.NT_RADIO_TECHNOLOGY_PROPRIETARY;
import static android.telephony.satellite.SatelliteManager.SATELLITE_ERROR;
import static android.telephony.satellite.SatelliteManager.SATELLITE_ERROR_NONE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_INVALID_ARGUMENTS;
import static android.telephony.satellite.SatelliteManager.SATELLITE_INVALID_MODEM_STATE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_INVALID_TELEPHONY_STATE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_OFF;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_UNAVAILABLE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_NOT_AUTHORIZED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_NOT_SUPPORTED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_NO_RESOURCES;
import static android.telephony.satellite.SatelliteManager.SATELLITE_RADIO_NOT_AVAILABLE;
import static android.telephony.satellite.SatelliteManager.SATELLITE_REQUEST_IN_PROGRESS;
import static android.telephony.satellite.SatelliteManager.SATELLITE_SERVICE_NOT_PROVISIONED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_SERVICE_PROVISION_IN_PROGRESS;

import static com.android.internal.telephony.satellite.SatelliteController.SATELLITE_MODE_ENABLED_FALSE;
import static com.android.internal.telephony.satellite.SatelliteController.SATELLITE_MODE_ENABLED_TRUE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.NonNull;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ICancellationSignal;
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;
import android.telephony.Rlog;
import android.telephony.satellite.ISatelliteDatagramCallback;
import android.telephony.satellite.ISatelliteProvisionStateCallback;
import android.telephony.satellite.ISatelliteStateCallback;
import android.telephony.satellite.ISatelliteTransmissionUpdateCallback;
import android.telephony.satellite.SatelliteCapabilities;
import android.telephony.satellite.SatelliteDatagram;
import android.telephony.satellite.SatelliteManager;
import android.telephony.satellite.SatelliteManager.SatelliteException;
import android.testing.AndroidTestingRunner;
import android.testing.TestableLooper;

import com.android.internal.telephony.IIntegerConsumer;
import com.android.internal.telephony.IVoidConsumer;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyTest;
import com.android.internal.telephony.satellite.metrics.ControllerMetricsStats;
import com.android.internal.telephony.satellite.metrics.ProvisionMetricsStats;
import com.android.internal.telephony.satellite.metrics.SessionMetricsStats;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidTestingRunner.class)
@TestableLooper.RunWithLooper
public class SatelliteControllerTest extends TelephonyTest {
    private static final String TAG = "SatelliteControllerTest";
    private static final long TIMEOUT = 500;
    private static final int SUB_ID = 0;
    private static final int MAX_BYTES_PER_OUT_GOING_DATAGRAM = 339;
    private static final String TEST_SATELLITE_TOKEN = "TEST_SATELLITE_TOKEN";
    private static final String TEST_NEXT_SATELLITE_TOKEN = "TEST_NEXT_SATELLITE_TOKEN";

    private TestSatelliteController mSatelliteControllerUT;
    private TestSharedPreferences mSharedPreferences;

    @Mock private DatagramController mMockDatagramController;
    @Mock private SatelliteModemInterface mMockSatelliteModemInterface;
    @Mock private SatelliteSessionController mMockSatelliteSessionController;
    @Mock private PointingAppController mMockPointingAppController;
    @Mock private ControllerMetricsStats mMockControllerMetricsStats;
    @Mock private ProvisionMetricsStats mMockProvisionMetricsStats;
    @Mock private SessionMetricsStats mMockSessionMetricsStats;
    private List<Integer> mIIntegerConsumerResults =  new ArrayList<>();
    @Mock private ISatelliteTransmissionUpdateCallback mStartTransmissionUpdateCallback;
    @Mock private ISatelliteTransmissionUpdateCallback mStopTransmissionUpdateCallback;
    private Semaphore mIIntegerConsumerSemaphore = new Semaphore(0);
    private IIntegerConsumer mIIntegerConsumer = new IIntegerConsumer.Stub() {
        @Override
        public void accept(int result) {
            logd("mIIntegerConsumer: result=" + result);
            mIIntegerConsumerResults.add(result);
            try {
                mIIntegerConsumerSemaphore.release();
            } catch (Exception ex) {
                loge("mIIntegerConsumer: Got exception in releasing semaphore, ex=" + ex);
            }
        }
    };

    private boolean mIsSatelliteServiceSupported = true;
    private boolean mIsPointingRequired = true;
    private Set<Integer> mSupportedRadioTechnologies = new HashSet<>(Arrays.asList(
            NT_RADIO_TECHNOLOGY_NR_NTN,
            NT_RADIO_TECHNOLOGY_EMTC_NTN,
            NT_RADIO_TECHNOLOGY_PROPRIETARY));
    private SatelliteCapabilities mSatelliteCapabilities = new SatelliteCapabilities(
            mSupportedRadioTechnologies, mIsPointingRequired, MAX_BYTES_PER_OUT_GOING_DATAGRAM,
            new HashMap<>());
    private Semaphore mSatelliteCapabilitiesSemaphore = new Semaphore(0);
    private SatelliteCapabilities mQueriedSatelliteCapabilities = null;
    private int mQueriedSatelliteCapabilitiesResultCode = SATELLITE_ERROR_NONE;
    private ResultReceiver mSatelliteCapabilitiesReceiver = new ResultReceiver(null) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mQueriedSatelliteCapabilitiesResultCode = resultCode;
            if (resultCode == SATELLITE_ERROR_NONE) {
                if (resultData.containsKey(KEY_SATELLITE_CAPABILITIES)) {
                    mQueriedSatelliteCapabilities = resultData.getParcelable(
                            KEY_SATELLITE_CAPABILITIES, SatelliteCapabilities.class);
                } else {
                    loge("KEY_SATELLITE_SUPPORTED does not exist.");
                    mQueriedSatelliteCapabilities = null;
                }
            } else {
                logd("mSatelliteSupportReceiver: resultCode=" + resultCode);
                mQueriedSatelliteCapabilities = null;
            }
            try {
                mSatelliteCapabilitiesSemaphore.release();
            } catch (Exception ex) {
                loge("mSatelliteSupportReceiver: Got exception in releasing semaphore, ex=" + ex);
            }
        }
    };

    private boolean mQueriedSatelliteSupported = false;
    private int mQueriedSatelliteSupportedResultCode = SATELLITE_ERROR_NONE;
    private Semaphore mSatelliteSupportSemaphore = new Semaphore(0);
    private ResultReceiver mSatelliteSupportReceiver = new ResultReceiver(null) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mQueriedSatelliteSupportedResultCode = resultCode;
            if (resultCode == SATELLITE_ERROR_NONE) {
                if (resultData.containsKey(KEY_SATELLITE_SUPPORTED)) {
                    mQueriedSatelliteSupported = resultData.getBoolean(KEY_SATELLITE_SUPPORTED);
                } else {
                    loge("KEY_SATELLITE_SUPPORTED does not exist.");
                    mQueriedSatelliteSupported = false;
                }
            } else {
                logd("mSatelliteSupportReceiver: resultCode=" + resultCode);
                mQueriedSatelliteSupported = false;
            }
            try {
                mSatelliteSupportSemaphore.release();
            } catch (Exception ex) {
                loge("mSatelliteSupportReceiver: Got exception in releasing semaphore, ex=" + ex);
            }
        }
    };

    private boolean mQueriedIsSatelliteEnabled = false;
    private int mQueriedIsSatelliteEnabledResultCode = SATELLITE_ERROR_NONE;
    private Semaphore mIsSatelliteEnabledSemaphore = new Semaphore(0);
    private ResultReceiver mIsSatelliteEnabledReceiver = new ResultReceiver(null) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mQueriedIsSatelliteEnabledResultCode = resultCode;
            if (resultCode == SATELLITE_ERROR_NONE) {
                if (resultData.containsKey(KEY_SATELLITE_ENABLED)) {
                    mQueriedIsSatelliteEnabled = resultData.getBoolean(KEY_SATELLITE_ENABLED);
                } else {
                    loge("KEY_SATELLITE_ENABLED does not exist.");
                    mQueriedIsSatelliteEnabled = false;
                }
            } else {
                logd("mIsSatelliteEnableReceiver: resultCode=" + resultCode);
                mQueriedIsSatelliteEnabled = false;
            }
            try {
                mIsSatelliteEnabledSemaphore.release();
            } catch (Exception ex) {
                loge("mIsSatelliteEnableReceiver: Got exception in releasing semaphore, ex=" + ex);
            }
        }
    };

    private boolean mQueriedIsDemoModeEnabled = false;
    private int mQueriedIsDemoModeEnabledResultCode = SATELLITE_ERROR_NONE;
    private Semaphore mIsDemoModeEnabledSemaphore = new Semaphore(0);
    private ResultReceiver mIsDemoModeEnabledReceiver = new ResultReceiver(null) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mQueriedIsDemoModeEnabledResultCode = resultCode;
            if (resultCode == SATELLITE_ERROR_NONE) {
                if (resultData.containsKey(KEY_DEMO_MODE_ENABLED)) {
                    mQueriedIsDemoModeEnabled = resultData.getBoolean(KEY_DEMO_MODE_ENABLED);
                } else {
                    loge("KEY_DEMO_MODE_ENABLED does not exist.");
                    mQueriedIsDemoModeEnabled = false;
                }
            } else {
                logd("mIsSatelliteEnableReceiver: resultCode=" + resultCode);
                mQueriedIsDemoModeEnabled = false;
            }
            try {
                mIsDemoModeEnabledSemaphore.release();
            } catch (Exception ex) {
                loge("mIsDemoModeEnabledReceiver: Got exception in releasing semaphore, ex=" + ex);
            }
        }
    };

    private boolean mQueriedIsSatelliteProvisioned = false;
    private int mQueriedIsSatelliteProvisionedResultCode = SATELLITE_ERROR_NONE;
    private Semaphore mIsSatelliteProvisionedSemaphore = new Semaphore(0);
    private ResultReceiver mIsSatelliteProvisionedReceiver = new ResultReceiver(null) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mQueriedIsSatelliteProvisionedResultCode = resultCode;
            if (resultCode == SATELLITE_ERROR_NONE) {
                if (resultData.containsKey(KEY_SATELLITE_PROVISIONED)) {
                    mQueriedIsSatelliteProvisioned =
                            resultData.getBoolean(KEY_SATELLITE_PROVISIONED);
                } else {
                    loge("KEY_SATELLITE_PROVISIONED does not exist.");
                    mQueriedIsSatelliteProvisioned = false;
                }
            } else {
                mQueriedIsSatelliteProvisioned = false;
            }
            try {
                mIsSatelliteProvisionedSemaphore.release();
            } catch (Exception ex) {
                loge("mIsSatelliteProvisionedReceiver: Got exception in releasing semaphore ex="
                        + ex);
            }
        }
    };

    private boolean mQueriedSatelliteAllowed = false;
    private int mQueriedSatelliteAllowedResultCode = SATELLITE_ERROR_NONE;
    private Semaphore mSatelliteAllowedSemaphore = new Semaphore(0);
    private ResultReceiver mSatelliteAllowedReceiver = new ResultReceiver(null) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mQueriedSatelliteAllowedResultCode = resultCode;
            if (resultCode == SATELLITE_ERROR_NONE) {
                if (resultData.containsKey(KEY_SATELLITE_COMMUNICATION_ALLOWED)) {
                    mQueriedSatelliteAllowed = resultData.getBoolean(
                            KEY_SATELLITE_COMMUNICATION_ALLOWED);
                } else {
                    loge("KEY_SATELLITE_COMMUNICATION_ALLOWED does not exist.");
                    mQueriedSatelliteAllowed = false;
                }
            } else {
                logd("mSatelliteSupportReceiver: resultCode=" + resultCode);
                mQueriedSatelliteAllowed = false;
            }
            try {
                mSatelliteAllowedSemaphore.release();
            } catch (Exception ex) {
                loge("mSatelliteAllowedReceiver: Got exception in releasing semaphore, ex=" + ex);
            }
        }
    };

    private int mQueriedSatelliteVisibilityTime = -1;
    private int mSatelliteNextVisibilityTime = 3600;
    private int mQueriedSatelliteVisibilityTimeResultCode = SATELLITE_ERROR_NONE;
    private Semaphore mSatelliteVisibilityTimeSemaphore = new Semaphore(0);
    private ResultReceiver mSatelliteVisibilityTimeReceiver = new ResultReceiver(null) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mQueriedSatelliteVisibilityTimeResultCode = resultCode;
            if (resultCode == SATELLITE_ERROR_NONE) {
                if (resultData.containsKey(KEY_SATELLITE_NEXT_VISIBILITY)) {
                    mQueriedSatelliteVisibilityTime = resultData.getInt(
                            KEY_SATELLITE_NEXT_VISIBILITY);
                } else {
                    loge("KEY_SATELLITE_NEXT_VISIBILITY does not exist.");
                    mQueriedSatelliteVisibilityTime = -1;
                }
            } else {
                logd("mSatelliteSupportReceiver: resultCode=" + resultCode);
                mQueriedSatelliteVisibilityTime = -1;
            }
            try {
                mSatelliteVisibilityTimeSemaphore.release();
            } catch (Exception ex) {
                loge("mSatelliteAllowedReceiver: Got exception in releasing semaphore, ex=" + ex);
            }
        }
    };


    @Before
    public void setUp() throws Exception {
        super.setUp(getClass().getSimpleName());
        MockitoAnnotations.initMocks(this);
        logd(TAG + " Setup!");

        replaceInstance(DatagramController.class, "sInstance", null,
                mMockDatagramController);
        replaceInstance(SatelliteModemInterface.class, "sInstance", null,
                mMockSatelliteModemInterface);
        replaceInstance(SatelliteSessionController.class, "sInstance", null,
                mMockSatelliteSessionController);
        replaceInstance(PointingAppController.class, "sInstance", null,
                mMockPointingAppController);
        replaceInstance(ControllerMetricsStats.class, "sInstance", null,
                mMockControllerMetricsStats);
        replaceInstance(ProvisionMetricsStats.class, "sInstance", null,
                mMockProvisionMetricsStats);
        replaceInstance(SessionMetricsStats.class, "sInstance", null,
                mMockSessionMetricsStats);

        mSharedPreferences = new TestSharedPreferences();
        when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mSharedPreferences);
        doReturn(mIsSatelliteServiceSupported)
                .when(mMockSatelliteModemInterface).isSatelliteServiceSupported();
        setUpResponseForRequestSatelliteCapabilities(
                mSatelliteCapabilities, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_RADIO_NOT_AVAILABLE);
        doNothing().when(mMockDatagramController).setDemoMode(anyBoolean());
        doNothing().when(mMockSatelliteSessionController)
                .onSatelliteEnabledStateChanged(anyBoolean());
        doNothing().when(mMockSatelliteSessionController).setDemoMode(anyBoolean());
        doNothing().when(mMockControllerMetricsStats).onSatelliteEnabled();
        doNothing().when(mMockControllerMetricsStats).reportServiceEnablementSuccessCount();
        doNothing().when(mMockControllerMetricsStats).reportServiceEnablementFailCount();
        doReturn(mMockSessionMetricsStats)
                .when(mMockSessionMetricsStats).setInitializationResult(anyInt());
        doReturn(mMockSessionMetricsStats)
                .when(mMockSessionMetricsStats).setRadioTechnology(anyInt());
        doNothing().when(mMockSessionMetricsStats).reportSessionMetrics();

        doReturn(mMockProvisionMetricsStats).when(mMockProvisionMetricsStats)
                .setResultCode(anyInt());
        doReturn(mMockProvisionMetricsStats).when(mMockProvisionMetricsStats)
                    .setIsProvisionRequest(eq(false));
        doNothing().when(mMockProvisionMetricsStats).reportProvisionMetrics();
        doNothing().when(mMockControllerMetricsStats).reportDeprovisionCount(anyInt());
        mSatelliteControllerUT = new TestSatelliteController(mContext, Looper.myLooper());
        verify(mMockSatelliteModemInterface).registerForSatelliteProvisionStateChanged(
                any(Handler.class),
                eq(26) /* EVENT_SATELLITE_PROVISION_STATE_CHANGED */,
                eq(null));
        verify(mMockSatelliteModemInterface).registerForPendingDatagrams(
                any(Handler.class),
                eq(27) /* EVENT_PENDING_DATAGRAMS */,
                eq(null));
        verify(mMockSatelliteModemInterface).registerForSatelliteModemStateChanged(
                any(Handler.class),
                eq(28) /* EVENT_SATELLITE_MODEM_STATE_CHANGED */,
                eq(null));
    }

    @After
    public void tearDown() throws Exception {
        logd(TAG + " tearDown");
        mSatelliteControllerUT = null;
        super.tearDown();
    }

    @Test
    public void testRequestIsSatelliteCommunicationAllowedForCurrentLocation() {
        mSatelliteAllowedSemaphore.drainPermits();
        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestIsSatelliteCommunicationAllowedForCurrentLocation(SUB_ID,
                mSatelliteAllowedReceiver);
        processAllMessages();
        assertTrue(waitForRequestIsSatelliteAllowedForCurrentLocationResult(1));
        assertEquals(SATELLITE_NOT_SUPPORTED, mQueriedSatelliteAllowedResultCode);

        resetSatelliteControllerUT();
        mSatelliteControllerUT.requestIsSatelliteCommunicationAllowedForCurrentLocation(SUB_ID,
                mSatelliteAllowedReceiver);
        processAllMessages();
        assertTrue(waitForRequestIsSatelliteAllowedForCurrentLocationResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, mQueriedSatelliteAllowedResultCode);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteAllowedForCurrentLocation(true, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestIsSatelliteCommunicationAllowedForCurrentLocation(SUB_ID,
                mSatelliteAllowedReceiver);
        processAllMessages();
        assertTrue(waitForRequestIsSatelliteAllowedForCurrentLocationResult(1));
        assertEquals(SATELLITE_ERROR_NONE, mQueriedSatelliteAllowedResultCode);
        assertTrue(mQueriedSatelliteAllowed);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpNullResponseForRequestIsSatelliteAllowedForCurrentLocation(SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestIsSatelliteCommunicationAllowedForCurrentLocation(SUB_ID,
                mSatelliteAllowedReceiver);
        processAllMessages();
        assertTrue(waitForRequestIsSatelliteAllowedForCurrentLocationResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, mQueriedSatelliteAllowedResultCode);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpNullResponseForRequestIsSatelliteAllowedForCurrentLocation(
                SATELLITE_INVALID_MODEM_STATE);
        mSatelliteControllerUT.requestIsSatelliteCommunicationAllowedForCurrentLocation(SUB_ID,
                mSatelliteAllowedReceiver);
        processAllMessages();
        assertTrue(waitForRequestIsSatelliteAllowedForCurrentLocationResult(1));
        assertEquals(SATELLITE_INVALID_MODEM_STATE, mQueriedSatelliteAllowedResultCode);
    }

    @Test
    public void testRequestTimeForNextSatelliteVisibility() {
        mSatelliteVisibilityTimeSemaphore.drainPermits();
        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestTimeForNextSatelliteVisibility(SUB_ID,
                mSatelliteVisibilityTimeReceiver);
        processAllMessages();
        assertTrue(waitForRequestTimeForNextSatelliteVisibilityResult(1));
        assertEquals(SATELLITE_NOT_SUPPORTED, mQueriedSatelliteVisibilityTimeResultCode);

        resetSatelliteControllerUT();
        mSatelliteControllerUT.requestTimeForNextSatelliteVisibility(SUB_ID,
                mSatelliteVisibilityTimeReceiver);
        processAllMessages();
        assertTrue(waitForRequestTimeForNextSatelliteVisibilityResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, mQueriedSatelliteVisibilityTimeResultCode);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestTimeForNextSatelliteVisibility(mSatelliteNextVisibilityTime,
                SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestTimeForNextSatelliteVisibility(SUB_ID,
                mSatelliteVisibilityTimeReceiver);
        processAllMessages();
        assertTrue(waitForRequestTimeForNextSatelliteVisibilityResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, mQueriedSatelliteVisibilityTimeResultCode);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        setUpResponseForRequestTimeForNextSatelliteVisibility(mSatelliteNextVisibilityTime,
                SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestTimeForNextSatelliteVisibility(SUB_ID,
                mSatelliteVisibilityTimeReceiver);
        processAllMessages();
        assertTrue(waitForRequestTimeForNextSatelliteVisibilityResult(1));
        assertEquals(SATELLITE_SERVICE_NOT_PROVISIONED, mQueriedSatelliteVisibilityTimeResultCode);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestTimeForNextSatelliteVisibility(mSatelliteNextVisibilityTime,
                SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestTimeForNextSatelliteVisibility(SUB_ID,
                mSatelliteVisibilityTimeReceiver);
        processAllMessages();
        assertTrue(waitForRequestTimeForNextSatelliteVisibilityResult(1));
        assertEquals(SATELLITE_ERROR_NONE, mQueriedSatelliteVisibilityTimeResultCode);
        assertEquals(mSatelliteNextVisibilityTime, mQueriedSatelliteVisibilityTime);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        setUpNullResponseForRequestTimeForNextSatelliteVisibility(
                SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestTimeForNextSatelliteVisibility(SUB_ID,
                mSatelliteVisibilityTimeReceiver);
        processAllMessages();
        assertTrue(waitForRequestTimeForNextSatelliteVisibilityResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, mQueriedSatelliteVisibilityTimeResultCode);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        setUpNullResponseForRequestTimeForNextSatelliteVisibility(
                SATELLITE_INVALID_MODEM_STATE);
        mSatelliteControllerUT.requestTimeForNextSatelliteVisibility(SUB_ID,
                mSatelliteVisibilityTimeReceiver);
        processAllMessages();
        assertTrue(waitForRequestTimeForNextSatelliteVisibilityResult(1));
        assertEquals(SATELLITE_INVALID_MODEM_STATE, mQueriedSatelliteVisibilityTimeResultCode);
    }

    @Test
    public void testRequestSatelliteEnabled() {
        mIsSatelliteEnabledSemaphore.drainPermits();

        // Fail to enable satellite when SatelliteController is not fully loaded yet.
        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, (long) mIIntegerConsumerResults.get(0));

        // Fail to enable satellite when the device does not support satellite.
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_NOT_SUPPORTED, (long) mIIntegerConsumerResults.get(0));

        // Fail to enable satellite when the device is not provisioned yet.
        mIIntegerConsumerResults.clear();
        resetSatelliteControllerUT();
        verify(mMockSatelliteSessionController, times(1)).onSatelliteEnabledStateChanged(eq(false));
        verify(mMockSatelliteSessionController, times(1)).setDemoMode(eq(false));
        verify(mMockDatagramController, times(1)).setDemoMode(eq(false));
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_SERVICE_NOT_PROVISIONED, (long) mIIntegerConsumerResults.get(0));

        sendProvisionedStateChangedEvent(true, null);
        processAllMessages();
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);

        // Successfully disable satellite when radio is turned off.
        mSatelliteControllerUT.setSettingsKeyForSatelliteModeCalled = false;
        setUpResponseForRequestSatelliteEnabled(false, false, SATELLITE_ERROR_NONE);
        setRadioPower(false);
        processAllMessages();
        verifySatelliteEnabled(false, SATELLITE_ERROR_NONE);
        assertTrue(mSatelliteControllerUT.setSettingsKeyForSatelliteModeCalled);
        assertEquals(
                SATELLITE_MODE_ENABLED_FALSE, mSatelliteControllerUT.satelliteModeSettingValue);
        verify(mMockSatelliteSessionController, times(2)).onSatelliteEnabledStateChanged(eq(false));
        verify(mMockSatelliteSessionController, times(2)).setDemoMode(eq(false));
        verify(mMockDatagramController, times(2)).setDemoMode(eq(false));
        verify(mMockControllerMetricsStats, times(1)).onSatelliteDisabled();

        // Fail to enable satellite when radio is off.
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestSatelliteEnabled(true, false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        // Radio is not on, can not enable satellite
        assertEquals(SATELLITE_INVALID_MODEM_STATE, (long) mIIntegerConsumerResults.get(0));

        setRadioPower(true);
        processAllMessages();
        verifySatelliteEnabled(false, SATELLITE_ERROR_NONE);

        // Fail to enable satellite with an error response from modem when radio is on.
        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.setSettingsKeyForSatelliteModeCalled = false;
        setUpResponseForRequestSatelliteEnabled(true, false, SATELLITE_INVALID_MODEM_STATE);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_MODEM_STATE, (long) mIIntegerConsumerResults.get(0));
        verifySatelliteEnabled(false, SATELLITE_ERROR_NONE);
        verify(mMockPointingAppController, never()).startPointingUI(anyBoolean());
        assertFalse(mSatelliteControllerUT.setSettingsKeyForSatelliteModeCalled);
        verify(mMockControllerMetricsStats, times(1)).reportServiceEnablementFailCount();

        // Successfully enable satellite when radio is on.
        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.setSettingsKeyForSatelliteModeCalled = false;
        setUpResponseForRequestSatelliteEnabled(true, false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        verifySatelliteEnabled(true, SATELLITE_ERROR_NONE);
        assertTrue(mSatelliteControllerUT.setSettingsKeyForSatelliteModeCalled);
        assertEquals(SATELLITE_MODE_ENABLED_TRUE, mSatelliteControllerUT.satelliteModeSettingValue);
        verify(mMockPointingAppController).startPointingUI(eq(false));
        verify(mMockSatelliteSessionController, times(1)).onSatelliteEnabledStateChanged(eq(true));
        verify(mMockSatelliteSessionController, times(3)).setDemoMode(eq(false));
        verify(mMockDatagramController, times(3)).setDemoMode(eq(false));
        verify(mMockControllerMetricsStats, times(1)).onSatelliteEnabled();
        verify(mMockControllerMetricsStats, times(1)).reportServiceEnablementSuccessCount();
        verify(mMockSessionMetricsStats, times(2)).setInitializationResult(anyInt());
        verify(mMockSessionMetricsStats, times(2)).setRadioTechnology(anyInt());
        verify(mMockSessionMetricsStats, times(2)).reportSessionMetrics();

        // Successfully enable satellite when it is already enabled.
        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        verifySatelliteEnabled(true, SATELLITE_ERROR_NONE);

        // Fail to enable satellite with a different demo mode when it is already enabled.
        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, true, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_ARGUMENTS, (long) mIIntegerConsumerResults.get(0));
        verifySatelliteEnabled(true, SATELLITE_ERROR_NONE);

        // Disable satellite.
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestSatelliteEnabled(false, false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, false, false, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        verifySatelliteEnabled(false, SATELLITE_ERROR_NONE);

        // Disable satellite when satellite is already disabled.
        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, false, false, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        verifySatelliteEnabled(false, SATELLITE_ERROR_NONE);

        // Disable satellite with a different demo mode when satellite is already disabled.
        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, false, true, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        verifySatelliteEnabled(false, SATELLITE_ERROR_NONE);

        // Send a second request while the first request in progress
        mIIntegerConsumerResults.clear();
        setUpNoResponseForRequestSatelliteEnabled(true, false);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertFalse(waitForIIntegerConsumerResult(1));
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_REQUEST_IN_PROGRESS, (long) mIIntegerConsumerResults.get(0));

        mIIntegerConsumerResults.clear();
        resetSatelliteControllerUTToSupportedAndProvisionedState();
        // Should receive callback for the above request when satellite modem is turned off.
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_MODEM_STATE, (long) mIIntegerConsumerResults.get(0));

        // Move to satellite-disabling in progress.
        setUpNoResponseForRequestSatelliteEnabled(false, false);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, false, false, mIIntegerConsumer);
        processAllMessages();
        assertFalse(waitForIIntegerConsumerResult(1));

        // Disable is in progress. Thus, a new request to enable satellite will be rejected.
        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR, (long) mIIntegerConsumerResults.get(0));

        mIIntegerConsumerResults.clear();
        resetSatelliteControllerUTToOffAndProvisionedState();
        // Should receive callback for the above request when satellite modem is turned off.
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_MODEM_STATE, (long) mIIntegerConsumerResults.get(0));

        /**
         * Make areAllRadiosDisabled return false and move mWaitingForRadioDisabled to true, which
         * will lead to no response for requestSatelliteEnabled.
         */
        mSatelliteControllerUT.allRadiosDisabled = false;
        setUpResponseForRequestSatelliteEnabled(true, false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertFalse(waitForIIntegerConsumerResult(1));

        resetSatelliteControllerUTEnabledState();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestSatelliteEnabled(false, false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, false, false, mIIntegerConsumer);
        processAllMessages();
        // We should receive 2 callbacks for the above 2 requests.
        assertTrue(waitForIIntegerConsumerResult(2));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(1));

        resetSatelliteControllerUTToOffAndProvisionedState();

        // Repeat the same test as above but with error response from modem for the second request
        mSatelliteControllerUT.allRadiosDisabled = false;
        setUpResponseForRequestSatelliteEnabled(true, false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertFalse(waitForIIntegerConsumerResult(1));

        resetSatelliteControllerUTEnabledState();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestSatelliteEnabled(false, false, SATELLITE_NO_RESOURCES);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, false, false, mIIntegerConsumer);
        processAllMessages();
        // We should receive 2 callbacks for the above 2 requests.
        assertTrue(waitForIIntegerConsumerResult(2));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        assertEquals(SATELLITE_NO_RESOURCES, (long) mIIntegerConsumerResults.get(1));
        mSatelliteControllerUT.allRadiosDisabled = true;
    }

    @Test
    public void testRequestSatelliteCapabilities() {
        mSatelliteCapabilitiesSemaphore.drainPermits();
        mSatelliteControllerUT.requestSatelliteCapabilities(SUB_ID, mSatelliteCapabilitiesReceiver);
        processAllMessages();
        assertTrue(waitForRequestSatelliteCapabilitiesResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, mQueriedSatelliteCapabilitiesResultCode);

        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteCapabilities(SUB_ID, mSatelliteCapabilitiesReceiver);
        processAllMessages();
        assertTrue(waitForRequestSatelliteCapabilitiesResult(1));
        assertEquals(SATELLITE_NOT_SUPPORTED, mQueriedSatelliteCapabilitiesResultCode);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestSatelliteCapabilities(mSatelliteCapabilities, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteCapabilities(SUB_ID, mSatelliteCapabilitiesReceiver);
        processAllMessages();
        assertTrue(waitForRequestSatelliteCapabilitiesResult(1));
        assertEquals(SATELLITE_ERROR_NONE, mQueriedSatelliteCapabilitiesResultCode);
        assertEquals(mSatelliteCapabilities, mQueriedSatelliteCapabilities);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpNullResponseForRequestSatelliteCapabilities(SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteCapabilities(SUB_ID, mSatelliteCapabilitiesReceiver);
        processAllMessages();
        assertTrue(waitForRequestSatelliteCapabilitiesResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, mQueriedSatelliteCapabilitiesResultCode);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpNullResponseForRequestSatelliteCapabilities(SATELLITE_INVALID_MODEM_STATE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteCapabilities(SUB_ID, mSatelliteCapabilitiesReceiver);
        processAllMessages();
        assertTrue(waitForRequestSatelliteCapabilitiesResult(1));
        assertEquals(SATELLITE_INVALID_MODEM_STATE, mQueriedSatelliteCapabilitiesResultCode);
    }

    @Test
    public void testStartSatelliteTransmissionUpdates() {
        mIIntegerConsumerSemaphore.drainPermits();
        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.startSatelliteTransmissionUpdates(SUB_ID, mIIntegerConsumer,
                mStartTransmissionUpdateCallback);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, (long) mIIntegerConsumerResults.get(0));

        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.startSatelliteTransmissionUpdates(SUB_ID, mIIntegerConsumer,
                mStartTransmissionUpdateCallback);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_NOT_SUPPORTED, (long) mIIntegerConsumerResults.get(0));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.startSatelliteTransmissionUpdates(SUB_ID, mIIntegerConsumer,
                mStartTransmissionUpdateCallback);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, (long) mIIntegerConsumerResults.get(0));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        setUpResponseForStartSatelliteTransmissionUpdates(SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.startSatelliteTransmissionUpdates(SUB_ID, mIIntegerConsumer,
                mStartTransmissionUpdateCallback);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_SERVICE_NOT_PROVISIONED, (long) mIIntegerConsumerResults.get(0));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        setUpResponseForStartSatelliteTransmissionUpdates(SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.startSatelliteTransmissionUpdates(SUB_ID, mIIntegerConsumer,
                mStartTransmissionUpdateCallback);
        verify(mMockPointingAppController).registerForSatelliteTransmissionUpdates(anyInt(),
                eq(mStartTransmissionUpdateCallback), any());
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        verify(mMockPointingAppController).startSatelliteTransmissionUpdates(any(Message.class),
                any(Phone.class));
        verify(mMockPointingAppController).setStartedSatelliteTransmissionUpdates(eq(true));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        setUpResponseForStartSatelliteTransmissionUpdates(SATELLITE_INVALID_TELEPHONY_STATE);
        mSatelliteControllerUT.startSatelliteTransmissionUpdates(SUB_ID, mIIntegerConsumer,
                mStartTransmissionUpdateCallback);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, (long) mIIntegerConsumerResults.get(0));
        verify(mMockPointingAppController).unregisterForSatelliteTransmissionUpdates(anyInt(),
                any(),  eq(mStartTransmissionUpdateCallback), any(Phone.class));
        verify(mMockPointingAppController).setStartedSatelliteTransmissionUpdates(eq(false));
    }

    @Test
    public void testStopSatelliteTransmissionUpdates() {
        mIIntegerConsumerSemaphore.drainPermits();
        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.stopSatelliteTransmissionUpdates(SUB_ID, mIIntegerConsumer,
                mStopTransmissionUpdateCallback);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, (long) mIIntegerConsumerResults.get(0));

        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.stopSatelliteTransmissionUpdates(SUB_ID, mIIntegerConsumer,
                mStopTransmissionUpdateCallback);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_NOT_SUPPORTED, (long) mIIntegerConsumerResults.get(0));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.stopSatelliteTransmissionUpdates(SUB_ID, mIIntegerConsumer,
                mStopTransmissionUpdateCallback);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, (long) mIIntegerConsumerResults.get(0));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        setUpResponseForStopSatelliteTransmissionUpdates(SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.stopSatelliteTransmissionUpdates(SUB_ID, mIIntegerConsumer,
                mStopTransmissionUpdateCallback);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_SERVICE_NOT_PROVISIONED, (long) mIIntegerConsumerResults.get(0));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        setUpResponseForStopSatelliteTransmissionUpdates(SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.stopSatelliteTransmissionUpdates(SUB_ID, mIIntegerConsumer,
                mStopTransmissionUpdateCallback);
        verify(mMockPointingAppController).unregisterForSatelliteTransmissionUpdates(anyInt(),
                any(),  eq(mStopTransmissionUpdateCallback), any(Phone.class));
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        verify(mMockPointingAppController).stopSatelliteTransmissionUpdates(any(Message.class),
                any(Phone.class));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        setUpResponseForStopSatelliteTransmissionUpdates(SATELLITE_INVALID_TELEPHONY_STATE);
        mSatelliteControllerUT.stopSatelliteTransmissionUpdates(SUB_ID, mIIntegerConsumer,
                mStopTransmissionUpdateCallback);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, (long) mIIntegerConsumerResults.get(0));
    }

    @Test
    public void testRequestIsDemoModeEnabled() {
        mIsDemoModeEnabledSemaphore.drainPermits();
        resetSatelliteControllerUT();
        mSatelliteControllerUT.requestIsDemoModeEnabled(SUB_ID, mIsDemoModeEnabledReceiver);
        assertTrue(waitForRequestIsDemoModeEnabledResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, mQueriedIsDemoModeEnabledResultCode);
        assertFalse(mQueriedIsDemoModeEnabled);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestIsDemoModeEnabled(SUB_ID, mIsDemoModeEnabledReceiver);
        assertTrue(waitForRequestIsDemoModeEnabledResult(1));
        assertEquals(SATELLITE_NOT_SUPPORTED, mQueriedIsDemoModeEnabledResultCode);
        assertFalse(mQueriedIsDemoModeEnabled);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestIsDemoModeEnabled(SUB_ID, mIsDemoModeEnabledReceiver);
        assertTrue(waitForRequestIsDemoModeEnabledResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, mQueriedIsDemoModeEnabledResultCode);
        assertFalse(mQueriedIsDemoModeEnabled);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestIsDemoModeEnabled(SUB_ID, mIsDemoModeEnabledReceiver);
        assertTrue(waitForRequestIsDemoModeEnabledResult(1));
        assertEquals(SATELLITE_SERVICE_NOT_PROVISIONED, mQueriedIsDemoModeEnabledResultCode);
        assertFalse(mQueriedIsDemoModeEnabled);

        resetSatelliteControllerUT();
        boolean isDemoModeEnabled = mSatelliteControllerUT.isDemoModeEnabled();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestIsDemoModeEnabled(SUB_ID, mIsDemoModeEnabledReceiver);
        assertTrue(waitForRequestIsDemoModeEnabledResult(1));
        assertEquals(SATELLITE_ERROR_NONE, mQueriedIsDemoModeEnabledResultCode);
        assertEquals(isDemoModeEnabled, mQueriedIsDemoModeEnabled);
    }

    @Test
    public void testIsSatelliteEnabled() {
        assertFalse(mSatelliteControllerUT.isSatelliteEnabled());
        setUpResponseForRequestIsSatelliteEnabled(true, SATELLITE_ERROR_NONE);
        mIsSatelliteEnabledSemaphore.drainPermits();
        mSatelliteControllerUT.requestIsSatelliteEnabled(SUB_ID, mIsSatelliteEnabledReceiver);
        processAllMessages();
        assertTrue(waitForRequestIsSatelliteEnabledResult(1));
        assertEquals(mSatelliteControllerUT.isSatelliteEnabled(), mQueriedIsSatelliteEnabled);
    }

    @Test
    public void testOnSatelliteServiceConnected() {
        verifySatelliteSupported(false, SATELLITE_RADIO_NOT_AVAILABLE);
        verifySatelliteEnabled(false, SATELLITE_INVALID_TELEPHONY_STATE);
        verifySatelliteProvisioned(false, SATELLITE_INVALID_TELEPHONY_STATE);

        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestSatelliteEnabled(false, false, SATELLITE_ERROR_NONE);

        mSatelliteControllerUT.onSatelliteServiceConnected();
        processAllMessages();

        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteEnabled(false, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
    }

    @Test
    public void testRegisterForSatelliteModemStateChanged() {
        ISatelliteStateCallback callback = new ISatelliteStateCallback.Stub() {
            @Override
            public void onSatelliteModemStateChanged(int state) {
                logd("onSatelliteModemStateChanged: state=" + state);
            }
        };
        int errorCode = mSatelliteControllerUT.registerForSatelliteModemStateChanged(
                SUB_ID, callback);
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, errorCode);
        verify(mMockSatelliteSessionController, never())
                .registerForSatelliteModemStateChanged(callback);

        resetSatelliteControllerUTToSupportedAndProvisionedState();

        errorCode = mSatelliteControllerUT.registerForSatelliteModemStateChanged(
                SUB_ID, callback);
        assertEquals(SATELLITE_ERROR_NONE, errorCode);
        verify(mMockSatelliteSessionController).registerForSatelliteModemStateChanged(callback);
    }

    @Test
    public void testUnregisterForSatelliteModemStateChanged() {
        ISatelliteStateCallback callback = new ISatelliteStateCallback.Stub() {
            @Override
            public void onSatelliteModemStateChanged(int state) {
                logd("onSatelliteModemStateChanged: state=" + state);
            }
        };
        mSatelliteControllerUT.unregisterForSatelliteModemStateChanged(SUB_ID, callback);
        verify(mMockSatelliteSessionController, never())
                .unregisterForSatelliteModemStateChanged(callback);

        resetSatelliteControllerUTToSupportedAndProvisionedState();

        mSatelliteControllerUT.unregisterForSatelliteModemStateChanged(SUB_ID, callback);
        verify(mMockSatelliteSessionController).unregisterForSatelliteModemStateChanged(callback);
    }

    @Test
    public void testRegisterForSatelliteProvisionStateChanged() {
        Semaphore semaphore = new Semaphore(0);
        ISatelliteProvisionStateCallback callback =
                new ISatelliteProvisionStateCallback.Stub() {
                    @Override
                    public void onSatelliteProvisionStateChanged(boolean provisioned) {
                        logd("onSatelliteProvisionStateChanged: provisioned=" + provisioned);
                        try {
                            semaphore.release();
                        } catch (Exception ex) {
                            loge("onSatelliteProvisionStateChanged: Got exception in releasing "
                                    + "semaphore, ex=" + ex);
                        }
                    }
                };
        int errorCode = mSatelliteControllerUT.registerForSatelliteProvisionStateChanged(
                SUB_ID, callback);
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, errorCode);

        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(false, SATELLITE_ERROR_NONE);
        errorCode = mSatelliteControllerUT.registerForSatelliteProvisionStateChanged(
                SUB_ID, callback);
        assertEquals(SATELLITE_NOT_SUPPORTED, errorCode);

        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        errorCode = mSatelliteControllerUT.registerForSatelliteProvisionStateChanged(
                SUB_ID, callback);
        assertEquals(SATELLITE_ERROR_NONE, errorCode);

        sendProvisionedStateChangedEvent(true, null);
        processAllMessages();
        assertTrue(waitForForEvents(
                semaphore, 1, "testRegisterForSatelliteProvisionStateChanged"));

        mSatelliteControllerUT.unregisterForSatelliteProvisionStateChanged(SUB_ID, callback);
        sendProvisionedStateChangedEvent(true, null);
        processAllMessages();
        assertFalse(waitForForEvents(
                semaphore, 1, "testRegisterForSatelliteProvisionStateChanged"));
    }

    @Test
    public void testRegisterForSatelliteDatagram() {
        ISatelliteDatagramCallback callback =
                new ISatelliteDatagramCallback.Stub() {
                    @Override
                    public void onSatelliteDatagramReceived(long datagramId,
                            @NonNull SatelliteDatagram datagram, int pendingCount,
                            @NonNull IVoidConsumer internalAck) {
                        logd("onSatelliteDatagramReceived");
                    }
                };
        when(mMockDatagramController.registerForSatelliteDatagram(eq(SUB_ID), eq(callback)))
                .thenReturn(SATELLITE_ERROR_NONE);
        int errorCode = mSatelliteControllerUT.registerForSatelliteDatagram(SUB_ID, callback);
        assertEquals(SATELLITE_ERROR_NONE, errorCode);
        verify(mMockDatagramController).registerForSatelliteDatagram(eq(SUB_ID), eq(callback));
    }

    @Test
    public void testUnregisterForSatelliteDatagram() {
        ISatelliteDatagramCallback callback =
                new ISatelliteDatagramCallback.Stub() {
                    @Override
                    public void onSatelliteDatagramReceived(long datagramId,
                            @NonNull SatelliteDatagram datagram, int pendingCount,
                            @NonNull IVoidConsumer internalAck) {
                        logd("onSatelliteDatagramReceived");
                    }
                };
        doNothing().when(mMockDatagramController)
                .unregisterForSatelliteDatagram(eq(SUB_ID), eq(callback));
        mSatelliteControllerUT.unregisterForSatelliteDatagram(SUB_ID, callback);
        verify(mMockDatagramController).unregisterForSatelliteDatagram(eq(SUB_ID), eq(callback));
    }

    @Test
    public void testSendSatelliteDatagram() {
        String mText = "This is a test datagram message from user";
        SatelliteDatagram datagram = new SatelliteDatagram(mText.getBytes());

        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.sendSatelliteDatagram(SUB_ID,
                SatelliteManager.DATAGRAM_TYPE_SOS_MESSAGE, datagram, true, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, (long) mIIntegerConsumerResults.get(0));
        verify(mMockDatagramController, never()).sendSatelliteDatagram(anyInt(),
                eq(SatelliteManager.DATAGRAM_TYPE_SOS_MESSAGE), eq(datagram), eq(true),
                any());

        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        sendProvisionedStateChangedEvent(false, null);
        processAllMessages();
        verifySatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.sendSatelliteDatagram(SUB_ID,
                SatelliteManager.DATAGRAM_TYPE_SOS_MESSAGE, datagram, true, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_SERVICE_NOT_PROVISIONED, (long) mIIntegerConsumerResults.get(0));
        verify(mMockDatagramController, never()).sendSatelliteDatagram(anyInt(),
                eq(SatelliteManager.DATAGRAM_TYPE_SOS_MESSAGE), eq(datagram), eq(true),
                any());

        mIIntegerConsumerResults.clear();
        sendProvisionedStateChangedEvent(true, null);
        processAllMessages();
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.sendSatelliteDatagram(SUB_ID,
                SatelliteManager.DATAGRAM_TYPE_SOS_MESSAGE, datagram, true, mIIntegerConsumer);
        processAllMessages();
        assertFalse(waitForIIntegerConsumerResult(1));
        verify(mMockDatagramController, times(1)).sendSatelliteDatagram(anyInt(),
                eq(SatelliteManager.DATAGRAM_TYPE_SOS_MESSAGE), eq(datagram), eq(true),
                any());
        verify(mMockPointingAppController, times(1)).startPointingUI(eq(true));
    }

    @Test
    public void testPollPendingSatelliteDatagrams() {
        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.pollPendingSatelliteDatagrams(SUB_ID, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, (long) mIIntegerConsumerResults.get(0));
        verify(mMockDatagramController, never()).pollPendingSatelliteDatagrams(anyInt(), any());

        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        sendProvisionedStateChangedEvent(false, null);
        processAllMessages();
        verifySatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.pollPendingSatelliteDatagrams(SUB_ID, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_SERVICE_NOT_PROVISIONED, (long) mIIntegerConsumerResults.get(0));
        verify(mMockDatagramController, never()).pollPendingSatelliteDatagrams(anyInt(), any());

        mIIntegerConsumerResults.clear();
        sendProvisionedStateChangedEvent(true, null);
        processAllMessages();
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.pollPendingSatelliteDatagrams(SUB_ID, mIIntegerConsumer);
        processAllMessages();
        assertFalse(waitForIIntegerConsumerResult(1));
        verify(mMockDatagramController, times(1)).pollPendingSatelliteDatagrams(anyInt(), any());
    }

    @Test
    public void testProvisionSatelliteService() {
        String mText = "This is test provision data.";
        byte[] testProvisionData = mText.getBytes();
        CancellationSignal cancellationSignal = new CancellationSignal();
        ICancellationSignal cancelRemote = null;
        mIIntegerConsumerResults.clear();
        cancelRemote = mSatelliteControllerUT.provisionSatelliteService(SUB_ID,
                TEST_SATELLITE_TOKEN,
                testProvisionData, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, (long) mIIntegerConsumerResults.get(0));
        assertNull(cancelRemote);

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(false, SATELLITE_ERROR_NONE);
        cancelRemote = mSatelliteControllerUT.provisionSatelliteService(SUB_ID,
                TEST_SATELLITE_TOKEN,
                testProvisionData, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_NOT_SUPPORTED, (long) mIIntegerConsumerResults.get(0));
        assertNull(cancelRemote);

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        cancelRemote = mSatelliteControllerUT.provisionSatelliteService(SUB_ID,
                TEST_SATELLITE_TOKEN,
                testProvisionData, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        assertNull(cancelRemote);

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        setUpResponseForProvisionSatelliteService(TEST_SATELLITE_TOKEN, testProvisionData,
                SATELLITE_ERROR_NONE);
        cancelRemote = mSatelliteControllerUT.provisionSatelliteService(SUB_ID,
                TEST_SATELLITE_TOKEN,
                testProvisionData, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        assertNotNull(cancelRemote);

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        setUpResponseForProvisionSatelliteService(TEST_SATELLITE_TOKEN, testProvisionData,
                SATELLITE_NOT_AUTHORIZED);
        cancelRemote = mSatelliteControllerUT.provisionSatelliteService(SUB_ID,
                TEST_SATELLITE_TOKEN,
                testProvisionData, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_NOT_AUTHORIZED, (long) mIIntegerConsumerResults.get(0));
        assertNotNull(cancelRemote);

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        setUpResponseForProvisionSatelliteService(TEST_NEXT_SATELLITE_TOKEN, testProvisionData,
                SATELLITE_ERROR_NONE);
        cancelRemote = mSatelliteControllerUT.provisionSatelliteService(SUB_ID,
                TEST_NEXT_SATELLITE_TOKEN, testProvisionData, mIIntegerConsumer);
        cancellationSignal.setRemote(cancelRemote);
        cancellationSignal.cancel();
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        verify(mMockSatelliteModemInterface).deprovisionSatelliteService(
                eq(TEST_NEXT_SATELLITE_TOKEN), any(Message.class));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        setUpNoResponseForProvisionSatelliteService(TEST_SATELLITE_TOKEN);
        setUpResponseForProvisionSatelliteService(TEST_NEXT_SATELLITE_TOKEN, testProvisionData,
                SATELLITE_ERROR_NONE);
        cancelRemote = mSatelliteControllerUT.provisionSatelliteService(SUB_ID,
                TEST_SATELLITE_TOKEN,
                testProvisionData, mIIntegerConsumer);
        cancelRemote = mSatelliteControllerUT.provisionSatelliteService(SUB_ID,
                TEST_NEXT_SATELLITE_TOKEN,
                testProvisionData, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_SERVICE_PROVISION_IN_PROGRESS,
                (long) mIIntegerConsumerResults.get(0));
    }

    @Test
    public void testDeprovisionSatelliteService() {
        mIIntegerConsumerSemaphore.drainPermits();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.deprovisionSatelliteService(SUB_ID,
                TEST_SATELLITE_TOKEN, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_NOT_SUPPORTED, (long) mIIntegerConsumerResults.get(0));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        mSatelliteControllerUT.deprovisionSatelliteService(SUB_ID,
                 TEST_SATELLITE_TOKEN, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, (long) mIIntegerConsumerResults.get(0));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForDeprovisionSatelliteService(TEST_SATELLITE_TOKEN, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.deprovisionSatelliteService(SUB_ID,
                TEST_SATELLITE_TOKEN, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_TELEPHONY_STATE, (long) mIIntegerConsumerResults.get(0));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(false, SATELLITE_ERROR_NONE);
        setUpResponseForDeprovisionSatelliteService(TEST_SATELLITE_TOKEN, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.deprovisionSatelliteService(SUB_ID,
                TEST_SATELLITE_TOKEN, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        setUpResponseForDeprovisionSatelliteService(TEST_SATELLITE_TOKEN, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.deprovisionSatelliteService(SUB_ID,
                TEST_SATELLITE_TOKEN, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));

        resetSatelliteControllerUT();
        mIIntegerConsumerResults.clear();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        setUpResponseForRequestIsSatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
        setUpResponseForDeprovisionSatelliteService(TEST_SATELLITE_TOKEN,
                SATELLITE_INVALID_MODEM_STATE);
        mSatelliteControllerUT.deprovisionSatelliteService(SUB_ID,
                TEST_SATELLITE_TOKEN, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_INVALID_MODEM_STATE, (long) mIIntegerConsumerResults.get(0));

    }

    private void resetSatelliteControllerUTEnabledState() {
        logd("resetSatelliteControllerUTEnabledState");
        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_RADIO_NOT_AVAILABLE);
        doReturn(true).when(mMockSatelliteModemInterface)
                .setSatelliteServicePackageName(anyString());
        mSatelliteControllerUT.setSatelliteServicePackageName("TestSatelliteService");
        processAllMessages();

        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        sendProvisionedStateChangedEvent(true, null);
        processAllMessages();
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
    }

    private void resetSatelliteControllerUT() {
        logd("resetSatelliteControllerUT");
        // Trigger cleanUpResources
        sendSatelliteModemStateChangedEvent(SATELLITE_MODEM_STATE_UNAVAILABLE, null);
        processAllMessages();

        // Reset all cached states
        setUpResponseForRequestIsSatelliteSupported(false, SATELLITE_RADIO_NOT_AVAILABLE);
        doReturn(true).when(mMockSatelliteModemInterface)
                .setSatelliteServicePackageName(anyString());
        mSatelliteControllerUT.setSatelliteServicePackageName("TestSatelliteService");
        processAllMessages();
    }

    private void resetSatelliteControllerUTToSupportedAndProvisionedState() {
        resetSatelliteControllerUT();
        setUpResponseForRequestIsSatelliteSupported(true, SATELLITE_ERROR_NONE);
        verifySatelliteSupported(true, SATELLITE_ERROR_NONE);
        sendProvisionedStateChangedEvent(true, null);
        processAllMessages();
        verifySatelliteProvisioned(true, SATELLITE_ERROR_NONE);
    }

    private void resetSatelliteControllerUTToOffAndProvisionedState() {
        resetSatelliteControllerUTToSupportedAndProvisionedState();
        sendSatelliteModemStateChangedEvent(SATELLITE_MODEM_STATE_OFF, null);
        processAllMessages();
        verifySatelliteEnabled(false, SATELLITE_ERROR_NONE);
    }

    private void resetSatelliteControllerUTToOnAndProvisionedState() {
        resetSatelliteControllerUTToOffAndProvisionedState();
        setRadioPower(true);
        processAllMessages();

        setUpResponseForRequestSatelliteEnabled(true, false, SATELLITE_ERROR_NONE);
        mSatelliteControllerUT.requestSatelliteEnabled(SUB_ID, true, false, mIIntegerConsumer);
        processAllMessages();
        assertTrue(waitForIIntegerConsumerResult(1));
        assertEquals(SATELLITE_ERROR_NONE, (long) mIIntegerConsumerResults.get(0));
        verifySatelliteEnabled(true, SATELLITE_ERROR_NONE);
    }

    private void setUpResponseForRequestIsSatelliteEnabled(boolean isSatelliteEnabled,
            @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[0];
            AsyncResult.forMessage(message, isSatelliteEnabled, exception);
            message.sendToTarget();
            return null;
        }).when(mMockSatelliteModemInterface).requestIsSatelliteEnabled(any(Message.class));
    }

    private void setUpResponseForRequestIsSatelliteSupported(
            boolean isSatelliteSupported, @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[0];
            AsyncResult.forMessage(message, isSatelliteSupported, exception);
            message.sendToTarget();
            return null;
        }).when(mMockSatelliteModemInterface).requestIsSatelliteSupported(any(Message.class));
    }

    private void setUpResponseForRequestIsSatelliteAllowedForCurrentLocation(
            boolean isSatelliteAllowed, @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[0];
            AsyncResult.forMessage(message, isSatelliteAllowed, exception);
            message.sendToTarget();
            return null;
        }).when(mMockSatelliteModemInterface)
                .requestIsSatelliteCommunicationAllowedForCurrentLocation(any(Message.class));
    }

    private void setUpNullResponseForRequestIsSatelliteAllowedForCurrentLocation(
            @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[0];
            AsyncResult.forMessage(message, null, exception);
            message.sendToTarget();
            return null;
        }).when(mMockSatelliteModemInterface)
                .requestIsSatelliteCommunicationAllowedForCurrentLocation(any(Message.class));
    }

    private void setUpResponseForRequestTimeForNextSatelliteVisibility(
            int satelliteVisibilityTime, @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        int[] visibilityTime = new int[] {satelliteVisibilityTime};
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[0];
            AsyncResult.forMessage(message, visibilityTime, exception);
            message.sendToTarget();
            return null;
        }).when(mMockSatelliteModemInterface)
                .requestTimeForNextSatelliteVisibility(any(Message.class));
    }

    private void setUpNullResponseForRequestTimeForNextSatelliteVisibility(
            @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[0];
            AsyncResult.forMessage(message, null, exception);
            message.sendToTarget();
            return null;
        }).when(mMockSatelliteModemInterface)
                .requestTimeForNextSatelliteVisibility(any(Message.class));
    }

    private void setUpResponseForRequestIsSatelliteProvisioned(
            boolean isSatelliteProvisioned, @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        int[] provisioned = new int[] {isSatelliteProvisioned ? 1 : 0};
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[0];
            AsyncResult.forMessage(message, provisioned, exception);
            message.sendToTarget();
            return null;
        }).when(mMockSatelliteModemInterface).requestIsSatelliteProvisioned(any(Message.class));
    }

    private void setUpResponseForRequestSatelliteEnabled(
            boolean enabled, boolean demoMode, @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[2];
            AsyncResult.forMessage(message, null, exception);
            message.sendToTarget();
            return null;
        }).when(mMockSatelliteModemInterface)
                .requestSatelliteEnabled(eq(enabled), eq(demoMode), any(Message.class));
    }

    private void setUpNoResponseForRequestSatelliteEnabled(boolean enabled, boolean demoMode) {
        doNothing().when(mMockSatelliteModemInterface)
                .requestSatelliteEnabled(eq(enabled), eq(demoMode), any(Message.class));
    }

    private void setUpResponseForProvisionSatelliteService(
            String token, byte[] provisionData, @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[2];
            AsyncResult.forMessage(message, null, exception);
            message.sendToTarget();
            return null;
        }).when(mMockSatelliteModemInterface)
                .provisionSatelliteService(eq(token), any(byte[].class), any(Message.class));
    }

    private void setUpNoResponseForProvisionSatelliteService(String token) {
        doNothing().when(mMockSatelliteModemInterface)
                .provisionSatelliteService(eq(token), any(), any(Message.class));
    }

    private void setUpResponseForDeprovisionSatelliteService(String token,
            @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[1];
            AsyncResult.forMessage(message, null, exception);
            message.sendToTarget();
            return null;
        }).when(mMockSatelliteModemInterface)
                .deprovisionSatelliteService(eq(token), any(Message.class));
    }

    private void setUpResponseForRequestSatelliteCapabilities(
            SatelliteCapabilities satelliteCapabilities,
            @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[0];
            AsyncResult.forMessage(message, satelliteCapabilities, exception);
            message.sendToTarget();
            return null;
        }).when(mMockSatelliteModemInterface).requestSatelliteCapabilities(any(Message.class));
    }

    private boolean waitForForEvents(
            Semaphore semaphore, int expectedNumberOfEvents, String caller) {
        for (int i = 0; i < expectedNumberOfEvents; i++) {
            try {
                if (!semaphore.tryAcquire(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    loge(caller + ": Timeout to receive the expected event");
                    return false;
                }
            } catch (Exception ex) {
                loge(caller + ": Got exception=" + ex);
                return false;
            }
        }
        return true;
    }

    private void setUpNullResponseForRequestSatelliteCapabilities(
            @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[0];
            AsyncResult.forMessage(message, null, exception);
            message.sendToTarget();
            return null;
        }).when(mMockSatelliteModemInterface).requestSatelliteCapabilities(any(Message.class));
    }

    private void setUpResponseForStartSatelliteTransmissionUpdates(
            @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[0];
            AsyncResult.forMessage(message, null, exception);
            message.sendToTarget();
            return null;
        }).when(mMockPointingAppController).startSatelliteTransmissionUpdates(any(Message.class),
                any());
    }

    private void setUpResponseForStopSatelliteTransmissionUpdates(
            @SatelliteManager.SatelliteError int error) {
        SatelliteException exception = (error == SATELLITE_ERROR_NONE)
                ? null : new SatelliteException(error);
        doAnswer(invocation -> {
            Message message = (Message) invocation.getArguments()[0];
            AsyncResult.forMessage(message, null, exception);
            message.sendToTarget();
            return null;
        }).when(mMockPointingAppController).stopSatelliteTransmissionUpdates(any(Message.class),
                any());
    }

    private boolean waitForRequestIsSatelliteSupportedResult(int expectedNumberOfEvents) {
        for (int i = 0; i < expectedNumberOfEvents; i++) {
            try {
                if (!mSatelliteSupportSemaphore.tryAcquire(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    loge("Timeout to receive requestIsSatelliteSupported() callback");
                    return false;
                }
            } catch (Exception ex) {
                loge("waitForRequestIsSatelliteSupportedResult: Got exception=" + ex);
                return false;
            }
        }
        return true;
    }

    private boolean waitForRequestIsSatelliteAllowedForCurrentLocationResult(
            int expectedNumberOfEvents) {
        for (int i = 0; i < expectedNumberOfEvents; i++) {
            try {
                if (!mSatelliteAllowedSemaphore.tryAcquire(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    loge("Timeout to receive "
                            + "requestIsSatelliteCommunicationAllowedForCurrentLocation()"
                            + " callback");
                    return false;
                }
            } catch (Exception ex) {
                loge("waitForRequestIsSatelliteSupportedResult: Got exception=" + ex);
                return false;
            }
        }
        return true;
    }

    private boolean waitForRequestTimeForNextSatelliteVisibilityResult(
            int expectedNumberOfEvents) {
        for (int i = 0; i < expectedNumberOfEvents; i++) {
            try {
                if (!mSatelliteVisibilityTimeSemaphore.tryAcquire(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    loge("Timeout to receive "
                            + "requestTimeForNextSatelliteVisibility() callback");
                    return false;
                }
            } catch (Exception ex) {
                loge("waitForRequestTimeForNextSatelliteVisibilityResult: Got exception=" + ex);
                return false;
            }
        }
        return true;
    }

    private boolean waitForRequestIsSatelliteEnabledResult(int expectedNumberOfEvents) {
        for (int i = 0; i < expectedNumberOfEvents; i++) {
            try {
                if (!mIsSatelliteEnabledSemaphore.tryAcquire(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    loge("Timeout to receive requestIsSatelliteEnabled() callback");
                    return false;
                }
            } catch (Exception ex) {
                loge("waitForRequestIsSatelliteEnabledResult: Got exception=" + ex);
                return false;
            }
        }
        return true;
    }

    private boolean waitForRequestIsSatelliteProvisionedResult(int expectedNumberOfEvents) {
        for (int i = 0; i < expectedNumberOfEvents; i++) {
            try {
                if (!mIsSatelliteProvisionedSemaphore.tryAcquire(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    loge("Timeout to receive requestIsSatelliteProvisioned() callback");
                    return false;
                }
            } catch (Exception ex) {
                loge("waitForRequestIsSatelliteProvisionedResult: Got exception=" + ex);
                return false;
            }
        }
        return true;
    }

    private boolean waitForRequestSatelliteCapabilitiesResult(int expectedNumberOfEvents) {
        for (int i = 0; i < expectedNumberOfEvents; i++) {
            try {
                if (!mSatelliteCapabilitiesSemaphore.tryAcquire(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    loge("Timeout to receive requestSatelliteCapabilities() callback");
                    return false;
                }
            } catch (Exception ex) {
                loge("waitForRequestSatelliteCapabilitiesResult: Got exception=" + ex);
                return false;
            }
        }
        return true;
    }

    private boolean waitForRequestIsDemoModeEnabledResult(int expectedNumberOfEvents) {
        for (int i = 0; i < expectedNumberOfEvents; i++) {
            try {
                if (!mIsDemoModeEnabledSemaphore.tryAcquire(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    loge("Timeout to receive requestIsDemoModeEnabled() callback");
                    return false;
                }
            } catch (Exception ex) {
                loge("waitForRequestIsDemoModeEnabled: Got exception=" + ex);
                return false;
            }
        }
        return true;
    }

    private boolean waitForIIntegerConsumerResult(int expectedNumberOfEvents) {
        for (int i = 0; i < expectedNumberOfEvents; i++) {
            try {
                if (!mIIntegerConsumerSemaphore.tryAcquire(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    loge("Timeout to receive IIntegerConsumer() callback");
                    return false;
                }
            } catch (Exception ex) {
                loge("waitForIIntegerConsumerResult: Got exception=" + ex);
                return false;
            }
        }
        return true;
    }

    private void verifySatelliteSupported(boolean supported, int expectedErrorCode) {
        mSatelliteSupportSemaphore.drainPermits();
        mSatelliteControllerUT.requestIsSatelliteSupported(SUB_ID, mSatelliteSupportReceiver);
        processAllMessages();
        assertTrue(waitForRequestIsSatelliteSupportedResult(1));
        assertEquals(expectedErrorCode, mQueriedSatelliteSupportedResultCode);
        assertEquals(supported, mQueriedSatelliteSupported);
    }

    private void verifySatelliteEnabled(boolean enabled, int expectedErrorCode) {
        mIsSatelliteEnabledSemaphore.drainPermits();
        mSatelliteControllerUT.requestIsSatelliteEnabled(SUB_ID, mIsSatelliteEnabledReceiver);
        processAllMessages();
        assertTrue(waitForRequestIsSatelliteEnabledResult(1));
        assertEquals(expectedErrorCode, mQueriedIsSatelliteEnabledResultCode);
        assertEquals(enabled, mQueriedIsSatelliteEnabled);
    }

    private void verifySatelliteProvisioned(boolean provisioned, int expectedErrorCode) {
        mIsSatelliteProvisionedSemaphore.drainPermits();
        mSatelliteControllerUT.requestIsSatelliteProvisioned(
                SUB_ID, mIsSatelliteProvisionedReceiver);
        processAllMessages();
        assertTrue(waitForRequestIsSatelliteProvisionedResult(1));
        assertEquals(expectedErrorCode, mQueriedIsSatelliteProvisionedResultCode);
        assertEquals(provisioned, mQueriedIsSatelliteProvisioned);
    }

    private void sendProvisionedStateChangedEvent(boolean provisioned, Throwable exception) {
        Message msg = mSatelliteControllerUT.obtainMessage(
                26 /* EVENT_SATELLITE_PROVISION_STATE_CHANGED */);
        msg.obj = new AsyncResult(null, provisioned, exception);
        msg.sendToTarget();
    }

    private void sendSatelliteModemStateChangedEvent(int state, Throwable exception) {
        Message msg = mSatelliteControllerUT.obtainMessage(
                28 /* EVENT_SATELLITE_MODEM_STATE_CHANGED */);
        msg.obj = new AsyncResult(null, state, exception);
        msg.sendToTarget();
    }

    private void setRadioPower(boolean on) {
        mSimulatedCommands.setRadioPower(on, false, false, null);
    }

    private static void loge(String message) {
        Rlog.e(TAG, message);
    }

    private static class TestSharedPreferences
            implements SharedPreferences, SharedPreferences.Editor {
        private HashMap<String, Object> mValues = new HashMap<String, Object>();

        public int getValueCount() {
            return mValues.size();
        }

        @Override
        public Editor edit() {
            return this;
        }

        @Override
        public boolean contains(String key) {
            return mValues.containsKey(key);
        }

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<String, Object>(mValues);
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            if (mValues.containsKey(key)) {
                return ((Boolean) mValues.get(key)).booleanValue();
            }
            return defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            if (mValues.containsKey(key)) {
                return ((Float) mValues.get(key)).floatValue();
            }
            return defValue;
        }

        @Override
        public int getInt(String key, int defValue) {
            if (mValues.containsKey(key)) {
                return ((Integer) mValues.get(key)).intValue();
            }
            return defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            if (mValues.containsKey(key)) {
                return ((Long) mValues.get(key)).longValue();
            }
            return defValue;
        }

        @Override
        public String getString(String key, String defValue) {
            if (mValues.containsKey(key)) return (String) mValues.get(key);
            else return defValue;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            if (mValues.containsKey(key)) {
                return (Set<String>) mValues.get(key);
            }
            return defValues;
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            mValues.put(key, Boolean.valueOf(value));
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            mValues.put(key, value);
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            mValues.put(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            mValues.put(key, value);
            return this;
        }

        @Override
        public Editor putString(String key, String value) {
            mValues.put(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            mValues.put(key, values);
            return this;
        }

        @Override
        public Editor remove(String key) {
            mValues.remove(key);
            return this;
        }

        @Override
        public Editor clear() {
            mValues.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return true;
        }

        @Override
        public void apply() {
            commit();
        }
    }

    private static class TestSatelliteController extends SatelliteController {
        public boolean setSettingsKeyForSatelliteModeCalled = false;
        public boolean allRadiosDisabled = true;
        public int satelliteModeSettingValue = SATELLITE_MODE_ENABLED_FALSE;

        TestSatelliteController(Context context, Looper looper) {
            super(context, looper);
            logd("Constructing TestSatelliteController");
        }

        @Override
        protected void initializeSatelliteModeRadios() {
            logd("initializeSatelliteModeRadios");
        }

        @Override
        protected void setSettingsKeyForSatelliteMode(int val) {
            logd("setSettingsKeyForSatelliteMode: val=" + val);
            satelliteModeSettingValue = val;
            setSettingsKeyForSatelliteModeCalled = true;
        }

        @Override
        protected boolean areAllRadiosDisabled() {
            return allRadiosDisabled;
        }
    }
}
