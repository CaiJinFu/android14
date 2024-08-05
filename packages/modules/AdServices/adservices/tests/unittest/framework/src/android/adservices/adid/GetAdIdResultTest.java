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
package android.adservices.adid;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcel;

import androidx.test.filters.SmallTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

/** Unit tests for {@link GetAdIdResult} */
@SmallTest
public final class GetAdIdResultTest {
    @Test
    public void testWriteToParcel() throws Exception {
        GetAdIdResult response =
                new GetAdIdResult.Builder().setAdId("UNITTEST_ADID").setLatEnabled(true).build();
        Parcel p = Parcel.obtain();
        response.writeToParcel(p, 0);
        p.setDataPosition(0);

        GetAdIdResult fromParcel = GetAdIdResult.CREATOR.createFromParcel(p);
        assertEquals(GetAdIdResult.CREATOR.newArray(1).length, 1);

        assertEquals(fromParcel.getAdId(), "UNITTEST_ADID");
        assertEquals(fromParcel.isLatEnabled(), true);

        assertEquals(fromParcel.equals(response), true);
        assertEquals(fromParcel.equals(fromParcel), true);
        assertEquals(fromParcel.equals("UNITTEST_ADID"), false);

        assertEquals(fromParcel.hashCode(), response.hashCode());

        assertEquals(response.getErrorMessage(), null);

        assertEquals(response.describeContents(), 0);

        assertThat(response.toString()).isNotNull();
    }

    @Test
    public void testWriteToParcel_nullableThrows() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    GetAdIdResult unusedResponse =
                            new GetAdIdResult.Builder().setAdId(null).build();
                });
    }
}
