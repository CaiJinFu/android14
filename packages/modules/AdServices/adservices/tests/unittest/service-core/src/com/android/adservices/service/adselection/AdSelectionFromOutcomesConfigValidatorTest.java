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

import static com.android.adservices.service.adselection.AdSelectionFromOutcomesConfigValidator.AD_OUTCOMES_CANNOT_BE_NULL_OR_EMPTY;
import static com.android.adservices.service.adselection.AdSelectionFromOutcomesConfigValidator.AD_SELECTION_IDS_DONT_EXIST;
import static com.android.adservices.service.adselection.AdSelectionFromOutcomesConfigValidator.INPUT_PARAM_CANNOT_BE_NULL;
import static com.android.adservices.service.adselection.AdSelectionFromOutcomesConfigValidator.SELECTION_LOGIC_URI_CANNOT_BE_NULL_OR_EMPTY;
import static com.android.adservices.service.adselection.AdSelectionFromOutcomesConfigValidator.SELLER_AND_URI_HOST_ARE_INCONSISTENT;
import static com.android.adservices.service.adselection.AdSelectionFromOutcomesConfigValidator.URI_IS_NOT_ABSOLUTE;
import static com.android.adservices.service.adselection.AdSelectionFromOutcomesConfigValidator.URI_IS_NOT_HTTPS;

import android.adservices.adselection.AdSelectionFromOutcomesConfig;
import android.adservices.adselection.AdSelectionFromOutcomesConfigFixture;
import android.adservices.adselection.CustomAudienceSignalsFixture;
import android.adservices.common.AdTechIdentifier;
import android.net.Uri;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.android.adservices.data.adselection.AdSelectionDatabase;
import com.android.adservices.data.adselection.AdSelectionEntryDao;
import com.android.adservices.data.adselection.CustomAudienceSignals;
import com.android.adservices.data.adselection.DBAdSelection;
import com.android.adservices.service.Flags;
import com.android.adservices.service.FlagsFactory;
import com.android.adservices.service.common.ValidatorTestUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Collections;

public class AdSelectionFromOutcomesConfigValidatorTest {
    private static final AdTechIdentifier SELLER_VALID_WITH_PREFIX =
            AdTechIdentifier.fromString("www.developer.android.com");
    private static final Uri SELECTION_LOGIC_URI_CONSISTENT_WITH_SELLER_WITH_PREFIX =
            Uri.parse("https://www.developer.android.com/finalWinnerSelectionLogic");
    private static final Uri SAMPLE_SELECTION_LOGIC_URI_DOESNT_MATCHES_SELLER =
            Uri.parse("https://selectionlogicuri.doesntmatchseller.com/finalWinnerSelectionLogic");
    private static final String AD_SELECTION_FROM_OUTCOME_INPUT_VIOLATION =
            String.format(
                    "Invalid object of type %s. The violations are:",
                    AdSelectionFromOutcomesConfig.class.getName());
    private static final String CALLER_PACKAGE_NAME = "com.caller.package";

    private AdSelectionEntryDao mAdSelectionEntryDao;
    private AdSelectionFromOutcomesConfigValidator mValidator;
    private PrebuiltLogicGenerator mPrebuiltLogicGenerator;
    private Flags mFlags;

    @Before
    public void setup() {
        mAdSelectionEntryDao =
                Room.inMemoryDatabaseBuilder(
                                ApplicationProvider.getApplicationContext(),
                                AdSelectionDatabase.class)
                        .build()
                        .adSelectionEntryDao();
        mFlags = FlagsFactory.getFlagsForTest();
        mPrebuiltLogicGenerator = new PrebuiltLogicGenerator(mFlags);
        mValidator =
                new AdSelectionFromOutcomesConfigValidator(
                        mAdSelectionEntryDao, CALLER_PACKAGE_NAME, mPrebuiltLogicGenerator);
    }

    @Test
    public void testVerifyAdSelectionFromOutcomesConfigSuccess() {
        mValidator.validate(
                persistIds(AdSelectionFromOutcomesConfigFixture.anAdSelectionFromOutcomesConfig()));
    }

    @Test
    public void testVerifyAdSelectionFromOutcomesConfigWithSellerWithPrefixSuccess() {
        mValidator.validate(
                persistIds(
                        AdSelectionFromOutcomesConfigFixture.anAdSelectionFromOutcomesConfig(
                                SELLER_VALID_WITH_PREFIX,
                                SELECTION_LOGIC_URI_CONSISTENT_WITH_SELLER_WITH_PREFIX)));
    }

    @Test
    public void testVerifyAdSelectionFromOutcomesConfigCannotBeNull() {
        NullPointerException exception =
                Assert.assertThrows(NullPointerException.class, () -> mValidator.validate(null));
        Assert.assertEquals(exception.getMessage(), INPUT_PARAM_CANNOT_BE_NULL);
    }

