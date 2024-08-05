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

package android.adservices.cts;

import static com.google.common.truth.Truth.assertThat;

import android.adservices.common.AdDataFixture;
import android.adservices.common.AdFilters;
import android.adservices.common.AdFiltersFixture;
import android.adservices.common.AppInstallFilters;
import android.adservices.common.AppInstallFiltersFixture;
import android.adservices.common.CommonFixture;
import android.adservices.common.FrequencyCapFiltersFixture;
import android.os.Parcel;

import androidx.test.filters.SmallTest;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

/** Unit tests for {@link AdFilters}. */
@SmallTest
public class AdFiltersTest {

    private static final String DIFFERENT_PACKAGE_NAME =
            CommonFixture.TEST_PACKAGE_NAME_1 + "arbitrary";

    private static final AdFilters APP_INSTALL_ONLY_FILTER =
            new AdFilters.Builder()
                    .setAppInstallFilters(AppInstallFiltersFixture.VALID_APP_INSTALL_FILTERS)
                    .build();

    @Ignore
    @Test
    public void testBuildNoSettersAppInstallOnly_success() {
        final AdFilters originalFilters = new AdFilters.Builder().build();

        assertThat(originalFilters.getAppInstallFilters()).isNull();
    }

    @Ignore
    @Test
    public void testBuildNullAdFiltersAppInstallOnly_success() {
        final AdFilters originalFilters =
                new AdFilters.Builder()
                        .setAppInstallFilters(null)
                        .build();

        assertThat(originalFilters.getAppInstallFilters()).isNull();
    }

    @Ignore
    @Test
    public void testBuildValidAdFiltersAppInstallOnly_success() {
        assertThat(APP_INSTALL_ONLY_FILTER.getAppInstallFilters())
                .isEqualTo(AppInstallFiltersFixture.VALID_APP_INSTALL_FILTERS);
    }

    @Ignore
    @Test
    public void testParcelAdFiltersAppInstallOnly_success() {
        Parcel targetParcel = Parcel.obtain();
        APP_INSTALL_ONLY_FILTER.writeToParcel(targetParcel, 0);
        targetParcel.setDataPosition(0);
        final AdFilters filtersFromParcel = AdFilters.CREATOR.createFromParcel(targetParcel);

        assertThat(filtersFromParcel.getAppInstallFilters())
                .isEqualTo(APP_INSTALL_ONLY_FILTER.getAppInstallFilters());
    }

    @Ignore
    @Test
    public void testEqualsIdenticalAppInstallOnly_success() {
        final AdFilters identicalFilters =
                new AdFilters.Builder()
                        .setAppInstallFilters(AppInstallFiltersFixture.VALID_APP_INSTALL_FILTERS)
                        .build();

        assertThat(APP_INSTALL_ONLY_FILTER.equals(identicalFilters)).isTrue();
    }

    @Ignore
    @Test
    public void testEqualsDifferentAppInstallOnly_success() {
        final AdFilters differentFilters =
                new AdFilters.Builder()
                        .setAppInstallFilters(
                                new AppInstallFilters.Builder()
                                        .setPackageNames(
                                                new HashSet<>(
                                                        Arrays.asList(DIFFERENT_PACKAGE_NAME)))
                                        .build())
                        .build();

        assertThat(APP_INSTALL_ONLY_FILTER.equals(differentFilters)).isFalse();
    }

    @Ignore
    @Test
    public void testHashCodeIdenticalAppInstallOnly_success() {
        final AdFilters identicalFilters =
                new AdFilters.Builder()
                        .setAppInstallFilters(AppInstallFiltersFixture.VALID_APP_INSTALL_FILTERS)
                        .build();

        assertThat(APP_INSTALL_ONLY_FILTER.hashCode()).isEqualTo(identicalFilters.hashCode());
    }

    @Ignore
    @Test
    public void testHashCodeDifferentAppInstallOnly_success() {
        final AdFilters differentFilters =
                new AdFilters.Builder()
                        .setAppInstallFilters(
                                new AppInstallFilters.Builder()
                                        .setPackageNames(
                                                new HashSet<>(
                                                        Arrays.asList(DIFFERENT_PACKAGE_NAME)))
                                        .build())
                        .build();

        assertThat(APP_INSTALL_ONLY_FILTER.hashCode()).isNotEqualTo(differentFilters.hashCode());
    }

    @Ignore
    @Test
    public void testToString() {
        final AdFilters originalFilters = AdFiltersFixture.getValidUnhiddenFilters();

        final String expectedString =
                String.format("AdFilters{" + getFrequencyCapString() + getAppInstallString() + "}");
        assertThat(originalFilters.toString()).isEqualTo(expectedString);
    }

    private String getFrequencyCapString() {
        if (AdDataFixture.FCAP_ENABLED) {
            return "mFrequencyCapFilters=" + FrequencyCapFiltersFixture.VALID_FREQUENCY_CAP_FILTERS;
        } else {
            return "";
        }
    }

    private String getAppInstallString() {
        String toReturn = "";
        if (AdDataFixture.APP_INSTALL_ENABLED) {
            if (AdDataFixture.FCAP_ENABLED) {
                toReturn += ", ";
            }
            toReturn += "mAppInstallFilters=" + AppInstallFiltersFixture.VALID_APP_INSTALL_FILTERS;
        }
        return toReturn;
    }
}
