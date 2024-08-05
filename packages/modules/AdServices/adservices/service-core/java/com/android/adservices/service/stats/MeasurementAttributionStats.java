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

package com.android.adservices.service.stats;

import android.annotation.NonNull;

import java.util.Objects;

/** class for measurement attribution stats. */
public class MeasurementAttributionStats {
    private int mCode;
    private int mSourceType;
    private int mSurfaceType;
    private int mResult;
    private int mFailureType;
    private boolean mIsSourceDerived;
    private boolean mIsInstallAttribution;
    private long mAttributionDelay;

    public MeasurementAttributionStats() {}

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MeasurementAttributionStats)) {
            return false;
        }
        MeasurementAttributionStats measurementAttributionStats = (MeasurementAttributionStats) obj;
        return mCode == measurementAttributionStats.getCode()
                && mSourceType == measurementAttributionStats.getSourceType()
                && mSurfaceType == measurementAttributionStats.getSurfaceType()
                && mResult == measurementAttributionStats.getResult()
                && mFailureType == measurementAttributionStats.getFailureType()
                && mIsSourceDerived == measurementAttributionStats.isSourceDerived()
                && mIsInstallAttribution == measurementAttributionStats.isInstallAttribution()
                && mAttributionDelay == measurementAttributionStats.getAttributionDelay();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mCode,
                mSourceType,
                mSurfaceType,
                mResult,
                mFailureType,
                mIsSourceDerived,
                mIsInstallAttribution,
                mAttributionDelay);
    }

    public int getCode() {
        return mCode;
    }

    public int getSourceType() {
        return mSourceType;
    }

    public int getSurfaceType() {
        return mSurfaceType;
    }

    public int getResult() {
        return mResult;
    }

    public int getFailureType() {
        return mFailureType;
    }

    public boolean isSourceDerived() {
        return mIsSourceDerived;
    }

    public boolean isInstallAttribution() {
        return mIsInstallAttribution;
    }

    public long getAttributionDelay() {
        return mAttributionDelay;
    }

    /** Builder for {@link MeasurementAttributionStats}. */
    public static final class Builder {
        private final MeasurementAttributionStats mBuilding;

        public Builder() {
            mBuilding = new MeasurementAttributionStats();
        }

        /** See {@link MeasurementAttributionStats#getCode()} . */
        public @NonNull MeasurementAttributionStats.Builder setCode(int code) {
            mBuilding.mCode = code;
            return this;
        }

        /** See {@link MeasurementAttributionStats#getSourceType()} . */
        public @NonNull MeasurementAttributionStats.Builder setSourceType(int sourceType) {
            mBuilding.mSourceType = sourceType;
            return this;
        }

        /** See {@link MeasurementAttributionStats#getSurfaceType()} . */
        public @NonNull MeasurementAttributionStats.Builder setSurfaceType(int surfaceType) {
            mBuilding.mSurfaceType = surfaceType;
            return this;
        }

        /** See {@link MeasurementAttributionStats#getResult()} . */
        public @NonNull MeasurementAttributionStats.Builder setResult(int result) {
            mBuilding.mResult = result;
            return this;
        }

        /** See {@link MeasurementAttributionStats#getFailureType()} . */
        public @NonNull MeasurementAttributionStats.Builder setFailureType(int failureType) {
            mBuilding.mFailureType = failureType;
            return this;
        }

        /** See {@link MeasurementAttributionStats#isSourceDerived()} . */
        public @NonNull MeasurementAttributionStats.Builder setSourceDerived(
                boolean isSourceDerived) {
            mBuilding.mIsSourceDerived = isSourceDerived;
            return this;
        }

        /** See {@link MeasurementAttributionStats#isSourceDerived()} . */
        public @NonNull MeasurementAttributionStats.Builder setInstallAttribution(
                boolean isInstallAttribution) {
            mBuilding.mIsInstallAttribution = isInstallAttribution;
            return this;
        }

        /** See {@link MeasurementAttributionStats#getAttributionDelay()} . */
        public @NonNull MeasurementAttributionStats.Builder setAttributionDelay(
                long attributionDelay) {
            mBuilding.mAttributionDelay = attributionDelay;
            return this;
        }
        /** Build the {@link MeasurementAttributionStats}. */
        public @NonNull MeasurementAttributionStats build() {
            return mBuilding;
        }
    }
}
