/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.adservices.service.measurement.reporting;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.util.Pair;

import com.android.adservices.data.measurement.DatastoreException;
import com.android.adservices.data.measurement.IMeasurementDao;
import com.android.adservices.service.Flags;
import com.android.adservices.service.FlagsFactory;
import com.android.adservices.service.common.AllowLists;
import com.android.adservices.service.measurement.EventSurfaceType;
import com.android.adservices.service.measurement.Source;
import com.android.adservices.service.measurement.Trigger;
import com.android.adservices.service.measurement.util.UnsignedLong;
import com.android.adservices.service.stats.AdServicesLogger;
import com.android.adservices.service.stats.AdServicesLoggerImpl;
import com.android.adservices.service.stats.MsmtDebugKeysMatchStats;

import com.google.common.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Util class for DebugKeys */
public class DebugKeyAccessor {
    @NonNull private final Flags mFlags;
    @NonNull private final AdServicesLogger mAdServicesLogger;
    @NonNull private final IMeasurementDao mMeasurementDao;

    public DebugKeyAccessor(IMeasurementDao measurementDao) {
        this(FlagsFactory.getFlags(), AdServicesLoggerImpl.getInstance(), measurementDao);
    }

    @VisibleForTesting
    DebugKeyAccessor(
            @NonNull Flags flags,
            @NonNull AdServicesLogger adServicesLogger,
            @NonNull IMeasurementDao measurementDao) {
        mFlags = flags;
        mAdServicesLogger = adServicesLogger;
        mMeasurementDao = measurementDao;
    }

