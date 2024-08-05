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

package com.android.adservices.data.measurement;


import com.android.adservices.LogUtil;
import com.android.internal.annotations.VisibleForTesting;

import java.util.Optional;

/**
 * Abstract class for Datastore management.
 */
public abstract class DatastoreManager {

    /**
     * Consumer interface for Dao operations.
     */
    @FunctionalInterface
    public interface ThrowingCheckedConsumer {
        /**
         * Performs the operation on {@link IMeasurementDao}.
         */
        void accept(IMeasurementDao measurementDao) throws DatastoreException;
    }

    /**
     * Function interface for Dao operations that returns {@link Output}.
     *
     * @param <Output> output type
     */
    @FunctionalInterface
    public interface ThrowingCheckedFunction<Output> {
        /**
         * Performs the operation on Dao.
         *
         * @return Output result of the operation
         */
        Output apply(IMeasurementDao measurementDao) throws DatastoreException;
    }

    /**
     * Creates a new transaction object for use in Dao.
     *
     * @return transaction
     */
    protected abstract ITransaction createNewTransaction();

    /**
     * Acquire an instance of Dao object for querying the datastore.
     *
     * @return Dao object.
     */
    @VisibleForTesting
    public abstract IMeasurementDao getMeasurementDao();

    /**
     * Runs the {@code execute} lambda in a transaction.
     *
     * @param execute lambda to be executed in a transaction
     * @param <T>     the class for result
     * @return Optional<T>, empty in case of an error, output otherwise
     */
    public final <T> Optional<T> runInTransactionWithResult(ThrowingCheckedFunction<T> execute) {
        IMeasurementDao measurementDao = getMeasurementDao();
        ITransaction transaction = createNewTransaction();
        if (transaction == null) {
            return Optional.empty();
        }
        measurementDao.setTransaction(transaction);
        transaction.begin();

        Optional<T> result;
        try {
            result = Optional.ofNullable(execute.apply(measurementDao));
        } catch (DatastoreException ex) {
            result = Optional.empty();
            safePrintDataStoreVersion();
            LogUtil.e(ex, "DatastoreException thrown during transaction");
            transaction.rollback();
        } catch (Exception ex) {
            // Catch all exceptions for rollback
            safePrintDataStoreVersion();
            LogUtil.e(ex, "Unhandled exception thrown during transaction");
            transaction.rollback();
            throw ex;
        } finally {
            transaction.end();
        }

        return result;
    }

    /**
     * Runs the {@code execute} lambda in a transaction.
     *
     * @param execute lambda to be executed in transaction
     * @return success true if execution succeeded, false otherwise
     */
    public final boolean runInTransaction(ThrowingCheckedConsumer execute) {
        return runInTransactionWithResult((measurementDao) -> {
            execute.accept(measurementDao);
            return true;
        }).orElse(false);
    }

    /** Prints the underlying data store version catching exceptions it can raise. */
    private void safePrintDataStoreVersion() {
        try {
            LogUtil.w("Underlying datastore version: " + getDataStoreVersion());
        } catch (Exception e) {
            // If fetching data store version throws an exception, skip printing the DB version.
            LogUtil.e(e, "Failed to print data store version.");
        }
    }

    /** Returns the version the underlying data store is at. E.g. user version of the DB. */
    protected abstract int getDataStoreVersion();
}
