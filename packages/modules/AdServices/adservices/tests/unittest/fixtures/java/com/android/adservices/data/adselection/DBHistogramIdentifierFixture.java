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

import com.android.adservices.service.adselection.HistogramEventFixture;

public class DBHistogramIdentifierFixture {
    public static final long VALID_FOREIGN_KEY = 10;

    public static final DBHistogramIdentifier VALID_DB_HISTOGRAM_IDENTIFIER =
            getValidDBHistogramIdentifierBuilder().build();

    public static DBHistogramIdentifier.Builder getValidDBHistogramIdentifierBuilder() {
        return DBHistogramIdentifier.builder()
                .setAdCounterKey(HistogramEventFixture.VALID_HISTOGRAM_EVENT.getAdCounterKey())
                .setBuyer(HistogramEventFixture.VALID_HISTOGRAM_EVENT.getBuyer())
                .setCustomAudienceOwner(
                        HistogramEventFixture.VALID_HISTOGRAM_EVENT.getCustomAudienceOwner())
                .setCustomAudienceName(
                        HistogramEventFixture.VALID_HISTOGRAM_EVENT.getCustomAudienceName());
    }
}
