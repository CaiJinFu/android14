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

package com.android.ondevicepersonalization.services.data.events;

import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.ondevicepersonalization.internal.util.AnnotationValidations;
import com.android.ondevicepersonalization.internal.util.DataClass;

/**
 * Query object for the Query table
 */
@DataClass(
        genBuilder = true,
        genEqualsHashCode = true
)
public class Query {
    /** The id of the query. */
    private final long mQueryId;

    /** Time of the query in milliseconds. */
    private final long mTimeMillis;

    /** Name of the package that handled the request */
    @NonNull
    private final String mServicePackageName;

    /** Blob representing the query. */
    @NonNull
    private final byte[] mQueryData;



    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/packages/modules/OnDevicePersonalization/src/com/android/ondevicepersonalization/services/data/events/Query.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    @DataClass.Generated.Member
    /* package-private */ Query(
            long queryId,
            long timeMillis,
            @NonNull String servicePackageName,
            @NonNull byte[] queryData) {
        this.mQueryId = queryId;
        this.mTimeMillis = timeMillis;
        this.mServicePackageName = servicePackageName;
        AnnotationValidations.validate(
                NonNull.class, null, mServicePackageName);
        this.mQueryData = queryData;
        AnnotationValidations.validate(
                NonNull.class, null, mQueryData);

        // onConstructed(); // You can define this method to get a callback
    }

    /**
     * The id of the query.
     */
    @DataClass.Generated.Member
    public long getQueryId() {
        return mQueryId;
    }

    /**
     * Time of the query in milliseconds.
     */
    @DataClass.Generated.Member
    public long getTimeMillis() {
        return mTimeMillis;
    }

    /**
     * Name of the package that handled the request
     */
    @DataClass.Generated.Member
    public @NonNull String getServicePackageName() {
        return mServicePackageName;
    }

    /**
     * Blob representing the query.
     */
    @DataClass.Generated.Member
    public @NonNull byte[] getQueryData() {
        return mQueryData;
    }

    @Override
    @DataClass.Generated.Member
    public boolean equals(@Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(Query other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        Query that = (Query) o;
        //noinspection PointlessBooleanExpression
        return true
                && mQueryId == that.mQueryId
                && mTimeMillis == that.mTimeMillis
                && java.util.Objects.equals(mServicePackageName, that.mServicePackageName)
                && java.util.Arrays.equals(mQueryData, that.mQueryData);
    }

    @Override
    @DataClass.Generated.Member
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + Long.hashCode(mQueryId);
        _hash = 31 * _hash + Long.hashCode(mTimeMillis);
        _hash = 31 * _hash + java.util.Objects.hashCode(mServicePackageName);
        _hash = 31 * _hash + java.util.Arrays.hashCode(mQueryData);
        return _hash;
    }

    /**
     * A builder for {@link Query}
     */
    @SuppressWarnings("WeakerAccess")
    @DataClass.Generated.Member
    public static class Builder {

        private long mQueryId;
        private long mTimeMillis;
        private @NonNull String mServicePackageName;
        private @NonNull byte[] mQueryData;

        private long mBuilderFieldsSet = 0L;

        public Builder() {
        }

        /**
         * Creates a new Builder.
         *
         * @param queryId
         *   The id of the query.
         * @param timeMillis
         *   Time of the query in milliseconds.
         * @param servicePackageName
         *   Name of the package that handled the request
         * @param queryData
         *   Blob representing the query.
         */
        public Builder(
                long queryId,
                long timeMillis,
                @NonNull String servicePackageName,
                @NonNull byte[] queryData) {
            mQueryId = queryId;
            mTimeMillis = timeMillis;
            mServicePackageName = servicePackageName;
            AnnotationValidations.validate(
                    NonNull.class, null, mServicePackageName);
            mQueryData = queryData;
            AnnotationValidations.validate(
                    NonNull.class, null, mQueryData);
        }

        /**
         * The id of the query.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setQueryId(long value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x1;
            mQueryId = value;
            return this;
        }

        /**
         * Time of the query in milliseconds.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setTimeMillis(long value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x2;
            mTimeMillis = value;
            return this;
        }

        /**
         * Name of the package that handled the request
         */
        @DataClass.Generated.Member
        public @NonNull Builder setServicePackageName(@NonNull String value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x4;
            mServicePackageName = value;
            return this;
        }

        /**
         * Blob representing the query.
         */
        @DataClass.Generated.Member
        public @NonNull Builder setQueryData(@NonNull byte... value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x8;
            mQueryData = value;
            return this;
        }

        /** Builds the instance. This builder should not be touched after calling this! */
        public @NonNull Query build() {
            checkNotUsed();
            mBuilderFieldsSet |= 0x10; // Mark builder used

            Query o = new Query(
                    mQueryId,
                    mTimeMillis,
                    mServicePackageName,
                    mQueryData);
            return o;
        }

        private void checkNotUsed() {
            if ((mBuilderFieldsSet & 0x10) != 0) {
                throw new IllegalStateException(
                        "This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @DataClass.Generated(
            time = 1680018439034L,
            codegenVersion = "1.0.23",
            sourceFile = "packages/modules/OnDevicePersonalization/src/com/android/ondevicepersonalization/services/data/events/Query.java",
            inputSignatures = "private final  long mQueryId\nprivate final  long mTimeMillis\nprivate final @android.annotation.NonNull java.lang.String mServicePackageName\nprivate final @android.annotation.NonNull byte[] mQueryData\nclass Query extends java.lang.Object implements []\n@com.android.ondevicepersonalization.internal.util.DataClass(genBuilder=true, genEqualsHashCode=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}