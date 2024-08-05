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

package com.android.adservices.data.topics.migration;

import static org.junit.Assert.assertThrows;

import android.database.sqlite.SQLiteDatabase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit tests for {@link com.android.adservices.data.topics.migration.AbstractTopicsDbMigrator} */
public class AbstractTopicsDbMigratorTest {
    @Mock private SQLiteDatabase mDb;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPerformMigration_onUpgrade() {
        // Test targetVersion is newer than newVersion on upgrading
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        initMigrator(/* targetVersion */ 3)
                                .performMigration(mDb, /* oldVersion */ 1, /* newVersion */ 2));

        // Test targetVersion is not newer than oldVersion on upgrading
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        initMigrator(/* targetVersion */ 1)
                                .performMigration(mDb, /* oldVersion */ 1, /* newVersion */ 2));

        // Test to perform on Upgrading
        initMigrator(/* targetVersion */ 2)
                .performMigration(mDb, /* oldVersion */ 1, /* newVersion */ 2);
    }

    // Initialize the abstractTopicsDbMigrator with a target version. Use isPerformed to indicate
    // when performMigration is invoked.
    private AbstractTopicsDbMigrator initMigrator(int targetVersion) {
        return new AbstractTopicsDbMigrator(targetVersion) {
            @Override
            public void performMigration(SQLiteDatabase db) {}
        };
    }
}
