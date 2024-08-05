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

package com.android.adservices.service.adselection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.adservices.common.AdSelectionSignals;
import android.adservices.common.CommonFixture;
import android.adservices.customaudience.CustomAudienceFixture;
import android.net.Uri;

import com.android.adservices.customaudience.DBCustomAudienceFixture;
import com.android.adservices.data.customaudience.DBCustomAudience;
import com.android.adservices.data.customaudience.DBTrustedBiddingData;
import com.android.adservices.service.common.httpclient.AdServicesHttpClientResponse;
import com.android.adservices.service.common.httpclient.AdServicesHttpsClient;
import com.android.adservices.service.devapi.CustomAudienceDevOverridesHelper;
import com.android.adservices.service.devapi.DevContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RunWith(MockitoJUnitRunner.class)
public class TrustedBiddingDataFetcherTest {

    private static final String NAME_1 = "n1";
    private static final String NAME_2 = "n2";
    private static final String NAME_3 = "n3";

    private static final String KEY_1 = "k1";
    private static final String KEY_2 = "k2";
    private static final String KEY_3 = "k3";

    private static final String VALUE_1 = "v1";
    private static final String VALUE_2 = "v2";

    private static final List<String> KEYS_1 = ImmutableList.of(KEY_1, KEY_2);
    private static final List<String> KEYS_2 = ImmutableList.of(KEY_2, KEY_3);
    private static final List<String> ALL_KEYS = ImmutableList.of(KEY_1, KEY_2, KEY_3);

    private static final Uri PATH_1 = CommonFixture.getUri(CommonFixture.VALID_BUYER_1, "/p1/");
    private static final Uri PATH_2 = CommonFixture.getUri(CommonFixture.VALID_BUYER_1, "/p2/");

    @Mock private AdServicesHttpsClient mAdServicesHttpsClient;
    @Mock private DevContext mDevContext;
    @Mock private CustomAudienceDevOverridesHelper mCustomAudienceDevOverridesHelper;

    private TrustedBiddingDataFetcher mTrustedBiddingDataFetcher;

    @Before
    public void setup() {
        mTrustedBiddingDataFetcher =
                new TrustedBiddingDataFetcher(
                        mAdServicesHttpsClient,
                        mDevContext,
                        mCustomAudienceDevOverridesHelper,
                        MoreExecutors.newDirectExecutorService());
    }

