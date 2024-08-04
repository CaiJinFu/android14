/*
 * Copyright 2022 The Android Open Source Project
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

package com.android.internal.telephony;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import android.os.Parcel;
import android.telephony.DataSpecificRegistrationInfo;
import android.telephony.LteVopsSupportInfo;

import androidx.test.filters.SmallTest;

import org.junit.Test;

/** Unit tests for {@link DataSpecificRegistrationInfo}. */
public class DataSpecificRegistrationInfoTest {
    @Test
    @SmallTest
    public void testBuilder() {
        DataSpecificRegistrationInfo dsri =
                new DataSpecificRegistrationInfo.Builder(3)
                .setNrAvailable(true)
                .setEnDcAvailable(true)
                .build();

        assertEquals(3, dsri.maxDataCalls);
        assertEquals(false, dsri.isDcNrRestricted);
        assertEquals(true, dsri.isNrAvailable);
        assertEquals(true, dsri.isEnDcAvailable);
        assertNull(dsri.getVopsSupportInfo());

        LteVopsSupportInfo vopsInfo = new LteVopsSupportInfo(
                LteVopsSupportInfo.LTE_STATUS_SUPPORTED, LteVopsSupportInfo.LTE_STATUS_SUPPORTED);
        dsri = new DataSpecificRegistrationInfo.Builder(5)
                .setDcNrRestricted(true)
                .setVopsSupportInfo(vopsInfo)
                .build();

        assertEquals(5, dsri.maxDataCalls);
        assertEquals(true, dsri.isDcNrRestricted);
        assertEquals(false, dsri.isNrAvailable);
        assertEquals(false, dsri.isEnDcAvailable);
        assertEquals(vopsInfo, dsri.getVopsSupportInfo());
    }

    @Test
    @SmallTest
    public void testParcel() {
        DataSpecificRegistrationInfo dsri =
                new DataSpecificRegistrationInfo.Builder(3)
                .setNrAvailable(true)
                .setEnDcAvailable(true)
                .setLteAttachResultType(DataSpecificRegistrationInfo.LTE_ATTACH_TYPE_COMBINED)
                .setLteAttachExtraInfo(DataSpecificRegistrationInfo.LTE_ATTACH_EXTRA_INFO_SMS_ONLY)
                .build();

        Parcel p = Parcel.obtain();
        dsri.writeToParcel(p, 0);
        p.setDataPosition(0);

        DataSpecificRegistrationInfo newDsri =
                DataSpecificRegistrationInfo.CREATOR.createFromParcel(p);
        assertEquals(dsri, newDsri);
    }
}
