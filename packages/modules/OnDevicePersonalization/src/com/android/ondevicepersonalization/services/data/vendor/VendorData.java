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

package com.android.ondevicepersonalization.services.data.vendor;

import android.annotation.NonNull;

import com.android.ondevicepersonalization.internal.util.AnnotationValidations;
import com.android.ondevicepersonalization.internal.util.DataClass;

/**
 * VendorData object for the VendorData table
 */
@DataClass(
        genBuilder = true,
        genEqualsHashCode = true
)
public class VendorData {
    /** Lookup key for the row - unique for each vendor */
    @NonNull
    private final String mKey;

    /** Row data - ads or other vendor settings */
    @NonNull
    private final byte[] mData;



    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/packages/modules/OnDevicePersonalization/src/com/android/ondevicepersonalization/services/data/vendor/VendorData.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    @DataClass.Generated.Member
    /* package-private */ VendorData(
            @NonNull String key,
            @NonNull byte[] data) {
        this.mKey = key;
        AnnotationValidations.validate(
                NonNull.class, null, mKey);
        this.mData = data;
        AnnotationValidations.validate(
                NonNull.class, null, mData);

        // onConstructed(); // You can define this method to get a callback
    }

    /**
     * Lookup key for the row - unique for each vendor
     */
    @DataClass.Generated.Member
    public @NonNull String getKey() {
        return mKey;
    }

    /**
     * Row data - ads or other vendor settings
     */
    @DataClass.Generated.Member
    public @NonNull byte[] getData() {
        return mData;
    }

    @Override
    @DataClass.Generated.Member
    public boolean equals(@android.annotation.Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(VendorData other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        VendorData that = (VendorData) o;
        //noinspection PointlessBooleanExpression
        return true
                && java.util.Objects.equals(mKey, that.mKey)
                && java.util.Arrays.equals(mData, that.mData);
    }

    @Override
    @DataClass.Generated.Member
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + java.util.Objects.hashCode(mKey);
        _hash = 31 * _hash + java.util.Arrays.hashCode(mData);
        return _hash;
    }

    /**
     * A builder for {@link VendorData}
     */
    @SuppressWarnings("WeakerAccess")
    @DataClass.Generated.Member
    public static class Builder {

        private @NonNull String mKey;
        private @NonNull byte[] mData;

        private long mBuilderFieldsSet = 0L;

        public Builder() {
        }

        /**
         * Creates a new Builder.
         *
         * @param key
         *   Lookup key for the row - unique for each vendor
         * @param data
         *   Row data - ads or other vendor settings
         */
        public Builder(
                @NonNull String key,
                @NonNull byte[] data) {
            mKey = key;
            AnnotationValidations.validate(
                    NonNull.class, null, mKey);
            mData = data;
            AnnotationValidations.validate(
                    NonNull.class, null, mData);
        }

        /**
         * Lookup key for the row - unique for each vendor
         */
        @DataClass.Generated.Member
        public @NonNull Builder setKey(@NonNull String value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x1;
            mKey = value;
            return this;
        }

        /**
         * Row data - ads or other vendor settings
         */
        @DataClass.Generated.Member
        public @NonNull Builder setData(@NonNull byte... value) {
            checkNotUsed();
            mBuilderFieldsSet |= 0x2;
            mData = value;
            return this;
        }

        /** Builds the instance. This builder should not be touched after calling this! */
        public @NonNull VendorData build() {
            checkNotUsed();
            mBuilderFieldsSet |= 0x4; // Mark builder used

            VendorData o = new VendorData(
                    mKey,
                    mData);
            return o;
        }

        private void checkNotUsed() {
            if ((mBuilderFieldsSet & 0x4) != 0) {
                throw new IllegalStateException(
                        "This Builder should not be reused. Use a new Builder instance instead");
            }
        }
    }

    @DataClass.Generated(
            time = 1680017865553L,
            codegenVersion = "1.0.23",
            sourceFile = "packages/modules/OnDevicePersonalization/src/com/android/ondevicepersonalization/services/data/vendor/VendorData.java",
            inputSignatures = "private final @android.annotation.NonNull java.lang.String mKey\nprivate final @android.annotation.NonNull byte[] mData\nclass VendorData extends java.lang.Object implements []\n@com.android.ondevicepersonalization.internal.util.DataClass(genBuilder=true, genEqualsHashCode=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}