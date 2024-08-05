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

package com.android.adservices.service.adselection;

import static com.android.adservices.service.common.Throttler.ApiKey.FLEDGE_API_UPDATE_AD_COUNTER_HISTOGRAM;
import static com.android.adservices.service.stats.AdServicesStatsLog.AD_SERVICES_API_CALLED__API_NAME__UPDATE_AD_COUNTER_HISTOGRAM;

import android.adservices.adselection.UpdateAdCounterHistogramCallback;
import android.adservices.adselection.UpdateAdCounterHistogramInput;
import android.adservices.common.AdServicesStatusUtils;
import android.adservices.common.FledgeErrorResponse;
import android.annotation.NonNull;
import android.os.Build;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

import com.android.adservices.LoggerFactory;
import com.android.adservices.service.Flags;
import com.android.adservices.service.common.AdSelectionServiceFilter;
import com.android.adservices.service.consent.ConsentManager;
import com.android.adservices.service.exception.FilterException;
import com.android.adservices.service.stats.AdServicesLogger;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Worker for validating input to and evaluating callback functions for the {@link
 * AdSelectionServiceImpl#updateAdCounterHistogram(UpdateAdCounterHistogramInput,
 * UpdateAdCounterHistogramCallback)} API.
 */
@RequiresApi(Build.VERSION_CODES.S)
public class UpdateAdCounterHistogramWorker {
    private static final int LOGGING_API_NAME =
            AD_SERVICES_API_CALLED__API_NAME__UPDATE_AD_COUNTER_HISTOGRAM;
    @NonNull private static final LoggerFactory.Logger sLogger = LoggerFactory.getFledgeLogger();
    @NonNull private final AdCounterHistogramUpdater mAdCounterHistogramUpdater;
    @NonNull private final ListeningExecutorService mExecutorService;
    @NonNull private final Clock mClock;
    @NonNull private final AdServicesLogger mAdServicesLogger;
    @NonNull private final Flags mFlags;
    @NonNull private final AdSelectionServiceFilter mAdSelectionServiceFilter;
    @NonNull private final ConsentManager mConsentManager;
    private final int mCallerUid;

    public UpdateAdCounterHistogramWorker(
            @NonNull AdCounterHistogramUpdater adCounterHistogramUpdater,
            @NonNull ExecutorService executor,
            @NonNull Clock clock,
            @NonNull AdServicesLogger adServicesLogger,
            @NonNull Flags flags,
            @NonNull AdSelectionServiceFilter adSelectionServiceFilter,
            @NonNull ConsentManager consentManager,
            int callerUid) {
        Objects.requireNonNull(adCounterHistogramUpdater);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(clock);
        Objects.requireNonNull(adServicesLogger);
        Objects.requireNonNull(flags);
        Objects.requireNonNull(adSelectionServiceFilter);
        Objects.requireNonNull(consentManager);

        mAdCounterHistogramUpdater = adCounterHistogramUpdater;
        mExecutorService = MoreExecutors.listeningDecorator(executor);
        mClock = clock;
        mAdServicesLogger = adServicesLogger;
        mFlags = flags;
        mAdSelectionServiceFilter = adSelectionServiceFilter;
        mConsentManager = consentManager;
        mCallerUid = callerUid;
    }

