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
package com.android.adservices.ui.settings.delegates;

import android.os.Build;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.Observer;

import com.android.adservices.api.R;
import com.android.adservices.service.stats.UiStatsLogger;
import com.android.adservices.ui.settings.activities.MeasurementActivity;
import com.android.adservices.ui.settings.fragments.AdServicesSettingsMeasurementFragment;
import com.android.adservices.ui.settings.viewmodels.MeasurementViewModel;
import com.android.adservices.ui.settings.viewmodels.MeasurementViewModel.MeasurementViewModelUiEvent;
import com.android.settingslib.widget.MainSwitchBar;

/**
 * Delegate class that helps AdServices Settings fragments to respond to all view model/user events.
 */
// TODO(b/269798827): Enable for R.
@RequiresApi(Build.VERSION_CODES.S)
public class MeasurementActionDelegate {
    private final MeasurementActivity mMeasurementActivity;
    private final MeasurementViewModel mMeasurementViewModel;

    public MeasurementActionDelegate(
            MeasurementActivity measurementActivity, MeasurementViewModel measurementViewModel) {
        this.mMeasurementActivity = measurementActivity;
        this.mMeasurementViewModel = measurementViewModel;
        listenToMeasurementViewModelUiEvents();
    }

    private void listenToMeasurementViewModelUiEvents() {
        Observer<MeasurementViewModelUiEvent> observer =
                event -> {
                    if (event == null) {
                        return;
                    }
                    try {
                        switch (event) {
                            case SWITCH_ON_MEASUREMENT:
                                mMeasurementViewModel.setMeasurementConsent(true);
                                break;
                            case SWITCH_OFF_MEASUREMENT:
                                mMeasurementViewModel.setMeasurementConsent(false);
                                break;
                            case RESET_MEASUREMENT:
                                UiStatsLogger.logResetMeasurementSelected(mMeasurementActivity);
                                mMeasurementViewModel.resetMeasurement();
                                Toast.makeText(
                                                mMeasurementActivity,
                                                R.string.settingsUI_measurement_are_reset,
                                                Toast.LENGTH_SHORT)
                                        .show();
                                break;
                        }
                    } finally {
                        mMeasurementViewModel.uiEventHandled();
                    }
                };
        mMeasurementViewModel.getUiEvents().observe(mMeasurementActivity, observer);
    }

    /**
     * Configure all UI elements in {@link AdServicesSettingsMeasurementFragment} to handle user
     * actions.
     */
    public void initMeasurementFragment(AdServicesSettingsMeasurementFragment fragment) {
        mMeasurementActivity.setTitle(R.string.settingsUI_measurement_view_title);
        configureMeasurementConsentSwitch(fragment);
        configureResetMeasurementButton(fragment);
        configurePrivacyPolicyLink();
    }

    private void configureResetMeasurementButton(AdServicesSettingsMeasurementFragment fragment) {
        View resetMeasurementButton =
                fragment.requireView().findViewById(R.id.reset_measurement_button);

        resetMeasurementButton.setOnClickListener(
                view -> mMeasurementViewModel.resetMeasurementButtonClickHandler());
    }

    private void configureMeasurementConsentSwitch(AdServicesSettingsMeasurementFragment fragment) {
        MainSwitchBar measurementSwitchBar =
                mMeasurementActivity.findViewById(R.id.measurement_switch_bar);
        mMeasurementViewModel
                .getMeasurementConsent()
                .observe(fragment, measurementSwitchBar::setChecked);
        measurementSwitchBar.setOnClickListener(
                switchBar ->
                        mMeasurementViewModel.consentSwitchClickHandler((MainSwitchBar) switchBar));
    }

    private void configurePrivacyPolicyLink() {
        TextView measurementFooter = mMeasurementActivity.findViewById(R.id.measurement_footer);
        measurementFooter.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