    @Test
    public void testDevOptionDisabled_fetchForMultipleUri()
            throws ExecutionException, InterruptedException {
        when(mAdServicesHttpsClient.fetchPayload(
                        argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_1, ALL_KEYS))))
                .thenReturn(
                        Futures.immediateFuture(
                                AdServicesHttpClientResponse.builder()
                                        .setResponseBody(
                                                new JSONObject(ImmutableMap.of(KEY_1, VALUE_1))
                                                        .toString())
                                        .build()));
        when(mAdServicesHttpsClient.fetchPayload(
                        argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_2, KEYS_2))))
                .thenReturn(
                        Futures.immediateFuture(
                                AdServicesHttpClientResponse.builder()
                                        .setResponseBody(
                                                new JSONObject(ImmutableMap.of(KEY_2, VALUE_2))
                                                        .toString())
                                        .build()));
        Map<Uri, JSONObject> result =
                mTrustedBiddingDataFetcher
                        .getTrustedBiddingDataForBuyer(
                                ImmutableList.of(
                                        getCustomAudience(NAME_1, PATH_1, KEYS_1),
                                        getCustomAudience(NAME_2, PATH_1, KEYS_2),
                                        getCustomAudience(NAME_3, PATH_2, KEYS_2)))
                        .get();

        assertEquals(2, result.size());
        assertTrue(result.containsKey(PATH_1));
        assertEquals(
                new JSONObject(ImmutableMap.of(KEY_1, VALUE_1)).toString(),
                result.get(PATH_1).toString());
        assertTrue(result.containsKey(PATH_2));
        assertEquals(
                new JSONObject(ImmutableMap.of(KEY_2, VALUE_2)).toString(),
                result.get(PATH_2).toString());

        verify(mAdServicesHttpsClient)
                .fetchPayload(argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_1, ALL_KEYS)));
        verify(mAdServicesHttpsClient)
                .fetchPayload(argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_2, KEYS_2)));

        verify(mDevContext).getDevOptionsEnabled();
        verifyNoMoreInteractions(
                mAdServicesHttpsClient, mDevContext, mCustomAudienceDevOverridesHelper);
    }

    @Test
    public void testDevOptionDisabled_fetchFailedOrNoResultFromServer()
            throws ExecutionException, InterruptedException {
        when(mAdServicesHttpsClient.fetchPayload(
                        argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_1, KEYS_1))))
                .thenReturn(
                        Futures.immediateFuture(
                                AdServicesHttpClientResponse.builder()
                                        .setResponseBody(
                                                new JSONObject(ImmutableMap.of(KEY_1, VALUE_1))
                                                        .toString())
                                        .build()));
        when(mAdServicesHttpsClient.fetchPayload(
                        argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_2, KEYS_2))))
                .thenReturn(Futures.immediateFailedFuture(new Exception()));

        Map<Uri, JSONObject> result =
                mTrustedBiddingDataFetcher
                        .getTrustedBiddingDataForBuyer(
                                ImmutableList.of(
                                        getCustomAudience(NAME_1, PATH_1, KEYS_1),
                                        getCustomAudience(NAME_2, PATH_2, KEYS_2)))
                        .get();

        assertEquals(1, result.size());
        assertTrue(result.containsKey(PATH_1));
        assertEquals(
                new JSONObject(ImmutableMap.of(KEY_1, VALUE_1)).toString(),
                result.get(PATH_1).toString());
        assertNull(result.get(PATH_2));

        verify(mAdServicesHttpsClient)
                .fetchPayload(argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_1, KEYS_1)));
        verify(mAdServicesHttpsClient)
                .fetchPayload(argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_2, KEYS_2)));

        verify(mDevContext).getDevOptionsEnabled();
        verifyNoMoreInteractions(
                mAdServicesHttpsClient, mDevContext, mCustomAudienceDevOverridesHelper);
    }

    @Test
    public void testDevOptionDisabled_serverReturnedMalformedData()
            throws ExecutionException, InterruptedException {
        when(mAdServicesHttpsClient.fetchPayload(
                        argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_1, KEYS_1))))
                .thenReturn(
                        Futures.immediateFuture(
                                AdServicesHttpClientResponse.builder()
                                        .setResponseBody(
                                                new JSONObject(ImmutableMap.of(KEY_1, VALUE_1))
                                                        .toString())
                                        .build()));
        when(mAdServicesHttpsClient.fetchPayload(
                        argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_2, KEYS_2))))
                .thenReturn(
                        Futures.immediateFuture(
                                AdServicesHttpClientResponse.builder()
                                        .setResponseBody("not A VALID JSON{{}")
                                        .build()));

        Map<Uri, JSONObject> result =
                mTrustedBiddingDataFetcher
                        .getTrustedBiddingDataForBuyer(
                                ImmutableList.of(
                                        getCustomAudience(NAME_1, PATH_1, KEYS_1),
                                        getCustomAudience(NAME_2, PATH_2, KEYS_2)))
                        .get();

        assertEquals(1, result.size());
        assertTrue(result.containsKey(PATH_1));
        assertEquals(
                new JSONObject(ImmutableMap.of(KEY_1, VALUE_1)).toString(),
                result.get(PATH_1).toString());
        assertNull(result.get(PATH_2));

        verify(mAdServicesHttpsClient)
                .fetchPayload(argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_1, KEYS_1)));
        verify(mAdServicesHttpsClient)
                .fetchPayload(argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_2, KEYS_2)));
        verify(mDevContext).getDevOptionsEnabled();
        verifyNoMoreInteractions(
                mAdServicesHttpsClient, mDevContext, mCustomAudienceDevOverridesHelper);
    }

    @Test
    public void testDevOptionEnabled_someCAhasDevOverride()
            throws ExecutionException, InterruptedException {
        when(mDevContext.getDevOptionsEnabled()).thenReturn(true);
        when(mCustomAudienceDevOverridesHelper.getTrustedBiddingSignalsOverride(
                        CustomAudienceFixture.VALID_OWNER, CommonFixture.VALID_BUYER_1, NAME_1))
                .thenReturn(AdSelectionSignals.EMPTY);
        when(mAdServicesHttpsClient.fetchPayload(
                        argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_2, KEYS_2))))
                .thenReturn(
                        Futures.immediateFuture(
                                AdServicesHttpClientResponse.builder()
                                        .setResponseBody(
                                                new JSONObject(ImmutableMap.of(KEY_2, VALUE_2))
                                                        .toString())
                                        .build()));

        AdServicesHttpClientResponse temp =
                AdServicesHttpClientResponse.builder()
                        .setResponseBody(new JSONObject(ImmutableMap.of(KEY_2, VALUE_2)).toString())
                        .build();

        assertEquals(temp.getResponseHeaders(), ImmutableMap.of());
        Map<Uri, JSONObject> result =
                mTrustedBiddingDataFetcher
                        .getTrustedBiddingDataForBuyer(
                                ImmutableList.of(
                                        getCustomAudience(NAME_1, PATH_1, KEYS_1),
                                        getCustomAudience(NAME_2, PATH_2, KEYS_2)))
                        .get();

        assertEquals(1, result.size());
        assertTrue(result.containsKey(PATH_2));
        assertEquals(
                new JSONObject(ImmutableMap.of(KEY_2, VALUE_2)).toString(),
                result.get(PATH_2).toString());
        assertNull(result.get(PATH_1));

        verify(mDevContext).getDevOptionsEnabled();
        verify(mCustomAudienceDevOverridesHelper)
                .getTrustedBiddingSignalsOverride(
                        CustomAudienceFixture.VALID_OWNER, CommonFixture.VALID_BUYER_1, NAME_1);
        verify(mCustomAudienceDevOverridesHelper)
                .getTrustedBiddingSignalsOverride(
                        CustomAudienceFixture.VALID_OWNER, CommonFixture.VALID_BUYER_1, NAME_2);
        verify(mAdServicesHttpsClient)
                .fetchPayload(argThat(new TestTrustedBiddingDataUriKeysMatcher(PATH_2, KEYS_2)));
        verifyNoMoreInteractions(
                mAdServicesHttpsClient, mDevContext, mCustomAudienceDevOverridesHelper);
    }

    private DBCustomAudience getCustomAudience(String name, Uri path, List<String> keys) {
        return DBCustomAudienceFixture.getValidBuilderByBuyer(CommonFixture.VALID_BUYER_1)
                .setName(name)
                .setTrustedBiddingData(
                        new DBTrustedBiddingData.Builder().setUri(path).setKeys(keys).build())
                .build();
    }

    private static class TestTrustedBiddingDataUriKeysMatcher implements ArgumentMatcher<Uri> {
        private final Uri mPath;
        private final List<String> mKeys;

        TestTrustedBiddingDataUriKeysMatcher(Uri path, List<String> keys) {
            mPath = path;
            mKeys = keys;
        }

        @Override
        public boolean matches(Uri argument) {
            if (argument == null) {
                return false;
            }

            if (!Objects.equals(mPath.toString(), argument.toString().split("\\?")[0])) {
                return false;
            }

            List<String> argumentKeys =
                    Lists.newArrayList(
                            argument.getQueryParameter(DBTrustedBiddingData.QUERY_PARAM_KEYS)
                                    .split(","));
            return argumentKeys.size() == mKeys.size()
                    && argumentKeys.containsAll(mKeys)
                    && mKeys.containsAll(argumentKeys);
        }
    }
}
