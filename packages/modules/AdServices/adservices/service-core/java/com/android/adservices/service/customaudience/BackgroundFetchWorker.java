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

package com.android.adservices.service.customaudience;

import android.annotation.NonNull;
import android.content.Context;

import com.android.adservices.LoggerFactory;
import com.android.adservices.concurrency.AdServicesExecutors;
import com.android.adservices.data.adselection.AppInstallDao;
import com.android.adservices.data.adselection.SharedStorageDatabase;
import com.android.adservices.data.customaudience.CustomAudienceDao;
import com.android.adservices.data.customaudience.CustomAudienceDatabase;
import com.android.adservices.data.customaudience.DBCustomAudienceBackgroundFetchData;
import com.android.adservices.data.enrollment.EnrollmentDao;
import com.android.adservices.service.Flags;
import com.android.adservices.service.FlagsFactory;
import com.android.adservices.service.common.SingletonRunner;
import com.android.internal.annotations.VisibleForTesting;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ExecutionSequencer;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Worker instance for updating custom audiences in the background. */
public class BackgroundFetchWorker {
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getFledgeLogger();
    public static final String JOB_DESCRIPTION = "FLEDGE background fetch";
    private static final Object SINGLETON_LOCK = new Object();
    private static volatile BackgroundFetchWorker sBackgroundFetchWorker;

    private final CustomAudienceDao mCustomAudienceDao;
    private final Flags mFlags;
    private final BackgroundFetchRunner mBackgroundFetchRunner;
    private final Clock mClock;
    private final SingletonRunner<Void> mSingletonRunner =
            new SingletonRunner<>(JOB_DESCRIPTION, this::doRun);

    @VisibleForTesting
    protected BackgroundFetchWorker(
            @NonNull CustomAudienceDao customAudienceDao,
            @NonNull Flags flags,
            @NonNull BackgroundFetchRunner backgroundFetchRunner,
            @NonNull Clock clock) {
        Objects.requireNonNull(customAudienceDao);
        Objects.requireNonNull(flags);
        Objects.requireNonNull(backgroundFetchRunner);
        Objects.requireNonNull(clock);
        mCustomAudienceDao = customAudienceDao;
        mFlags = flags;
        mBackgroundFetchRunner = backgroundFetchRunner;
        mClock = clock;
    }

    /**
     * Gets an instance of a {@link BackgroundFetchWorker}.
     *
     * <p>If an instance hasn't been initialized, a new singleton will be created and returned.
     */
    @NonNull
    public static BackgroundFetchWorker getInstance(@NonNull Context context) {
        Objects.requireNonNull(context);

        if (sBackgroundFetchWorker == null) {
            synchronized (SINGLETON_LOCK) {
                if (sBackgroundFetchWorker == null) {
                    CustomAudienceDao customAudienceDao =
                            CustomAudienceDatabase.getInstance(context).customAudienceDao();
                    AppInstallDao appInstallDao =
                            SharedStorageDatabase.getInstance(context).appInstallDao();
                    Flags flags = FlagsFactory.getFlags();
                    sBackgroundFetchWorker =
                            new BackgroundFetchWorker(
                                    customAudienceDao,
                                    flags,
                                    new BackgroundFetchRunner(
                                            customAudienceDao,
                                            appInstallDao,
                                            context.getPackageManager(),
                                            EnrollmentDao.getInstance(context),
                                            flags),
                                    Clock.systemUTC());
                }
            }
        }

        return sBackgroundFetchWorker;
    }

    /**
     * Runs the background fetch job for FLEDGE, including garbage collection and updating custom
     * audiences.
     *
     * @return A future to be used to check when the task has completed.
     */
    public FluentFuture<Void> runBackgroundFetch() {
        sLogger.d("Starting %s", JOB_DESCRIPTION);
        return mSingletonRunner.runSingleInstance();
    }

    /** Requests that any ongoing work be stopped gracefully and waits for work to be stopped. */
    public void stopWork() {
        mSingletonRunner.stopWork();
    }

