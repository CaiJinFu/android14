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

package com.android.adservices.service.appsearch;

import android.annotation.NonNull;
import android.app.adservices.AdServicesManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.platformstorage.PlatformStorage;

import com.android.adservices.LogUtil;
import com.android.adservices.concurrency.AdServicesExecutors;
import com.android.adservices.service.consent.ConsentConstants;
import com.android.internal.annotations.VisibleForTesting;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class provides an interface to read/write measurement rollback data to AppSearch. This is
 * read when the measurement API starts up, and is used to determine whether a rollback happened and
 * the measurement db needs to be cleared.
 */
// TODO(b/269798827): Enable for R.
@RequiresApi(Build.VERSION_CODES.S)
class AppSearchMeasurementRollbackWorker {
    private static final String DATABASE_NAME = "measurement_rollback";

    // At the worker level, we ensure that writes do not conflict with any other writes/reads.
    private static final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();

    // Timeout for AppSearch write query in milliseconds.
    private static final int TIMEOUT_MS = 2000;

    private final String mUserId;
    private final ListenableFuture<AppSearchSession> mSearchSession;
    private final Executor mExecutor = AdServicesExecutors.getBackgroundExecutor();

    private AppSearchMeasurementRollbackWorker(@NonNull Context context, @NonNull String userId) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(userId);

        mUserId = userId;
        mSearchSession =
                PlatformStorage.createSearchSessionAsync(
                        new PlatformStorage.SearchContext.Builder(context, DATABASE_NAME).build());
    }

    static AppSearchMeasurementRollbackWorker getInstance(
            @NonNull Context context, @NonNull String userId) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(userId);
        return new AppSearchMeasurementRollbackWorker(context, userId);
    }

    void recordAdServicesDeletionOccurred(
            @AdServicesManager.DeletionApiType int deletionApiType, long currentApexVersion) {
        READ_WRITE_LOCK.writeLock().lock();
        try {
            AppSearchMeasurementRollbackDao dao =
                    createAppSearchMeasurementRollbackDao(deletionApiType, currentApexVersion);
            // Recording measurement deletion is only useful within Android S, since the package
            // name changes from T+. This causes the entire measurement DB to be inaccessible on OTA
            // to T. As a result, the written data doesn't need to be preserved across an OTA, so we
            // don't need to share it with the T package. Thus, we can send an empty list for the
            // packageIdentifiers parameter.
            dao.writeData(mSearchSession, List.of(), mExecutor)
                    .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            LogUtil.d("Wrote measurement rollback data to AppSearch: %s", dao);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LogUtil.e(e, "Failed to write measurement rollback to AppSearch");
            throw new RuntimeException(ConsentConstants.ERROR_MESSAGE_APPSEARCH_FAILURE);
        } finally {
            READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    @VisibleForTesting
    AppSearchMeasurementRollbackDao createAppSearchMeasurementRollbackDao(
            @AdServicesManager.DeletionApiType int deletionApiType, long currentApexVersion) {
        return new AppSearchMeasurementRollbackDao(
                AppSearchMeasurementRollbackDao.getRowId(mUserId, deletionApiType),
                AppSearchMeasurementRollbackDao.NAMESPACE,
                mUserId,
                currentApexVersion);
    }

    void clearAdServicesDeletionOccurred(@NonNull String rowId) {
        Objects.requireNonNull(rowId);

        READ_WRITE_LOCK.writeLock().lock();
        try {
            AppSearchDao.deleteData(
                            AppSearchMeasurementRollbackDao.class,
                            mSearchSession,
                            mExecutor,
                            rowId,
                            AppSearchMeasurementRollbackDao.NAMESPACE)
                    .get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            LogUtil.d("Deleted MeasurementRollback data from AppSearch for: %s", rowId);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            LogUtil.e(e, "Failed to delete MeasurementRollback data in AppSearch");
            throw new RuntimeException(ConsentConstants.ERROR_MESSAGE_APPSEARCH_FAILURE);
        } finally {
            READ_WRITE_LOCK.writeLock().unlock();
        }
    }

    AppSearchMeasurementRollbackDao getAdServicesDeletionRollbackMetadata(
            @AdServicesManager.DeletionApiType int deletionApiType) {
        READ_WRITE_LOCK.writeLock().lock();
        try {
            AppSearchMeasurementRollbackDao dao =
                    AppSearchMeasurementRollbackDao.readDocument(
                            mSearchSession, mExecutor, mUserId);
            LogUtil.d("Result of query for AppSearchMeasurementRollbackDao: %s", dao);
            return dao;
        } finally {
            READ_WRITE_LOCK.writeLock().unlock();
        }
    }
}
