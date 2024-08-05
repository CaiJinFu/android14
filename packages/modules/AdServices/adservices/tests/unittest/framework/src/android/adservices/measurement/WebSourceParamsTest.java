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

package android.adservices.measurement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.net.Uri;
import android.os.Parcel;

import org.junit.Test;

/** Unit tests for {@link WebSourceParams}. */
public class WebSourceParamsTest {
    private static final Uri REGISTRATION_URI = Uri.parse("https://foo.com");
    private static final WebSourceParams WEB_SOURCE_PARAMS =
            new WebSourceParams.Builder(REGISTRATION_URI).setDebugKeyAllowed(true).build();

    private WebSourceParams createExampleRegistration() {
        return new WebSourceParams.Builder(REGISTRATION_URI).setDebugKeyAllowed(true).build();
    }

    private void verifyExampleRegistration(WebSourceParams request) {
        assertEquals(REGISTRATION_URI, request.getRegistrationUri());
        assertTrue(request.isDebugKeyAllowed());
    }

    @Test
    public void testCreationAttribution() {
        verifyExampleRegistration(WEB_SOURCE_PARAMS);
    }

    @Test
    public void testParcelingAttribution() {
        Parcel p = Parcel.obtain();
        WEB_SOURCE_PARAMS.writeToParcel(p, 0);
        p.setDataPosition(0);
        verifyExampleRegistration(WebSourceParams.CREATOR.createFromParcel(p));
        p.recycle();
    }

    @Test
    public void testDescribeContents() {
        assertEquals(0, createExampleRegistration().describeContents());
    }
}
