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
package com.android.adservices.ui.settings.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.RequiresApi;
import androidx.core.view.WindowCompat;

import com.android.adservices.service.FlagsFactory;
import com.android.adservices.ui.ModeSelector;
import com.android.adservices.ui.OTAResourcesManager;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

/**
 * Android application activity for controlling settings related to PP (Privacy Preserving) APIs.
 * This class is the base class for all other activities. We need an activity for each page in order
 * for {@link CollapsingToolbarBaseActivity} to work properly.
 */
// TODO(b/269798827): Enable for R.
@RequiresApi(Build.VERSION_CODES.S)
public abstract class AdServicesBaseActivity extends CollapsingToolbarBaseActivity
        implements ModeSelector {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (FlagsFactory.getFlags().getUiOtaStringsFeatureEnabled()) {
            OTAResourcesManager.applyOTAResources(getApplicationContext(), false);
        }
        if (FlagsFactory.getFlags().getU18UxEnabled()) {
            initWithMode(/* should refresh UI */ false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