    /**
     * This is kept in sync with the match type codes in {@link
     * com.android.adservices.service.stats.AdServicesStatsLog}.
     */
    @IntDef(
            value = {
                AttributionType.UNKNOWN,
                AttributionType.SOURCE_APP_TRIGGER_APP,
                AttributionType.SOURCE_APP_TRIGGER_WEB,
                AttributionType.SOURCE_WEB_TRIGGER_APP,
                AttributionType.SOURCE_WEB_TRIGGER_WEB
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AttributionType {
        int UNKNOWN = 0;
        int SOURCE_APP_TRIGGER_APP = 1;
        int SOURCE_APP_TRIGGER_WEB = 2;
        int SOURCE_WEB_TRIGGER_APP = 3;
        int SOURCE_WEB_TRIGGER_WEB = 4;
    }

    /** Returns DebugKey according to the permissions set */
    public Pair<UnsignedLong, UnsignedLong> getDebugKeys(Source source, Trigger trigger)
            throws DatastoreException {
        Set<String> allowedEnrollmentsString =
                new HashSet<>(
                        AllowLists.splitAllowList(
                                mFlags.getMeasurementDebugJoinKeyEnrollmentAllowlist()));
        String blockedEnrollmentsAdIdMatchingString =
                mFlags.getMeasurementPlatformDebugAdIdMatchingEnrollmentBlocklist();
        Set<String> blockedEnrollmentsAdIdMatching =
                new HashSet<>(AllowLists.splitAllowList(blockedEnrollmentsAdIdMatchingString));
        UnsignedLong sourceDebugKey = null;
        UnsignedLong triggerDebugKey = null;
        Long joinKeyHash = null;
        @AttributionType int attributionType = getAttributionType(source, trigger);
        boolean doDebugJoinKeysMatch = false;
        switch (attributionType) {
            case AttributionType.SOURCE_APP_TRIGGER_APP:
                if (source.hasAdIdPermission()) {
                    sourceDebugKey = source.getDebugKey();
                }
                if (trigger.hasAdIdPermission()) {
                    triggerDebugKey = trigger.getDebugKey();
                }
                break;
            case AttributionType.SOURCE_WEB_TRIGGER_WEB:
                // TODO(b/280323940): Web<>Web Debug Keys AdID option
                if (trigger.getRegistrant().equals(source.getRegistrant())) {
                    if (source.hasArDebugPermission()) {
                        sourceDebugKey = source.getDebugKey();
                    }
                    if (trigger.hasArDebugPermission()) {
                        triggerDebugKey = trigger.getDebugKey();
                    }
                } else if (canMatchJoinKeys(source, trigger, allowedEnrollmentsString)) {
                    // Attempted to match, so assigning a non-null value to emit metric
                    joinKeyHash = 0L;
                    if (source.getDebugJoinKey().equals(trigger.getDebugJoinKey())) {
                        sourceDebugKey = source.getDebugKey();
                        triggerDebugKey = trigger.getDebugKey();
                        joinKeyHash = (long) source.getDebugJoinKey().hashCode();
                        doDebugJoinKeysMatch = true;
                    }
                }
                break;
            case AttributionType.SOURCE_APP_TRIGGER_WEB:
                if (canMatchAdIdAppSourceToWebTrigger(source, trigger)
                        && canMatchAdIdEnrollments(
                                source,
                                trigger,
                                blockedEnrollmentsAdIdMatchingString,
                                blockedEnrollmentsAdIdMatching)) {
                    if (source.getPlatformAdId().equals(trigger.getDebugAdId())
                            && isEnrollmentIdWithinUniqueAdIdLimit(trigger.getEnrollmentId())) {
                        sourceDebugKey = source.getDebugKey();
                        triggerDebugKey = trigger.getDebugKey();
                    }
                    // TODO(b/280322027): Record result for metrics emission.
                    break;
                }
                // fall-through for join key matching
            case AttributionType.SOURCE_WEB_TRIGGER_APP:
                if (canMatchAdIdWebSourceToAppTrigger(source, trigger)
                        && canMatchAdIdEnrollments(
                                source,
                                trigger,
                                blockedEnrollmentsAdIdMatchingString,
                                blockedEnrollmentsAdIdMatching)) {
                    if (trigger.getPlatformAdId().equals(source.getDebugAdId())
                            && isEnrollmentIdWithinUniqueAdIdLimit(source.getEnrollmentId())) {
                        sourceDebugKey = source.getDebugKey();
                        triggerDebugKey = trigger.getDebugKey();
                    }
                    // TODO(b/280322027): Record result for metrics emission.
                    break;
                }
                if (canMatchJoinKeys(source, trigger, allowedEnrollmentsString)) {
                    // Attempted to match, so assigning a non-null value to emit metric
                    joinKeyHash = 0L;
                    if (source.getDebugJoinKey().equals(trigger.getDebugJoinKey())) {
                        sourceDebugKey = source.getDebugKey();
                        triggerDebugKey = trigger.getDebugKey();
                        joinKeyHash = (long) source.getDebugJoinKey().hashCode();
                        doDebugJoinKeysMatch = true;
                    }
                }
                break;
            case AttributionType.UNKNOWN:
                // fall-through
            default:
                break;
        }
        logDebugKeysMatch(
                joinKeyHash, trigger, attributionType, doDebugJoinKeysMatch, mAdServicesLogger);
        return new Pair<>(sourceDebugKey, triggerDebugKey);
    }

    /** Returns DebugKey according to the permissions set */
    public Pair<UnsignedLong, UnsignedLong> getDebugKeysForVerboseTriggerDebugReport(
            @NonNull Source source, Trigger trigger) throws DatastoreException {
        if (source == null) {
            if (trigger.getDestinationType() == EventSurfaceType.WEB
                    && trigger.hasArDebugPermission()) {
                return new Pair<>(null, trigger.getDebugKey());
            } else if (trigger.getDestinationType() == EventSurfaceType.APP
                    && trigger.hasAdIdPermission()) {
                return new Pair<>(null, trigger.getDebugKey());
            } else {
                return new Pair<>(null, null);
            }
        }
        Set<String> allowedEnrollmentsString =
                new HashSet<>(
                        AllowLists.splitAllowList(
                                mFlags.getMeasurementDebugJoinKeyEnrollmentAllowlist()));
        String blockedEnrollmentsAdIdMatchingString =
                mFlags.getMeasurementPlatformDebugAdIdMatchingEnrollmentBlocklist();
        Set<String> blockedEnrollmentsAdIdMatching =
                new HashSet<>(AllowLists.splitAllowList(blockedEnrollmentsAdIdMatchingString));
        UnsignedLong sourceDebugKey = null;
        UnsignedLong triggerDebugKey = null;
        Long joinKeyHash = null;
        @AttributionType int attributionType = getAttributionType(source, trigger);
        boolean doDebugJoinKeysMatch = false;
        switch (attributionType) {
            case AttributionType.SOURCE_APP_TRIGGER_APP:
                // Gated on Trigger Adid permission.
                if (!trigger.hasAdIdPermission()) {
                    break;
                }
                triggerDebugKey = trigger.getDebugKey();
                if (source.hasAdIdPermission()) {
                    sourceDebugKey = source.getDebugKey();
                }
                break;
            case AttributionType.SOURCE_WEB_TRIGGER_WEB:
                // Gated on Trigger ar_debug permission.
                if (!trigger.hasArDebugPermission()) {
                    break;
                }
                triggerDebugKey = trigger.getDebugKey();
                if (trigger.getRegistrant().equals(source.getRegistrant())) {
                    if (source.hasArDebugPermission()) {
                        sourceDebugKey = source.getDebugKey();
                    }
                } else {
                    // Send source_debug_key when condition meets.
                    if (canMatchJoinKeys(source, trigger, allowedEnrollmentsString)) {
                        // Attempted to match, so assigning a non-null value to emit metric
                        joinKeyHash = 0L;
                        if (source.getDebugJoinKey().equals(trigger.getDebugJoinKey())) {
                            sourceDebugKey = source.getDebugKey();
                            joinKeyHash = (long) source.getDebugJoinKey().hashCode();
                            doDebugJoinKeysMatch = true;
                        }
                    }
                }
                break;
            case AttributionType.SOURCE_APP_TRIGGER_WEB:
                // Gated on Trigger ar_debug permission.
                if (!trigger.hasArDebugPermission()) {
                    break;
                }
                triggerDebugKey = trigger.getDebugKey();
                // Send source_debug_key when condition meets.
                if (canMatchAdIdAppSourceToWebTrigger(source, trigger)
                        && canMatchAdIdEnrollments(
                                source,
                                trigger,
                                blockedEnrollmentsAdIdMatchingString,
                                blockedEnrollmentsAdIdMatching)) {
                    if (source.getPlatformAdId().equals(trigger.getDebugAdId())
                            && isEnrollmentIdWithinUniqueAdIdLimit(trigger.getEnrollmentId())) {
                        sourceDebugKey = source.getDebugKey();
                    }
                    // TODO(b/280322027): Record result for metrics emission.
                } else if (canMatchJoinKeys(source, trigger, allowedEnrollmentsString)) {
                    // Attempted to match, so assigning a non-null value to emit metric
                    joinKeyHash = 0L;
                    if (source.getDebugJoinKey().equals(trigger.getDebugJoinKey())) {
                        sourceDebugKey = source.getDebugKey();
                        joinKeyHash = (long) source.getDebugJoinKey().hashCode();
                        doDebugJoinKeysMatch = true;
                    }
                }
                break;
            case AttributionType.SOURCE_WEB_TRIGGER_APP:
                // Gated on Trigger Adid permission.
                if (!trigger.hasAdIdPermission()) {
                    break;
                }
                triggerDebugKey = trigger.getDebugKey();
                // Send source_debug_key when condition meets.
                if (canMatchAdIdWebSourceToAppTrigger(source, trigger)
                        && canMatchAdIdEnrollments(
                                source,
                                trigger,
                                blockedEnrollmentsAdIdMatchingString,
                                blockedEnrollmentsAdIdMatching)) {
                    if (trigger.getPlatformAdId().equals(source.getDebugAdId())
                            && isEnrollmentIdWithinUniqueAdIdLimit(source.getEnrollmentId())) {
                        sourceDebugKey = source.getDebugKey();
                    }
                    // TODO(b/280322027): Record result for metrics emission.
                } else if (canMatchJoinKeys(source, trigger, allowedEnrollmentsString)) {
                    // Attempted to match, so assigning a non-null value to emit metric
                    joinKeyHash = 0L;
                    if (source.getDebugJoinKey().equals(trigger.getDebugJoinKey())) {
                        sourceDebugKey = source.getDebugKey();
                        joinKeyHash = (long) source.getDebugJoinKey().hashCode();
                        doDebugJoinKeysMatch = true;
                    }
                }
                break;
            case AttributionType.UNKNOWN:
                // fall-through
            default:
                break;
        }
        logDebugKeysMatch(
                joinKeyHash, trigger, attributionType, doDebugJoinKeysMatch, mAdServicesLogger);
        return new Pair<>(sourceDebugKey, triggerDebugKey);
    }

    private void logDebugKeysMatch(
            Long joinKeyHash,
            Trigger trigger,
            int attributionType,
            boolean doDebugJoinKeysMatch,
            AdServicesLogger mAdServicesLogger) {
        long debugKeyHashLimit = mFlags.getMeasurementDebugJoinKeyHashLimit();
        // The provided hash limit is valid and the join key was attempted to be matched.
        if (debugKeyHashLimit > 0 && joinKeyHash != null) {
            long hashedValue = joinKeyHash % debugKeyHashLimit;
            MsmtDebugKeysMatchStats stats =
                    MsmtDebugKeysMatchStats.builder()
                            .setAdTechEnrollmentId(trigger.getEnrollmentId())
                            .setAttributionType(attributionType)
                            .setMatched(doDebugJoinKeysMatch)
                            .setDebugJoinKeyHashedValue(hashedValue)
                            .setDebugJoinKeyHashLimit(debugKeyHashLimit)
                            .build();
            mAdServicesLogger.logMeasurementDebugKeysMatch(stats);
        }
    }
    private static boolean canMatchJoinKeys(
            Source source, Trigger trigger, Set<String> allowedEnrollmentsString) {
        return allowedEnrollmentsString.contains(trigger.getEnrollmentId())
                && allowedEnrollmentsString.contains(source.getEnrollmentId())
                && Objects.nonNull(source.getDebugJoinKey())
                && Objects.nonNull(trigger.getDebugJoinKey());
    }

    private boolean canMatchAdIdEnrollments(
            Source source,
            Trigger trigger,
            String blockedEnrollmentsString,
            Set<String> blockedEnrollments) {
        return !AllowLists.doesAllowListAllowAll(blockedEnrollmentsString)
                && !blockedEnrollments.contains(source.getEnrollmentId())
                && !blockedEnrollments.contains(trigger.getEnrollmentId());
    }

    private static boolean canMatchAdIdAppSourceToWebTrigger(Source source, Trigger trigger) {
        return trigger.hasArDebugPermission()
                && Objects.nonNull(source.getPlatformAdId())
                && Objects.nonNull(trigger.getDebugAdId());
    }

    private static boolean canMatchAdIdWebSourceToAppTrigger(Source source, Trigger trigger) {
        return source.hasArDebugPermission()
                && Objects.nonNull(source.getDebugAdId())
                && Objects.nonNull(trigger.getPlatformAdId());
    }

    private boolean isEnrollmentIdWithinUniqueAdIdLimit(String enrollmentId)
            throws DatastoreException {
        long numUnique = mMeasurementDao.countDistinctDebugAdIdsUsedByEnrollment(enrollmentId);
        return numUnique < mFlags.getMeasurementPlatformDebugAdIdMatchingLimit();
    }

    @AttributionType
    private static int getAttributionType(Source source, Trigger trigger) {
        boolean isSourceApp = source.getPublisherType() == EventSurfaceType.APP;
        if (trigger.getDestinationType() == EventSurfaceType.WEB) {
            // Web Conversion
            return isSourceApp
                    ? AttributionType.SOURCE_APP_TRIGGER_WEB
                    : AttributionType.SOURCE_WEB_TRIGGER_WEB;
        } else {
            // App Conversion
            return isSourceApp
                    ? AttributionType.SOURCE_APP_TRIGGER_APP
                    : AttributionType.SOURCE_WEB_TRIGGER_APP;
        }
    }
}