    /**
     * Executes the service implementation for the {@link
     * AdSelectionServiceImpl#updateAdCounterHistogram(UpdateAdCounterHistogramInput,
     * UpdateAdCounterHistogramCallback)} API.
     */
    public void updateAdCounterHistogram(
            @NonNull UpdateAdCounterHistogramInput inputParams,
            @NonNull UpdateAdCounterHistogramCallback callback) {
        Objects.requireNonNull(inputParams);
        Objects.requireNonNull(callback);

        sLogger.v("Evaluating updateAdCounterHistogram request");
        final Instant currentTimestamp = mClock.instant();

        FluentFuture.from(mExecutorService.submit(() -> validateRequest(inputParams)))
                .transformAsync(
                        unusedResult ->
                                submitUpdateAdCounterHistogram(inputParams, currentTimestamp),
                        mExecutorService)
                .addCallback(
                        new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                sLogger.v("Completed updateAdCounterHistogram execution");
                                mAdServicesLogger.logFledgeApiCallStats(
                                        LOGGING_API_NAME, AdServicesStatusUtils.STATUS_SUCCESS, 0);
                                notifySuccess(callback);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                sLogger.d(
                                        t,
                                        "Error encountered in updateAdCounterHistogram execution");
                                invokeFailure(callback, t);
                            }
                        },
                        mExecutorService);
    }

    private ListenableFuture<Void> submitUpdateAdCounterHistogram(
            @NonNull UpdateAdCounterHistogramInput inputParams, @NonNull Instant currentTimestamp) {
        return mExecutorService.submit(
                () -> {
                    sLogger.v("Updating ad counter histogram with timestamp %s", currentTimestamp);
                    mAdCounterHistogramUpdater.updateNonWinHistogram(
                            inputParams.getAdSelectionId(),
                            inputParams.getCallerPackageName(),
                            inputParams.getAdEventType(),
                            currentTimestamp);
                    return null;
                });
    }

    private Void validateRequest(@NonNull UpdateAdCounterHistogramInput inputParams) {
        sLogger.v("Validating updateAdCounterHistogram request");

        if (!mFlags.getFledgeAdSelectionFilteringEnabled()) {
            sLogger.v("Ad selection filtering disabled");
            throw new IllegalStateException();
        }

        mAdSelectionServiceFilter.filterRequest(
                inputParams.getCallerAdTech(),
                inputParams.getCallerPackageName(),
                true,
                false,
                mCallerUid,
                LOGGING_API_NAME,
                FLEDGE_API_UPDATE_AD_COUNTER_HISTOGRAM);

        // TODO(b/271147154): Merge into the filterRequest call once all filters update
        //  FLEDGE per-app consent
        if (mConsentManager.isFledgeConsentRevokedForAppAfterSettingFledgeUse(
                inputParams.getCallerPackageName())) {
            sLogger.v(
                    "Consent revoked for caller package name %s",
                    inputParams.getCallerPackageName());
            throw new ConsentManager.RevokedConsentException();
        }

        return null;
    }

    private void notifySuccess(@NonNull UpdateAdCounterHistogramCallback callback) {
        try {
            callback.onSuccess();
        } catch (RemoteException e) {
            sLogger.e(e, "Unable to send successful result to the callback");
            mAdServicesLogger.logFledgeApiCallStats(
                    LOGGING_API_NAME, AdServicesStatusUtils.STATUS_UNKNOWN_ERROR, 0);
            throw new RuntimeException(e);
        }
    }

    private void invokeFailure(
            @NonNull UpdateAdCounterHistogramCallback callback, @NonNull Throwable t) {
        // TODO(b/271147154): Modify to check for FilterException cause once consent is
        //  incorporated into the filter
        if (t instanceof ConsentManager.RevokedConsentException) {
            mAdServicesLogger.logFledgeApiCallStats(
                    LOGGING_API_NAME, AdServicesStatusUtils.STATUS_USER_CONSENT_REVOKED, 0);
            notifySuccess(callback);
            return;
        }

        int resultCode;
        boolean isFilterException = t instanceof FilterException;

        if (isFilterException) {
            resultCode = FilterException.getResultCode(t);
        } else if (t instanceof IllegalArgumentException || t instanceof NullPointerException) {
            resultCode = AdServicesStatusUtils.STATUS_INVALID_ARGUMENT;
        } else {
            resultCode = AdServicesStatusUtils.STATUS_INTERNAL_ERROR;
        }

        // Skip logging if a FilterException occurs; the AdSelectionServiceFilter internally
        // ensures that the failing assertion is logged
        if (!isFilterException) {
            mAdServicesLogger.logFledgeApiCallStats(LOGGING_API_NAME, resultCode, 0);
        }

        notifyFailure(callback, resultCode, t.getMessage());
    }

    private void notifyFailure(
            @NonNull UpdateAdCounterHistogramCallback callback,
            int statusCode,
            @NonNull String errorMessage) {
        try {
            callback.onFailure(
                    new FledgeErrorResponse.Builder()
                            .setStatusCode(statusCode)
                            .setErrorMessage(errorMessage)
                            .build());
        } catch (RemoteException e) {
            sLogger.e(e, "Unable to send failed result to the callback");
            mAdServicesLogger.logFledgeApiCallStats(
                    LOGGING_API_NAME, AdServicesStatusUtils.STATUS_UNKNOWN_ERROR, 0);
            throw new RuntimeException(e);
        }
    }
}
