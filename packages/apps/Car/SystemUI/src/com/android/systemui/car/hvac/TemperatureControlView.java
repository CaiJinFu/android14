/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.systemui.car.hvac;

import static android.car.VehiclePropertyIds.HVAC_POWER_ON;
import static android.car.VehiclePropertyIds.HVAC_TEMPERATURE_SET;

import android.car.hardware.CarPropertyConfig;
import android.car.hardware.CarPropertyValue;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;

import com.android.systemui.R;

import java.util.List;

public class TemperatureControlView extends LinearLayout implements HvacView {
    protected static final int BUTTON_REPEAT_INTERVAL_MS = 500;
    protected TextView mTempTextView;

    private static final int INVALID_ID = -1;

    private final int mAreaId;
    private final int mAvailableTextColor;
    private final int mUnavailableTextColor;

    private boolean mPowerOn = false;
    private boolean mTemperatureSetAvailable = false;
    private HvacPropertySetter mHvacPropertySetter;
    private String mTempInDisplay;
    private View mIncreaseButton;
    private View mDecreaseButton;
    private float mMinTempC;
    private float mMinTempF;
    private float mMaxTempC;
    private String mTemperatureFormatCelsius;
    private String mTemperatureFormatFahrenheit;

    private float mTemperatureIncrementCelsius;
    private float mTemperatureIncrementFahrenheit;
    private float mCurrentTempC = -1.0f;
    private boolean mDisplayInFahrenheit = true;

