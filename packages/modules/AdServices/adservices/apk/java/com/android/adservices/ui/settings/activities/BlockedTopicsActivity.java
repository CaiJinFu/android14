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

import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;

import com.android.adservices.api.R;
import com.android.adservices.ui.settings.delegates.BlockedTopicsActionDelegate;
import com.android.adservices.ui.settings.fragments.AdServicesSettingsBlockedTopicsFragment;
import com.android.adservices.ui.settings.viewmodels.BlockedTopicsViewModel;

/**
 * Android application activity for controlling blocked topics that were generated from Topics API.
 */
// TODO(b/269798827): Enable for R.
@RequiresApi(Build.VERSION_CODES.S)
public class BlockedTopicsActivity extends AdServicesBaseActivity {
    private BlockedTopicsActionDelegate mActionDelegate;

    /** @return the action delegate for the activity. */
    public BlockedTopicsActionDelegate getActionDelegate() {
        return mActionDelegate;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adservices_settings_main_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragment_container_view,
                        AdServicesSettingsBlockedTopicsFragment.class,
                        null)
                .setReorderingAllowed(true)
                .commit();
        initActionDelegate();
    }

    @Override
    public void initBeta() {}

    @Override
    public void initGA() {}

    @Override
    public void initU18() {}

    private void initActionDelegate() {
        mActionDelegate =
                new BlockedTopicsActionDelegate(
                        this, new ViewModelProvider(this).get(BlockedTopicsViewModel.class));
    }
}
