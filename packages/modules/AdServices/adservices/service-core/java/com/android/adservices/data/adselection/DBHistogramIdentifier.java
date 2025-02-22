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

package com.android.adservices.data.adselection;

import android.adservices.common.AdTechIdentifier;
import android.adservices.common.FrequencyCapFilters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.android.adservices.service.adselection.HistogramEvent;

import com.google.auto.value.AutoValue;

import java.util.Objects;

/**
 * POJO for the identifying fields associated with a histogram of ad events registered by an adtech.
 *
 * <p>These events are used to compute frequency histograms to be used during ad selection
 * filtering.
 */
@AutoValue
@AutoValue.CopyAnnotations
@Entity(tableName = DBHistogramIdentifier.TABLE_NAME, inheritSuperIndices = true)
public abstract class DBHistogramIdentifier {
    public static final String TABLE_NAME = "fcap_histogram_ids";

    /**
     * Returns the numerical ID linking {@link DBHistogramEventData} to their associated
     * identifiers.
     *
     * <p>This ID is only used internally in the frequency cap histogram tables and does not need to
     * be stable or reproducible. It is auto-generated by Room if set to {@code null} on insertion.
     */
    @AutoValue.CopyAnnotations
    @ColumnInfo(name = "foreign_key_id")
    @PrimaryKey(autoGenerate = true)
    @Nullable
    public abstract Long getHistogramIdentifierForeignKey();

    /**
     * Returns the arbitrary String representing a grouping that a buyer adtech has assigned to an
     * ad or histogram.
     */
    @AutoValue.CopyAnnotations
    @ColumnInfo(name = "ad_counter_key", index = true)
    @NonNull
    public abstract String getAdCounterKey();

    /** Returns the histogram's buyer adtech's {@link AdTechIdentifier}. */
    @AutoValue.CopyAnnotations
    @ColumnInfo(name = "buyer", index = true)
    @NonNull
    public abstract AdTechIdentifier getBuyer();

    /** Returns the owner package name of the custom audience the histogram is associated with. */
    @AutoValue.CopyAnnotations
    @ColumnInfo(name = "custom_audience_owner", index = true)
    @Nullable
    public abstract String getCustomAudienceOwner();

    /** Returns the name of the custom audience the histogram is associated with. */
    @AutoValue.CopyAnnotations
    @ColumnInfo(name = "custom_audience_name", index = true)
    @Nullable
    public abstract String getCustomAudienceName();

    /** Returns an AutoValue builder for a {@link DBHistogramIdentifier} object. */
    @NonNull
    public static Builder builder() {
        return new AutoValue_DBHistogramIdentifier.Builder()
                .setHistogramIdentifierForeignKey(
                        SharedStorageDatabase.FOREIGN_KEY_AUTOGENERATE_SUBSTITUTE);
    }

    /**
     * Creates a {@link DBHistogramIdentifier} object using the builder.
     *
     * <p>Required for Room SQLite integration.
     */
    @NonNull
    public static DBHistogramIdentifier create(
            @Nullable Long histogramIdentifierForeignKey,
            @NonNull String adCounterKey,
            @NonNull AdTechIdentifier buyer,
            @Nullable String customAudienceOwner,
            @Nullable String customAudienceName) {
        return builder()
                .setHistogramIdentifierForeignKey(histogramIdentifierForeignKey)
                .setAdCounterKey(adCounterKey)
                .setBuyer(buyer)
                .setCustomAudienceOwner(customAudienceOwner)
                .setCustomAudienceName(customAudienceName)
                .build();
    }

    /**
     * Creates and returns a new {@link DBHistogramIdentifier} object from the given {@link
     * HistogramEvent}.
     *
     * <p>The resulting {@link DBHistogramIdentifier} object is built to autogenerate a foreign key
     * ID on insertion into the database.
     */
    @NonNull
    public static DBHistogramIdentifier fromHistogramEvent(@NonNull HistogramEvent event) {
        Objects.requireNonNull(event);

        Builder tempBuilder =
                builder().setAdCounterKey(event.getAdCounterKey()).setBuyer(event.getBuyer());

        // Only win-typed events must be scoped to a custom audience, so leave them null otherwise
        if (event.getAdEventType() == FrequencyCapFilters.AD_EVENT_TYPE_WIN) {
            tempBuilder
                    .setCustomAudienceOwner(event.getCustomAudienceOwner())
                    .setCustomAudienceName(event.getCustomAudienceName());
        }

        return tempBuilder.build();
    }

    /** Builder class for a {@link DBHistogramIdentifier} object. */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Sets the numerical ID linking {@link DBHistogramEventData} to their associated
         * identifiers.
         *
         * <p>This ID is only used internally in the frequency cap histogram tables and does not
         * need to be stable or reproducible. It is auto-generated by Room if set to {@code null} on
         * insertion.
         */
        @NonNull
        public abstract Builder setHistogramIdentifierForeignKey(@Nullable Long value);

        /**
         * Sets the arbitrary String representing a grouping that a buyer adtech has assigned to an
         * ad or histogram.
         */
        @NonNull
        public abstract Builder setAdCounterKey(@NonNull String value);

        /** Sets the histogram's buyer adtech's {@link AdTechIdentifier}. */
        @NonNull
        public abstract Builder setBuyer(@NonNull AdTechIdentifier value);

        /** Sets the owner package name of the custom audience the histogram is associated with. */
        @NonNull
        public abstract Builder setCustomAudienceOwner(@Nullable String value);

        /** Sets the name of the custom audience the histogram is associated with. */
        @NonNull
        public abstract Builder setCustomAudienceName(@Nullable String value);

        /**
         * Builds and returns the {@link DBHistogramIdentifier} object.
         *
         * @throws IllegalStateException if any required field is unset when the object is built
         */
        @NonNull
        public abstract DBHistogramIdentifier build();
    }
}