    public TemperatureControlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HvacView);
        mAreaId = typedArray.getInt(R.styleable.HvacView_hvacAreaId, INVALID_ID);
        mTemperatureFormatCelsius = getResources().getString(
                R.string.hvac_temperature_format_celsius);
        mTemperatureFormatFahrenheit = getResources().getString(
                R.string.hvac_temperature_format_fahrenheit);
        mTemperatureIncrementCelsius = getResources().getFloat(
                R.fraction.celsius_temperature_increment);
        mTemperatureIncrementFahrenheit = getResources().getFloat(
                R.fraction.fahrenheit_temperature_increment);

        mMinTempC = getResources().getFloat(R.dimen.hvac_min_value_celsius);
        mMinTempF = getResources().getFloat(R.dimen.hvac_min_value_fahrenheit);
        mMaxTempC = getResources().getFloat(R.dimen.hvac_max_value_celsius);
        mAvailableTextColor = ContextCompat.getColor(getContext(), R.color.system_bar_text_color);
        mUnavailableTextColor = ContextCompat.getColor(getContext(),
                R.color.system_bar_text_unavailable_color);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mTempTextView = requireViewById(R.id.hvac_temperature_text);
        mIncreaseButton = requireViewById(R.id.hvac_increase_button);
        mDecreaseButton = requireViewById(R.id.hvac_decrease_button);
        initButtons();
        updateTemperatureView();
    }

    @Override
    public void onHvacTemperatureUnitChanged(boolean usesFahrenheit) {
        mDisplayInFahrenheit = usesFahrenheit;
        updateTemperatureView();
    }

    @Override
    public void onPropertyChanged(CarPropertyValue value) {
        if (value.getPropertyId() == HVAC_TEMPERATURE_SET) {
            mCurrentTempC = (Float) value.getValue();
            mTemperatureSetAvailable = value.getStatus() == CarPropertyValue.STATUS_AVAILABLE;
        }

        if (value.getPropertyId() == HVAC_POWER_ON) {
            mPowerOn = (Boolean) value.getValue();
        }
        updateTemperatureView();
    }

    @Override
    public @HvacController.HvacProperty Integer getHvacPropertyToView() {
        return HVAC_TEMPERATURE_SET;
    }

    @Override
    public @HvacController.AreaId Integer getAreaId() {
        return mAreaId;
    }

    @Override
    public void setHvacPropertySetter(HvacPropertySetter hvacPropertySetter) {
        mHvacPropertySetter = hvacPropertySetter;
    }

    @Override
    public void setConfigInfo(CarPropertyConfig<?> carPropertyConfig) {
        List<Integer> configArray = carPropertyConfig.getConfigArray();
        // Need to divide by 10 because config array values are temperature values that have been
        // multiplied by 10.
        mMinTempC = configArray.get(0) / 10f;
        mMaxTempC = configArray.get(1) / 10f;
        mTemperatureIncrementCelsius = configArray.get(2) / 10f;

        mMinTempF = configArray.get(3) / 10f;
        mTemperatureIncrementFahrenheit = configArray.get(5) / 10f;
    }

    @Override
    public void onLocaleListChanged() {
        updateTemperatureView();
    }

    /**
     * Returns {@code true} if temperature should be available for change.
     */
    public boolean isTemperatureAvailableForChange() {
        return mPowerOn && mTemperatureSetAvailable && mHvacPropertySetter != null;
    }

    /**
     * Updates the temperature view logic on the UI thread.
     */
    protected void updateTemperatureViewUiThread() {
        mTempTextView.setText(mTempInDisplay);
        mTempTextView.setTextColor(mPowerOn && mTemperatureSetAvailable
                ? mAvailableTextColor : mUnavailableTextColor);
    }

    protected String getTempInDisplay() {
        return mTempInDisplay;
    }

    protected float getCurrentTempC() {
        return mCurrentTempC;
    }

    @VisibleForTesting
    String getTempFormatInFahrenheit() {
        return mTemperatureFormatFahrenheit;
    }

    @VisibleForTesting
    String getTempFormatInCelsius() {
        return mTemperatureFormatCelsius;
    }

    @VisibleForTesting
    float getCelsiusTemperatureIncrement() {
        return mTemperatureIncrementCelsius;
    }

    @VisibleForTesting
    float getFahrenheitTemperatureIncrement() {
        return mTemperatureIncrementFahrenheit;
    }

    private void initButtons() {
        mIncreaseButton.setOnClickListener((v) -> incrementTemperature(true));
        mDecreaseButton.setOnClickListener((v) -> incrementTemperature(false));

        setHoldToRepeatButton(mIncreaseButton);
        setHoldToRepeatButton(mDecreaseButton);
    }

    private void incrementTemperature(boolean increment) {
        if (!mPowerOn) return;

        float newTempC = increment
                ? mCurrentTempC + mTemperatureIncrementCelsius
                : mCurrentTempC - mTemperatureIncrementCelsius;
        setTemperature(newTempC);
    }

    private void updateTemperatureView() {
        float tempToDisplayUnformatted =
                mDisplayInFahrenheit ? celsiusToFahrenheit(mCurrentTempC) : mCurrentTempC;

        mTempInDisplay = String.format(
                mDisplayInFahrenheit ? mTemperatureFormatFahrenheit : mTemperatureFormatCelsius,
                tempToDisplayUnformatted);
        mContext.getMainExecutor().execute(this::updateTemperatureViewUiThread);
    }

    private void setTemperature(float tempC) {
        tempC = Math.min(tempC, mMaxTempC);
        tempC = Math.max(tempC, mMinTempC);
        if (isTemperatureAvailableForChange()) {
            mHvacPropertySetter.setHvacProperty(HVAC_TEMPERATURE_SET, mAreaId, tempC);
        }
    }

    /**
     * Configures the {@code button} to perform its click action repeatedly if pressed and held with
     * {@link #BUTTON_REPEAT_INTERVAL_MS}.
     */
    private void setHoldToRepeatButton(View button) {
        Runnable repeatClickRunnable = new Runnable() {
            @Override
            public void run() {
                button.performClick();
                mContext.getMainThreadHandler().postDelayed(this, BUTTON_REPEAT_INTERVAL_MS);
            }
        };

        button.setOnTouchListener((view, event) -> {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // Handle click action here since click listener is suppressed.
                    repeatClickRunnable.run();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mContext.getMainThreadHandler().removeCallbacks(repeatClickRunnable);
            }

            // Return true so on click listener is not called superfluously.
            return true;
        });
    }

    private float celsiusToFahrenheit(float tempC) {
        int numIncrements = Math.round((tempC - mMinTempC) / mTemperatureIncrementCelsius);
        return mTemperatureIncrementFahrenheit * numIncrements + mMinTempF;
    }
}
