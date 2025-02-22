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
package android.car.apitest;

import android.car.test.AbstractExpectableTestCase;
import android.car.test.ApiCheckerRule;
import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;

/**
 * Base class for tests that don't need to connect to a {@link android.car.Car} object.
 *
 * <p>Typically used to test POJO-like (Plain-Old Java Objects) classes; for tests that need a
 * {@link android.car.Car} object, use {@link CarApiTestBase} instead.
 */
public abstract class CarLessApiTestBase extends AbstractExpectableTestCase {

    private static final String TAG = CarLessApiTestBase.class.getSimpleName();

    protected final Context mContext = InstrumentationRegistry.getInstrumentation()
            .getTargetContext();

    // TODO(b/242350638): temporary hack to allow subclasses to disable checks - should be removed
    // when not needed anymore
    private final ApiCheckerRule.Builder mApiCheckerRuleBuilder = new ApiCheckerRule.Builder();

    @Rule
    public final ApiCheckerRule mApiCheckerRule;

    // TODO(b/242350638): temporary hack to allow subclasses to disable checks - should be removed
    // when not needed anymore
    protected CarLessApiTestBase() {
        configApiCheckerRule(mApiCheckerRuleBuilder);
        mApiCheckerRule = mApiCheckerRuleBuilder.build();
    }

    // TODO(b/242350638): temporary hack to allow subclasses to disable checks - should be removed
    // when not needed anymore
    protected void configApiCheckerRule(ApiCheckerRule.Builder builder) {
        Log.v(TAG, "Good News, Everyone! Class " + getClass()
                + " doesn't override configApiCheckerRule()");
    }
}
