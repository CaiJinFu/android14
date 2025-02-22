/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.intelligence.search.indexing.car;

import android.content.Context;

import com.android.settings.intelligence.search.indexing.DatabaseIndexingManager;
import com.android.settings.intelligence.search.indexing.IndexDataConverter;
import com.android.settings.intelligence.search.indexing.PreIndexData;

/**
 * Car extension to {@link DatabaseIndexingManager} to use {@link CarIndexDataConverter} for
 * converting {@link PreIndexData} into {@link CarIndexData}.
 */
public class CarDatabaseIndexingManager extends DatabaseIndexingManager {

    public CarDatabaseIndexingManager(Context context) {
        super(context);
    }

    @Override
    protected IndexDataConverter getIndexDataConverter() {
        return new CarIndexDataConverter();
    }
}