    private FluentFuture<Void> doRun(@NonNull Supplier<Boolean> shouldStop) {
        Instant jobStartTime = mClock.instant();
        return cleanupFledgeData(jobStartTime)
                .transform(
                        ignored -> getFetchDataList(shouldStop, jobStartTime),
                        AdServicesExecutors.getBackgroundExecutor())
                .transformAsync(
                        fetchDataList -> updateData(fetchDataList, shouldStop, jobStartTime),
                        AdServicesExecutors.getBackgroundExecutor())
                .withTimeout(
                        mFlags.getFledgeBackgroundFetchJobMaxRuntimeMs(),
                        TimeUnit.MILLISECONDS,
                        AdServicesExecutors.getScheduler());
    }

    private ListenableFuture<Void> updateData(
            @NonNull List<DBCustomAudienceBackgroundFetchData> fetchDataList,
            @NonNull Supplier<Boolean> shouldStop,
            @NonNull Instant jobStartTime) {
        if (fetchDataList.isEmpty()) {
            sLogger.d("No custom audiences found to update");
            return FluentFuture.from(Futures.immediateVoidFuture());
        }

        sLogger.d("Updating %d custom audiences", fetchDataList.size());
        // Divide the gathered CAs among worker threads
        int numWorkers =
                Math.min(
                        Math.max(1, Runtime.getRuntime().availableProcessors() - 2),
                        mFlags.getFledgeBackgroundFetchThreadPoolSize());
        int numCustomAudiencesPerWorker =
                (fetchDataList.size() / numWorkers)
                        + (((fetchDataList.size() % numWorkers) == 0) ? 0 : 1);

        List<ListenableFuture<?>> subListFutureUpdates = new ArrayList<>();
        for (final List<DBCustomAudienceBackgroundFetchData> fetchDataSubList :
                Lists.partition(fetchDataList, numCustomAudiencesPerWorker)) {
            if (shouldStop.get()) {
                break;
            }
            // Updates in each batch are sequenced
            ExecutionSequencer sequencer = ExecutionSequencer.create();
            for (DBCustomAudienceBackgroundFetchData fetchData : fetchDataSubList) {
                subListFutureUpdates.add(
                        sequencer.submitAsync(
                                () ->
                                        mBackgroundFetchRunner.updateCustomAudience(
                                                jobStartTime, fetchData),
                                AdServicesExecutors.getBackgroundExecutor()));
            }
        }

        return FluentFuture.from(Futures.allAsList(subListFutureUpdates))
                .transform(ignored -> null, AdServicesExecutors.getLightWeightExecutor());
    }

    private List<DBCustomAudienceBackgroundFetchData> getFetchDataList(
            @NonNull Supplier<Boolean> shouldStop, @NonNull Instant jobStartTime) {
        if (shouldStop.get()) {
            sLogger.d("Stopping " + JOB_DESCRIPTION);
            return ImmutableList.of();
        }

        // Fetch stale/eligible/delinquent custom audiences
        return mCustomAudienceDao.getActiveEligibleCustomAudienceBackgroundFetchData(
                jobStartTime, mFlags.getFledgeBackgroundFetchMaxNumUpdated());
    }

    private FluentFuture<?> cleanupFledgeData(Instant jobStartTime) {
        return FluentFuture.from(
                AdServicesExecutors.getBackgroundExecutor()
                        .submit(
                                () -> {
                                    // Clean up custom audiences first so the actual fetch won't do
                                    // unnecessary work
                                    mBackgroundFetchRunner.deleteExpiredCustomAudiences(
                                            jobStartTime);
                                    mBackgroundFetchRunner.deleteDisallowedOwnerCustomAudiences();
                                    mBackgroundFetchRunner.deleteDisallowedBuyerCustomAudiences();
                                    if (mFlags.getFledgeAdSelectionFilteringEnabled()) {
                                        mBackgroundFetchRunner
                                                .deleteDisallowedPackageAppInstallEntries();
                                    }
                                }));
    }
}