    @Test
    public void testVerifyAdSelectionFromOutcomesConfigAdOutcomeIdsCannotBeEmpty() {
        IllegalArgumentException exception =
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mValidator.validate(
                                        AdSelectionFromOutcomesConfigFixture
                                                .anAdSelectionFromOutcomesConfig(
                                                        Collections.emptyList())));
        ValidatorTestUtil.assertValidationFailuresMatch(
                exception,
                AD_SELECTION_FROM_OUTCOME_INPUT_VIOLATION,
                Collections.singletonList(AD_OUTCOMES_CANNOT_BE_NULL_OR_EMPTY));
    }

    @Test
    public void testVerifyAdSelectionFromOutcomesConfigAdOutcomeIdsShouldExistInTheDB() {
        IllegalArgumentException exception =
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mValidator.validate(
                                        AdSelectionFromOutcomesConfigFixture
                                                .anAdSelectionFromOutcomesConfig()));

        String expectedViolation =
                String.format(
                        AD_SELECTION_IDS_DONT_EXIST,
                        AdSelectionFromOutcomesConfigFixture.anAdSelectionFromOutcomesConfig()
                                .getAdSelectionIds());

        ValidatorTestUtil.assertValidationFailuresMatch(
                exception,
                AD_SELECTION_FROM_OUTCOME_INPUT_VIOLATION,
                Collections.singletonList(expectedViolation));
    }

    @Test
    public void testVerifyAdSelectionFromOutcomesConfigUriCannotBeEmpty() {
        IllegalArgumentException exception =
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mValidator.validate(
                                        AdSelectionFromOutcomesConfigFixture
                                                .anAdSelectionFromOutcomesConfig(Uri.parse(""))));

        ValidatorTestUtil.assertValidationFailuresMatch(
                exception,
                AD_SELECTION_FROM_OUTCOME_INPUT_VIOLATION,
                Collections.singletonList(SELECTION_LOGIC_URI_CANNOT_BE_NULL_OR_EMPTY));
    }

    @Test
    public void testVerifyAdSelectionFromOutcomesConfigUriCannotBeRelative() {
        IllegalArgumentException exception =
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mValidator.validate(
                                        AdSelectionFromOutcomesConfigFixture
                                                .anAdSelectionFromOutcomesConfig(
                                                        Uri.parse("/this/is/relative/path"))));

        ValidatorTestUtil.assertValidationFailuresMatch(
                exception,
                AD_SELECTION_FROM_OUTCOME_INPUT_VIOLATION,
                Collections.singletonList(URI_IS_NOT_ABSOLUTE));
    }

    @Test
    public void testVerifyAdSelectionFromOutcomesConfigUriMustBeHttps() {
        IllegalArgumentException exception =
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mValidator.validate(
                                        AdSelectionFromOutcomesConfigFixture
                                                .anAdSelectionFromOutcomesConfig(
                                                        Uri.parse("http://google.com"))));

        ValidatorTestUtil.assertValidationFailuresMatch(
                exception,
                AD_SELECTION_FROM_OUTCOME_INPUT_VIOLATION,
                Collections.singletonList(URI_IS_NOT_HTTPS));
    }

    @Test
    public void testVerifyAdSelectionFromOutcomesConfigUriHostShouldBeSameAsSellerHost() {
        AdSelectionFromOutcomesConfig config =
                AdSelectionFromOutcomesConfigFixture.anAdSelectionFromOutcomesConfig(
                        SAMPLE_SELECTION_LOGIC_URI_DOESNT_MATCHES_SELLER);
        IllegalArgumentException exception =
                Assert.assertThrows(
                        IllegalArgumentException.class, () -> mValidator.validate(config));
        String expectedViolation =
                String.format(
                        SELLER_AND_URI_HOST_ARE_INCONSISTENT,
                        Uri.parse("https://" + config.getSeller()).getHost(),
                        config.getSelectionLogicUri().getHost());
        ValidatorTestUtil.assertValidationFailuresMatch(
                exception,
                AD_SELECTION_FROM_OUTCOME_INPUT_VIOLATION,
                Collections.singletonList(expectedViolation));
    }

    private AdSelectionFromOutcomesConfig persistIds(AdSelectionFromOutcomesConfig config) {
        final Uri biddingLogicUri1 = Uri.parse("https://www.domain.com/logic/1");
        final Uri renderUri = Uri.parse("https://www.domain.com/advert/");
        final Instant activationTime = Instant.now();
        final String contextualSignals = "contextual_signals";
        final CustomAudienceSignals customAudienceSignals =
                CustomAudienceSignalsFixture.aCustomAudienceSignals();

        for (Long adOutcomeId : config.getAdSelectionIds()) {
            final DBAdSelection dbAdSelectionEntry =
                    new DBAdSelection.Builder()
                            .setAdSelectionId(adOutcomeId)
                            .setCustomAudienceSignals(customAudienceSignals)
                            .setContextualSignals(contextualSignals)
                            .setBiddingLogicUri(biddingLogicUri1)
                            .setWinningAdRenderUri(renderUri)
                            .setWinningAdBid(adOutcomeId * 10.0)
                            .setCreationTimestamp(activationTime)
                            .setCallerPackageName(CALLER_PACKAGE_NAME)
                            .build();
            mAdSelectionEntryDao.persistAdSelection(dbAdSelectionEntry);
        }
        return config;
    }
}
