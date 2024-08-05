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

package com.android.adservices.service.adselection;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.android.adservices.data.adselection.AppInstallDao;
import com.android.adservices.data.adselection.FrequencyCapDao;
import com.android.adservices.data.adselection.SharedStorageDatabase;
import com.android.adservices.service.Flags;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AdFilteringFeatureFactoryTest {
    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();
    AppInstallDao mAppInstallDao =
            Room.inMemoryDatabaseBuilder(CONTEXT, SharedStorageDatabase.class)
                    .build()
                    .appInstallDao();
    FrequencyCapDao mFrequencyCapDao =
            Room.inMemoryDatabaseBuilder(CONTEXT, SharedStorageDatabase.class)
                    .build()
                    .frequencyCapDao();

    @Test
    public void testGetAdFiltererFilteringEnabled() {
        AdFilteringFeatureFactory adFilteringFeatureFactory =
                new AdFilteringFeatureFactory(
                        mAppInstallDao,
                        mFrequencyCapDao,
                        new FlagsWithAdSelectionFilteringEnabled());
        assertTrue(adFilteringFeatureFactory.getAdFilterer() instanceof AdFiltererImpl);
    }

    @Test
    public void testGetAdFiltererFilteringDisabled() {
        AdFilteringFeatureFactory adFilteringFeatureFactory =
                new AdFilteringFeatureFactory(
                        null, null, new FlagsWithAdSelectionFilteringDisabled());
        assertTrue(adFilteringFeatureFactory.getAdFilterer() instanceof AdFiltererNoOpImpl);
    }

    @Test
    public void testGetAdCounterKeyCopierFilteringEnabled() {
        AdFilteringFeatureFactory adFilteringFeatureFactory =
                new AdFilteringFeatureFactory(
                        mAppInstallDao,
                        mFrequencyCapDao,
                        new FlagsWithAdSelectionFilteringEnabled());
        assertTrue(
                adFilteringFeatureFactory.getAdCounterKeyCopier()
                        instanceof AdCounterKeyCopierImpl);
    }

    @Test
    public void testGetAdCounterKeyCopierFilteringDisabled() {
        AdFilteringFeatureFactory adFilteringFeatureFactory =
                new AdFilteringFeatureFactory(
                        null, null, new FlagsWithAdSelectionFilteringDisabled());
        assertTrue(
                adFilteringFeatureFactory.getAdCounterKeyCopier()
                        instanceof AdCounterKeyCopierNoOpImpl);
    }

    private static class FlagsWithAdSelectionFilteringDisabled implements Flags {
        @Override
        public boolean getFledgeAdSelectionFilteringEnabled() {
            return false;
        }
    }

    private static class FlagsWithAdSelectionFilteringEnabled implements Flags {
        @Override
        public boolean getFledgeAdSelectionFilteringEnabled() {
            return true;
        }
    }
}
