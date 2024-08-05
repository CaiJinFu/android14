// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include "src/guardrail/StatsdStats.h"

#include <gtest/gtest.h>

#include <vector>

#include "src/metrics/parsing_utils/metrics_manager_util.h"
#include "statslog_statsdtest.h"
#include "tests/statsd_test_util.h"

#ifdef __ANDROID__

namespace android {
namespace os {
namespace statsd {

using std::vector;

TEST(StatsdStatsTest, TestValidConfigAdd) {
    StatsdStats stats;
    ConfigKey key(0, 12345);
    const int metricsCount = 10;
    const int conditionsCount = 20;
    const int matchersCount = 30;
    const int alertsCount = 10;
    stats.noteConfigReceived(key, metricsCount, conditionsCount, matchersCount, alertsCount, {},
                             nullopt /*valid config*/);
    vector<uint8_t> output;
    stats.dumpStats(&output, false /*reset stats*/);

    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);
    ASSERT_EQ(1, report.config_stats_size());
    const auto& configReport = report.config_stats(0);
    EXPECT_EQ(0, configReport.uid());
    EXPECT_EQ(12345, configReport.id());
    EXPECT_EQ(metricsCount, configReport.metric_count());
    EXPECT_EQ(conditionsCount, configReport.condition_count());
    EXPECT_EQ(matchersCount, configReport.matcher_count());
    EXPECT_EQ(alertsCount, configReport.alert_count());
    EXPECT_EQ(true, configReport.is_valid());
    EXPECT_FALSE(configReport.has_invalid_config_reason());
    EXPECT_FALSE(configReport.has_deletion_time_sec());
}

TEST(StatsdStatsTest, TestInvalidConfigAdd) {
    StatsdStats stats;
    ConfigKey key(0, 12345);
    const int metricsCount = 10;
    const int conditionsCount = 20;
    const int matchersCount = 30;
    const int alertsCount = 10;
    optional<InvalidConfigReason> invalidConfigReason =
            InvalidConfigReason(INVALID_CONFIG_REASON_UNKNOWN, 1);
    invalidConfigReason->stateId = 2;
    invalidConfigReason->alertId = 3;
    invalidConfigReason->alarmId = 4;
    invalidConfigReason->subscriptionId = 5;
    invalidConfigReason->matcherIds.push_back(6);
    invalidConfigReason->matcherIds.push_back(7);
    invalidConfigReason->conditionIds.push_back(8);
    invalidConfigReason->conditionIds.push_back(9);
    invalidConfigReason->conditionIds.push_back(10);
    stats.noteConfigReceived(key, metricsCount, conditionsCount, matchersCount, alertsCount, {},
                             invalidConfigReason /*bad config*/);
    vector<uint8_t> output;
    stats.dumpStats(&output, false);

    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);
    ASSERT_EQ(1, report.config_stats_size());
    const auto& configReport = report.config_stats(0);
    // The invalid config should be put into icebox with a deletion time.
    EXPECT_TRUE(configReport.has_deletion_time_sec());
    EXPECT_TRUE(configReport.has_invalid_config_reason());
    EXPECT_EQ(configReport.invalid_config_reason().reason(), INVALID_CONFIG_REASON_UNKNOWN);
    EXPECT_EQ(configReport.invalid_config_reason().metric_id(), 1);
    EXPECT_EQ(configReport.invalid_config_reason().state_id(), 2);
    EXPECT_EQ(configReport.invalid_config_reason().alert_id(), 3);
    EXPECT_EQ(configReport.invalid_config_reason().alarm_id(), 4);
    EXPECT_EQ(configReport.invalid_config_reason().subscription_id(), 5);
    EXPECT_EQ(configReport.invalid_config_reason().matcher_id_size(), 2);
    EXPECT_EQ(configReport.invalid_config_reason().matcher_id(0), 6);
    EXPECT_EQ(configReport.invalid_config_reason().matcher_id(1), 7);
    EXPECT_EQ(configReport.invalid_config_reason().condition_id_size(), 3);
    EXPECT_EQ(configReport.invalid_config_reason().condition_id(0), 8);
    EXPECT_EQ(configReport.invalid_config_reason().condition_id(1), 9);
    EXPECT_EQ(configReport.invalid_config_reason().condition_id(2), 10);
}

TEST(StatsdStatsTest, TestInvalidConfigMissingMetricId) {
    StatsdStats stats;
    ConfigKey key(0, 12345);
    const int metricsCount = 10;
    const int conditionsCount = 20;
    const int matchersCount = 30;
    const int alertsCount = 10;
    optional<InvalidConfigReason> invalidConfigReason =
            InvalidConfigReason(INVALID_CONFIG_REASON_SUBSCRIPTION_SUBSCRIBER_INFO_MISSING);
    invalidConfigReason->stateId = 1;
    invalidConfigReason->alertId = 2;
    invalidConfigReason->alarmId = 3;
    invalidConfigReason->subscriptionId = 4;
    invalidConfigReason->matcherIds.push_back(5);
    invalidConfigReason->conditionIds.push_back(6);
    invalidConfigReason->conditionIds.push_back(7);
    stats.noteConfigReceived(key, metricsCount, conditionsCount, matchersCount, alertsCount, {},
                             invalidConfigReason /*bad config*/);
    vector<uint8_t> output;
    stats.dumpStats(&output, false);

    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);
    ASSERT_EQ(1, report.config_stats_size());
    const auto& configReport = report.config_stats(0);
    // The invalid config should be put into icebox with a deletion time.
    EXPECT_TRUE(configReport.has_deletion_time_sec());
    EXPECT_TRUE(configReport.has_invalid_config_reason());
    EXPECT_EQ(configReport.invalid_config_reason().reason(),
              INVALID_CONFIG_REASON_SUBSCRIPTION_SUBSCRIBER_INFO_MISSING);
    EXPECT_FALSE(configReport.invalid_config_reason().has_metric_id());
    EXPECT_EQ(configReport.invalid_config_reason().state_id(), 1);
    EXPECT_EQ(configReport.invalid_config_reason().alert_id(), 2);
    EXPECT_EQ(configReport.invalid_config_reason().alarm_id(), 3);
    EXPECT_EQ(configReport.invalid_config_reason().subscription_id(), 4);
    EXPECT_EQ(configReport.invalid_config_reason().matcher_id_size(), 1);
    EXPECT_EQ(configReport.invalid_config_reason().matcher_id(0), 5);
    EXPECT_EQ(configReport.invalid_config_reason().condition_id_size(), 2);
    EXPECT_EQ(configReport.invalid_config_reason().condition_id(0), 6);
    EXPECT_EQ(configReport.invalid_config_reason().condition_id(1), 7);
}

TEST(StatsdStatsTest, TestInvalidConfigOnlyMetricId) {
    StatsdStats stats;
    ConfigKey key(0, 12345);
    const int metricsCount = 10;
    const int conditionsCount = 20;
    const int matchersCount = 30;
    const int alertsCount = 10;
    optional<InvalidConfigReason> invalidConfigReason =
            InvalidConfigReason(INVALID_CONFIG_REASON_METRIC_NOT_IN_PREV_CONFIG, 1);
    stats.noteConfigReceived(key, metricsCount, conditionsCount, matchersCount, alertsCount, {},
                             invalidConfigReason /*bad config*/);
    vector<uint8_t> output;
    stats.dumpStats(&output, false);

    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);
    ASSERT_EQ(1, report.config_stats_size());
    const auto& configReport = report.config_stats(0);
    // The invalid config should be put into icebox with a deletion time.
    EXPECT_TRUE(configReport.has_deletion_time_sec());
    EXPECT_TRUE(configReport.has_invalid_config_reason());
    EXPECT_EQ(configReport.invalid_config_reason().reason(),
              INVALID_CONFIG_REASON_METRIC_NOT_IN_PREV_CONFIG);
    EXPECT_EQ(configReport.invalid_config_reason().metric_id(), 1);
    EXPECT_FALSE(configReport.invalid_config_reason().has_state_id());
    EXPECT_FALSE(configReport.invalid_config_reason().has_alert_id());
    EXPECT_FALSE(configReport.invalid_config_reason().has_alarm_id());
    EXPECT_FALSE(configReport.invalid_config_reason().has_subscription_id());
    EXPECT_EQ(configReport.invalid_config_reason().matcher_id_size(), 0);
    EXPECT_EQ(configReport.invalid_config_reason().condition_id_size(), 0);
}

TEST(StatsdStatsTest, TestConfigRemove) {
    StatsdStats stats;
    ConfigKey key(0, 12345);
    const int metricsCount = 10;
    const int conditionsCount = 20;
    const int matchersCount = 30;
    const int alertsCount = 10;
    stats.noteConfigReceived(key, metricsCount, conditionsCount, matchersCount, alertsCount, {},
                             nullopt);
    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);
    ASSERT_EQ(1, report.config_stats_size());
    const auto& configReport = report.config_stats(0);
    EXPECT_FALSE(configReport.has_deletion_time_sec());

    stats.noteConfigRemoved(key);
    stats.dumpStats(&output, false);
    good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);
    ASSERT_EQ(1, report.config_stats_size());
    const auto& configReport2 = report.config_stats(0);
    EXPECT_TRUE(configReport2.has_deletion_time_sec());
}

TEST(StatsdStatsTest, TestSubStats) {
    StatsdStats stats;
    ConfigKey key(0, 12345);
    stats.noteConfigReceived(key, 2, 3, 4, 5, {std::make_pair(123, 456)}, nullopt);

    stats.noteMatcherMatched(key, StringToId("matcher1"));
    stats.noteMatcherMatched(key, StringToId("matcher1"));
    stats.noteMatcherMatched(key, StringToId("matcher2"));

    stats.noteConditionDimensionSize(key, StringToId("condition1"), 250);
    stats.noteConditionDimensionSize(key, StringToId("condition1"), 240);

    stats.noteMetricDimensionSize(key, StringToId("metric1"), 201);
    stats.noteMetricDimensionSize(key, StringToId("metric1"), 202);

    stats.noteAnomalyDeclared(key, StringToId("alert1"));
    stats.noteAnomalyDeclared(key, StringToId("alert1"));
    stats.noteAnomalyDeclared(key, StringToId("alert2"));

    // broadcast-> 2
    stats.noteBroadcastSent(key);
    stats.noteBroadcastSent(key);

    // data drop -> 1
    stats.noteDataDropped(key, 123);

    // dump report -> 3
    stats.noteMetricsReportSent(key, 0);
    stats.noteMetricsReportSent(key, 0);
    stats.noteMetricsReportSent(key, 0);

    // activation_time_sec -> 2
    stats.noteActiveStatusChanged(key, true);
    stats.noteActiveStatusChanged(key, true);

    // deactivation_time_sec -> 1
    stats.noteActiveStatusChanged(key, false);

    vector<uint8_t> output;
    stats.dumpStats(&output, true);  // Dump and reset stats
    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);
    ASSERT_EQ(1, report.config_stats_size());
    const auto& configReport = report.config_stats(0);
    ASSERT_EQ(2, configReport.broadcast_sent_time_sec_size());
    ASSERT_EQ(1, configReport.data_drop_time_sec_size());
    ASSERT_EQ(1, configReport.data_drop_bytes_size());
    EXPECT_EQ(123, configReport.data_drop_bytes(0));
    ASSERT_EQ(3, configReport.dump_report_time_sec_size());
    ASSERT_EQ(3, configReport.dump_report_data_size_size());
    ASSERT_EQ(2, configReport.activation_time_sec_size());
    ASSERT_EQ(1, configReport.deactivation_time_sec_size());
    ASSERT_EQ(1, configReport.annotation_size());
    EXPECT_EQ(123, configReport.annotation(0).field_int64());
    EXPECT_EQ(456, configReport.annotation(0).field_int32());

    ASSERT_EQ(2, configReport.matcher_stats_size());
    // matcher1 is the first in the list
    if (configReport.matcher_stats(0).id() == StringToId("matcher1")) {
        EXPECT_EQ(2, configReport.matcher_stats(0).matched_times());
        EXPECT_EQ(1, configReport.matcher_stats(1).matched_times());
        EXPECT_EQ(StringToId("matcher2"), configReport.matcher_stats(1).id());
    } else {
        // matcher1 is the second in the list.
        EXPECT_EQ(1, configReport.matcher_stats(0).matched_times());
        EXPECT_EQ(StringToId("matcher2"), configReport.matcher_stats(0).id());

        EXPECT_EQ(2, configReport.matcher_stats(1).matched_times());
        EXPECT_EQ(StringToId("matcher1"), configReport.matcher_stats(1).id());
    }

    ASSERT_EQ(2, configReport.alert_stats_size());
    bool alert1first = configReport.alert_stats(0).id() == StringToId("alert1");
    EXPECT_EQ(StringToId("alert1"), configReport.alert_stats(alert1first ? 0 : 1).id());
    EXPECT_EQ(2, configReport.alert_stats(alert1first ? 0 : 1).alerted_times());
    EXPECT_EQ(StringToId("alert2"), configReport.alert_stats(alert1first ? 1 : 0).id());
    EXPECT_EQ(1, configReport.alert_stats(alert1first ? 1 : 0).alerted_times());

    ASSERT_EQ(1, configReport.condition_stats_size());
    EXPECT_EQ(StringToId("condition1"), configReport.condition_stats(0).id());
    EXPECT_EQ(250, configReport.condition_stats(0).max_tuple_counts());

    ASSERT_EQ(1, configReport.metric_stats_size());
    EXPECT_EQ(StringToId("metric1"), configReport.metric_stats(0).id());
    EXPECT_EQ(202, configReport.metric_stats(0).max_tuple_counts());

    // after resetting the stats, some new events come
    stats.noteMatcherMatched(key, StringToId("matcher99"));
    stats.noteConditionDimensionSize(key, StringToId("condition99"), 300);
    stats.noteMetricDimensionSize(key, StringToId("metric99tion99"), 270);
    stats.noteAnomalyDeclared(key, StringToId("alert99"));

    // now the config stats should only contain the stats about the new event.
    stats.dumpStats(&output, false);
    good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);
    ASSERT_EQ(1, report.config_stats_size());
    const auto& configReport2 = report.config_stats(0);
    ASSERT_EQ(1, configReport2.matcher_stats_size());
    EXPECT_EQ(StringToId("matcher99"), configReport2.matcher_stats(0).id());
    EXPECT_EQ(1, configReport2.matcher_stats(0).matched_times());

    ASSERT_EQ(1, configReport2.condition_stats_size());
    EXPECT_EQ(StringToId("condition99"), configReport2.condition_stats(0).id());
    EXPECT_EQ(300, configReport2.condition_stats(0).max_tuple_counts());

    ASSERT_EQ(1, configReport2.metric_stats_size());
    EXPECT_EQ(StringToId("metric99tion99"), configReport2.metric_stats(0).id());
    EXPECT_EQ(270, configReport2.metric_stats(0).max_tuple_counts());

    ASSERT_EQ(1, configReport2.alert_stats_size());
    EXPECT_EQ(StringToId("alert99"), configReport2.alert_stats(0).id());
    EXPECT_EQ(1, configReport2.alert_stats(0).alerted_times());
}

TEST(StatsdStatsTest, TestAtomLog) {
    StatsdStats stats;
    time_t now = time(nullptr);
    // old event, we get it from the stats buffer. should be ignored.
    stats.noteAtomLogged(util::SENSOR_STATE_CHANGED, 1000, false);

    stats.noteAtomLogged(util::SENSOR_STATE_CHANGED, now + 1, false);
    stats.noteAtomLogged(util::SENSOR_STATE_CHANGED, now + 2, false);
    stats.noteAtomLogged(util::APP_CRASH_OCCURRED, now + 3, false);

    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);

    ASSERT_EQ(2, report.atom_stats_size());
    bool sensorAtomGood = false;
    bool dropboxAtomGood = false;

    for (const auto& atomStats : report.atom_stats()) {
        if (atomStats.tag() == util::SENSOR_STATE_CHANGED && atomStats.count() == 3) {
            sensorAtomGood = true;
        }
        if (atomStats.tag() == util::APP_CRASH_OCCURRED && atomStats.count() == 1) {
            dropboxAtomGood = true;
        }
        EXPECT_FALSE(atomStats.has_dropped_count());
        EXPECT_FALSE(atomStats.has_skip_count());
    }

    EXPECT_TRUE(dropboxAtomGood);
    EXPECT_TRUE(sensorAtomGood);
}

TEST(StatsdStatsTest, TestNonPlatformAtomLog) {
    StatsdStats stats;
    time_t now = time(nullptr);
    int newAtom1 = StatsdStats::kMaxPushedAtomId + 1;
    int newAtom2 = StatsdStats::kMaxPushedAtomId + 2;

    stats.noteAtomLogged(newAtom1, now + 1, false);
    stats.noteAtomLogged(newAtom1, now + 2, false);
    stats.noteAtomLogged(newAtom2, now + 3, false);

    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);

    ASSERT_EQ(2, report.atom_stats_size());
    bool newAtom1Good = false;
    bool newAtom2Good = false;

    for (const auto& atomStats : report.atom_stats()) {
        if (atomStats.tag() == newAtom1 && atomStats.count() == 2) {
            newAtom1Good = true;
        }
        if (atomStats.tag() == newAtom2 && atomStats.count() == 1) {
            newAtom2Good = true;
        }
        EXPECT_FALSE(atomStats.has_dropped_count());
        EXPECT_FALSE(atomStats.has_skip_count());
    }

    EXPECT_TRUE(newAtom1Good);
    EXPECT_TRUE(newAtom2Good);
}

TEST(StatsdStatsTest, TestPullAtomStats) {
    StatsdStats stats;

    stats.updateMinPullIntervalSec(util::DISK_SPACE, 3333L);
    stats.updateMinPullIntervalSec(util::DISK_SPACE, 2222L);
    stats.updateMinPullIntervalSec(util::DISK_SPACE, 4444L);

    stats.notePull(util::DISK_SPACE);
    stats.notePullTime(util::DISK_SPACE, 1111L);
    stats.notePullDelay(util::DISK_SPACE, 1111L);
    stats.notePull(util::DISK_SPACE);
    stats.notePullTime(util::DISK_SPACE, 3333L);
    stats.notePullDelay(util::DISK_SPACE, 3335L);
    stats.notePull(util::DISK_SPACE);
    stats.notePullFromCache(util::DISK_SPACE);
    stats.notePullerCallbackRegistrationChanged(util::DISK_SPACE, true);
    stats.notePullerCallbackRegistrationChanged(util::DISK_SPACE, false);
    stats.notePullerCallbackRegistrationChanged(util::DISK_SPACE, true);
    stats.notePullBinderCallFailed(util::DISK_SPACE);
    stats.notePullUidProviderNotFound(util::DISK_SPACE);
    stats.notePullerNotFound(util::DISK_SPACE);
    stats.notePullerNotFound(util::DISK_SPACE);
    stats.notePullTimeout(util::DISK_SPACE, 3000L, 6000L);
    stats.notePullTimeout(util::DISK_SPACE, 4000L, 7000L);

    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);

    ASSERT_EQ(1, report.pulled_atom_stats_size());

    EXPECT_EQ(util::DISK_SPACE, report.pulled_atom_stats(0).atom_id());
    EXPECT_EQ(3, report.pulled_atom_stats(0).total_pull());
    EXPECT_EQ(1, report.pulled_atom_stats(0).total_pull_from_cache());
    EXPECT_EQ(2222L, report.pulled_atom_stats(0).min_pull_interval_sec());
    EXPECT_EQ(2222L, report.pulled_atom_stats(0).average_pull_time_nanos());
    EXPECT_EQ(3333L, report.pulled_atom_stats(0).max_pull_time_nanos());
    EXPECT_EQ(2223L, report.pulled_atom_stats(0).average_pull_delay_nanos());
    EXPECT_EQ(3335L, report.pulled_atom_stats(0).max_pull_delay_nanos());
    EXPECT_EQ(2L, report.pulled_atom_stats(0).registered_count());
    EXPECT_EQ(1L, report.pulled_atom_stats(0).unregistered_count());
    EXPECT_EQ(1L, report.pulled_atom_stats(0).binder_call_failed());
    EXPECT_EQ(1L, report.pulled_atom_stats(0).failed_uid_provider_not_found());
    EXPECT_EQ(2L, report.pulled_atom_stats(0).puller_not_found());
    ASSERT_EQ(2, report.pulled_atom_stats(0).pull_atom_metadata_size());
    EXPECT_EQ(3000L, report.pulled_atom_stats(0).pull_atom_metadata(0).pull_timeout_uptime_millis());
    EXPECT_EQ(4000L, report.pulled_atom_stats(0).pull_atom_metadata(1).pull_timeout_uptime_millis());
    EXPECT_EQ(6000L, report.pulled_atom_stats(0).pull_atom_metadata(0)
            .pull_timeout_elapsed_millis());
    EXPECT_EQ(7000L, report.pulled_atom_stats(0).pull_atom_metadata(1)
            .pull_timeout_elapsed_millis());
}

TEST(StatsdStatsTest, TestAtomMetricsStats) {
    StatsdStats stats;
    time_t now = time(nullptr);
    // old event, we get it from the stats buffer. should be ignored.
    stats.noteBucketDropped(10000000000LL);

    stats.noteBucketBoundaryDelayNs(10000000000LL, -1L);
    stats.noteBucketBoundaryDelayNs(10000000000LL, -10L);
    stats.noteBucketBoundaryDelayNs(10000000000LL, 2L);

    stats.noteBucketBoundaryDelayNs(10000000001LL, 1L);

    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);

    ASSERT_EQ(2, report.atom_metric_stats().size());

    auto atomStats = report.atom_metric_stats(0);
    EXPECT_EQ(10000000000LL, atomStats.metric_id());
    EXPECT_EQ(1L, atomStats.bucket_dropped());
    EXPECT_EQ(-10L, atomStats.min_bucket_boundary_delay_ns());
    EXPECT_EQ(2L, atomStats.max_bucket_boundary_delay_ns());

    auto atomStats2 = report.atom_metric_stats(1);
    EXPECT_EQ(10000000001LL, atomStats2.metric_id());
    EXPECT_EQ(0L, atomStats2.bucket_dropped());
    EXPECT_EQ(0L, atomStats2.min_bucket_boundary_delay_ns());
    EXPECT_EQ(1L, atomStats2.max_bucket_boundary_delay_ns());
}

TEST(StatsdStatsTest, TestRestrictedMetricsStats) {
    StatsdStats stats;
    const int64_t metricId = -1234556L;
    ConfigKey key(0, 12345);
    stats.noteConfigReceived(key, 2, 3, 4, 5, {}, nullopt);
    stats.noteRestrictedMetricInsertError(key, metricId);
    stats.noteRestrictedMetricTableCreationError(key, metricId);
    stats.noteRestrictedMetricTableDeletionError(key, metricId);
    stats.noteDeviceInfoTableCreationFailed(key);
    stats.noteRestrictedMetricFlushLatency(key, metricId, 3000);
    stats.noteRestrictedMetricFlushLatency(key, metricId, 3001);
    stats.noteRestrictedMetricCategoryChanged(key, metricId);
    stats.noteRestrictedConfigFlushLatency(key, 4000);
    ConfigKey configKeyWithoutError(0, 666);
    stats.noteConfigReceived(configKeyWithoutError, 2, 3, 4, 5, {}, nullopt);
    stats.noteDbCorrupted(key);
    stats.noteDbCorrupted(key);
    stats.noteRestrictedConfigDbSize(key, 999, 111);

    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);

    ASSERT_EQ(2, report.config_stats().size());
    ASSERT_EQ(0, report.config_stats(0).restricted_metric_stats().size());
    ASSERT_EQ(1, report.config_stats(1).restricted_metric_stats().size());
    EXPECT_EQ(1, report.config_stats(1).restricted_metric_stats(0).insert_error());
    EXPECT_EQ(1, report.config_stats(1).restricted_metric_stats(0).table_creation_error());
    EXPECT_EQ(1, report.config_stats(1).restricted_metric_stats(0).table_deletion_error());
    EXPECT_EQ(1, report.config_stats(1).restricted_metric_stats(0).category_changed_count());
    ASSERT_EQ(2, report.config_stats(1).restricted_metric_stats(0).flush_latency_ns().size());
    EXPECT_EQ(3000, report.config_stats(1).restricted_metric_stats(0).flush_latency_ns(0));
    EXPECT_EQ(3001, report.config_stats(1).restricted_metric_stats(0).flush_latency_ns(1));
    ASSERT_EQ(1, report.config_stats(1).restricted_db_size_time_sec().size());
    EXPECT_EQ(999, report.config_stats(1).restricted_db_size_time_sec(0));
    ASSERT_EQ(1, report.config_stats(1).restricted_db_size_bytes().size());
    EXPECT_EQ(111, report.config_stats(1).restricted_db_size_bytes(0));
    ASSERT_EQ(1, report.config_stats(1).restricted_flush_latency().size());
    EXPECT_EQ(4000, report.config_stats(1).restricted_flush_latency(0));
    EXPECT_TRUE(report.config_stats(1).device_info_table_creation_failed());
    EXPECT_EQ(metricId, report.config_stats(1).restricted_metric_stats(0).restricted_metric_id());
    EXPECT_EQ(2, report.config_stats(1).restricted_db_corrupted_count());
}

TEST(StatsdStatsTest, TestRestrictedMetricsQueryStats) {
    StatsdStats stats;
    const int32_t callingUid = 100;
    ConfigKey configKey(0, 12345);
    const string configPackage = "com.google.android.gm";
    int64_t beforeNoteMetricSucceed = getWallClockNs();
    stats.noteQueryRestrictedMetricSucceed(configKey.GetId(), configPackage, configKey.GetUid(),
                                           callingUid, /*queryLatencyNs=*/5 * NS_PER_SEC);
    int64_t afterNoteMetricSucceed = getWallClockNs();

    const int64_t configIdWithError = 111;
    stats.noteQueryRestrictedMetricFailed(configIdWithError, configPackage, std::nullopt,
                                          callingUid, InvalidQueryReason(AMBIGUOUS_CONFIG_KEY));
    stats.noteQueryRestrictedMetricFailed(configIdWithError, configPackage, std::nullopt,
                                          callingUid, InvalidQueryReason(AMBIGUOUS_CONFIG_KEY),
                                          "error_message");

    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);

    ASSERT_EQ(3, report.restricted_metric_query_stats().size());
    EXPECT_EQ(configKey.GetId(), report.restricted_metric_query_stats(0).config_id());
    EXPECT_EQ(configKey.GetUid(), report.restricted_metric_query_stats(0).config_uid());
    EXPECT_EQ(callingUid, report.restricted_metric_query_stats(0).calling_uid());
    EXPECT_EQ(configPackage, report.restricted_metric_query_stats(0).config_package());
    EXPECT_FALSE(report.restricted_metric_query_stats(0).has_query_error());
    EXPECT_LT(beforeNoteMetricSucceed,
              report.restricted_metric_query_stats(0).query_wall_time_ns());
    EXPECT_GT(afterNoteMetricSucceed, report.restricted_metric_query_stats(0).query_wall_time_ns());
    EXPECT_EQ(5 * NS_PER_SEC, report.restricted_metric_query_stats(0).query_latency_ns());
    EXPECT_EQ(configIdWithError, report.restricted_metric_query_stats(1).config_id());
    EXPECT_EQ(AMBIGUOUS_CONFIG_KEY, report.restricted_metric_query_stats(1).invalid_query_reason());
    EXPECT_EQ(false, report.restricted_metric_query_stats(1).has_config_uid());
    EXPECT_FALSE(report.restricted_metric_query_stats(1).has_query_error());
    EXPECT_FALSE(report.restricted_metric_query_stats(1).has_query_latency_ns());
    EXPECT_EQ("error_message", report.restricted_metric_query_stats(2).query_error());
    EXPECT_FALSE(report.restricted_metric_query_stats(2).has_query_latency_ns());
    EXPECT_NE(report.restricted_metric_query_stats(1).query_wall_time_ns(),
              report.restricted_metric_query_stats(0).query_wall_time_ns());
}

TEST(StatsdStatsTest, TestAnomalyMonitor) {
    StatsdStats stats;
    stats.noteRegisteredAnomalyAlarmChanged();
    stats.noteRegisteredAnomalyAlarmChanged();

    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    bool good = report.ParseFromArray(&output[0], output.size());
    EXPECT_TRUE(good);

    EXPECT_EQ(2, report.anomaly_alarm_stats().alarms_registered());
}

TEST(StatsdStatsTest, TestTimestampThreshold) {
    StatsdStats stats;
    vector<int32_t> timestamps;
    for (int i = 0; i < StatsdStats::kMaxTimestampCount; i++) {
        timestamps.push_back(i);
    }
    ConfigKey key(0, 12345);
    stats.noteConfigReceived(key, 2, 3, 4, 5, {}, nullopt);

    for (int i = 0; i < StatsdStats::kMaxTimestampCount; i++) {
        stats.noteDataDropped(key, timestamps[i]);
        stats.noteBroadcastSent(key, timestamps[i]);
        stats.noteMetricsReportSent(key, 0, timestamps[i]);
        stats.noteActiveStatusChanged(key, true, timestamps[i]);
        stats.noteActiveStatusChanged(key, false, timestamps[i]);
    }

    int32_t newTimestamp = 10000;

    // now it should trigger removing oldest timestamp
    stats.noteDataDropped(key, 123, 10000);
    stats.noteBroadcastSent(key, 10000);
    stats.noteMetricsReportSent(key, 0, 10000);
    stats.noteActiveStatusChanged(key, true, 10000);
    stats.noteActiveStatusChanged(key, false, 10000);

    EXPECT_TRUE(stats.mConfigStats.find(key) != stats.mConfigStats.end());
    const auto& configStats = stats.mConfigStats[key];

    size_t maxCount = StatsdStats::kMaxTimestampCount;
    ASSERT_EQ(maxCount, configStats->broadcast_sent_time_sec.size());
    ASSERT_EQ(maxCount, configStats->data_drop_time_sec.size());
    ASSERT_EQ(maxCount, configStats->dump_report_stats.size());
    ASSERT_EQ(maxCount, configStats->activation_time_sec.size());
    ASSERT_EQ(maxCount, configStats->deactivation_time_sec.size());

    // the oldest timestamp is the second timestamp in history
    EXPECT_EQ(1, configStats->broadcast_sent_time_sec.front());
    EXPECT_EQ(1, configStats->data_drop_bytes.front());
    EXPECT_EQ(1, configStats->dump_report_stats.front().first);
    EXPECT_EQ(1, configStats->activation_time_sec.front());
    EXPECT_EQ(1, configStats->deactivation_time_sec.front());

    // the last timestamp is the newest timestamp.
    EXPECT_EQ(newTimestamp, configStats->broadcast_sent_time_sec.back());
    EXPECT_EQ(newTimestamp, configStats->data_drop_time_sec.back());
    EXPECT_EQ(123, configStats->data_drop_bytes.back());
    EXPECT_EQ(newTimestamp, configStats->dump_report_stats.back().first);
    EXPECT_EQ(newTimestamp, configStats->activation_time_sec.back());
    EXPECT_EQ(newTimestamp, configStats->deactivation_time_sec.back());
}

TEST(StatsdStatsTest, TestSystemServerCrash) {
    StatsdStats stats;
    vector<int32_t> timestamps;
    for (int i = 0; i < StatsdStats::kMaxSystemServerRestarts; i++) {
        timestamps.push_back(i);
        stats.noteSystemServerRestart(timestamps[i]);
    }
    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    EXPECT_TRUE(report.ParseFromArray(&output[0], output.size()));
    const int maxCount = StatsdStats::kMaxSystemServerRestarts;
    ASSERT_EQ(maxCount, (int)report.system_restart_sec_size());

    stats.noteSystemServerRestart(StatsdStats::kMaxSystemServerRestarts + 1);
    output.clear();
    stats.dumpStats(&output, false);
    EXPECT_TRUE(report.ParseFromArray(&output[0], output.size()));
    ASSERT_EQ(maxCount, (int)report.system_restart_sec_size());
    EXPECT_EQ(StatsdStats::kMaxSystemServerRestarts + 1, report.system_restart_sec(maxCount - 1));
}

TEST(StatsdStatsTest, TestActivationBroadcastGuardrailHit) {
    StatsdStats stats;
    int uid1 = 1;
    int uid2 = 2;
    stats.noteActivationBroadcastGuardrailHit(uid1, 10);
    stats.noteActivationBroadcastGuardrailHit(uid1, 20);

    // Test that we only keep 20 timestamps.
    for (int i = 0; i < 100; i++) {
        stats.noteActivationBroadcastGuardrailHit(uid2, i);
    }

    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    EXPECT_TRUE(report.ParseFromArray(&output[0], output.size()));

    ASSERT_EQ(2, report.activation_guardrail_stats_size());
    bool uid1Good = false;
    bool uid2Good = false;
    for (const auto& guardrailTimes : report.activation_guardrail_stats()) {
        if (uid1 == guardrailTimes.uid()) {
            uid1Good = true;
            ASSERT_EQ(2, guardrailTimes.guardrail_met_sec_size());
            EXPECT_EQ(10, guardrailTimes.guardrail_met_sec(0));
            EXPECT_EQ(20, guardrailTimes.guardrail_met_sec(1));
        } else if (uid2 == guardrailTimes.uid()) {
            int maxCount = StatsdStats::kMaxTimestampCount;
            uid2Good = true;
            ASSERT_EQ(maxCount, guardrailTimes.guardrail_met_sec_size());
            for (int i = 0; i < maxCount; i++) {
                EXPECT_EQ(100 - maxCount + i, guardrailTimes.guardrail_met_sec(i));
            }
        } else {
            FAIL() << "Unexpected uid.";
        }
    }
    EXPECT_TRUE(uid1Good);
    EXPECT_TRUE(uid2Good);
}

TEST(StatsdStatsTest, TestAtomErrorStats) {
    StatsdStats stats;

    int pushAtomTag = 100;
    int pullAtomTag = 1000;
    int numErrors = 10;

    for (int i = 0; i < numErrors; i++) {
        // We must call noteAtomLogged as well because only those pushed atoms
        // that have been logged will have stats printed about them in the
        // proto.
        stats.noteAtomLogged(pushAtomTag, /*timeSec=*/0, false);
        stats.noteAtomError(pushAtomTag, /*pull=*/false);

        stats.noteAtomError(pullAtomTag, /*pull=*/true);
    }

    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    EXPECT_TRUE(report.ParseFromArray(&output[0], output.size()));

    // Check error count = numErrors for push atom
    ASSERT_EQ(1, report.atom_stats_size());
    const auto& pushedAtomStats = report.atom_stats(0);
    EXPECT_EQ(pushAtomTag, pushedAtomStats.tag());
    EXPECT_EQ(numErrors, pushedAtomStats.error_count());
    EXPECT_FALSE(pushedAtomStats.has_dropped_count());
    EXPECT_FALSE(pushedAtomStats.has_skip_count());

    // Check error count = numErrors for pull atom
    ASSERT_EQ(1, report.pulled_atom_stats_size());
    const auto& pulledAtomStats = report.pulled_atom_stats(0);
    EXPECT_EQ(pullAtomTag, pulledAtomStats.atom_id());
    EXPECT_EQ(numErrors, pulledAtomStats.atom_error_count());
}

TEST(StatsdStatsTest, TestAtomDroppedStats) {
    StatsdStats stats;

    const int pushAtomTag = 100;
    const int nonPlatformPushAtomTag = StatsdStats::kMaxPushedAtomId + 100;

    const int numDropped = 10;
    for (int i = 0; i < numDropped; i++) {
        stats.noteEventQueueOverflow(/*oldestEventTimestampNs*/ 0, pushAtomTag, false);
        stats.noteEventQueueOverflow(/*oldestEventTimestampNs*/ 0, nonPlatformPushAtomTag, false);
    }

    vector<uint8_t> output;
    stats.dumpStats(&output, true);
    ASSERT_EQ(0, stats.mPushedAtomDropsStats.size());

    StatsdStatsReport report;
    EXPECT_TRUE(report.ParseFromArray(&output[0], output.size()));

    // Check dropped_count = numDropped for push atoms
    ASSERT_EQ(2, report.atom_stats_size());

    const auto& pushedAtomStats = report.atom_stats(0);
    EXPECT_EQ(pushAtomTag, pushedAtomStats.tag());
    EXPECT_EQ(numDropped, pushedAtomStats.count());
    EXPECT_EQ(numDropped, pushedAtomStats.dropped_count());
    EXPECT_FALSE(pushedAtomStats.has_error_count());
    EXPECT_FALSE(pushedAtomStats.has_skip_count());

    const auto& nonPlatformPushedAtomStats = report.atom_stats(1);
    EXPECT_EQ(nonPlatformPushAtomTag, nonPlatformPushedAtomStats.tag());
    EXPECT_EQ(numDropped, nonPlatformPushedAtomStats.count());
    EXPECT_EQ(numDropped, nonPlatformPushedAtomStats.dropped_count());
    EXPECT_FALSE(nonPlatformPushedAtomStats.has_error_count());
    EXPECT_FALSE(nonPlatformPushedAtomStats.has_skip_count());
}

TEST(StatsdStatsTest, TestAtomLoggedAndDroppedStats) {
    StatsdStats stats;

    const int pushAtomTag = 100;
    const int nonPlatformPushAtomTag = StatsdStats::kMaxPushedAtomId + 100;

    const int numLogged = 10;
    for (int i = 0; i < numLogged; i++) {
        stats.noteAtomLogged(pushAtomTag, /*timeSec*/ 0, false);
        stats.noteAtomLogged(nonPlatformPushAtomTag, /*timeSec*/ 0, false);
    }

    const int numDropped = 10;
    for (int i = 0; i < numDropped; i++) {
        stats.noteEventQueueOverflow(/*oldestEventTimestampNs*/ 0, pushAtomTag, false);
        stats.noteEventQueueOverflow(/*oldestEventTimestampNs*/ 0, nonPlatformPushAtomTag, false);
    }

    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    EXPECT_TRUE(report.ParseFromArray(&output[0], output.size()));

    // Check dropped_count = numDropped for push atoms
    ASSERT_EQ(2, report.atom_stats_size());

    const auto& pushedAtomStats = report.atom_stats(0);
    EXPECT_EQ(pushAtomTag, pushedAtomStats.tag());
    EXPECT_EQ(numLogged + numDropped, pushedAtomStats.count());
    EXPECT_EQ(numDropped, pushedAtomStats.dropped_count());
    EXPECT_FALSE(pushedAtomStats.has_error_count());
    EXPECT_FALSE(pushedAtomStats.has_skip_count());

    const auto& nonPlatformPushedAtomStats = report.atom_stats(1);
    EXPECT_EQ(nonPlatformPushAtomTag, nonPlatformPushedAtomStats.tag());
    EXPECT_EQ(numLogged + numDropped, nonPlatformPushedAtomStats.count());
    EXPECT_EQ(numDropped, nonPlatformPushedAtomStats.dropped_count());
    EXPECT_FALSE(nonPlatformPushedAtomStats.has_error_count());
    EXPECT_FALSE(nonPlatformPushedAtomStats.has_skip_count());
}

TEST(StatsdStatsTest, TestAtomSkippedStats) {
    StatsdStats stats;

    const int pushAtomTag = 100;
    const int nonPlatformPushAtomTag = StatsdStats::kMaxPushedAtomId + 100;
    const int numSkipped = 10;

    for (int i = 0; i < numSkipped; i++) {
        stats.noteAtomLogged(pushAtomTag, /*timeSec=*/0, /*isSkipped*/ true);
        stats.noteAtomLogged(nonPlatformPushAtomTag, /*timeSec=*/0, /*isSkipped*/ true);
    }

    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    EXPECT_TRUE(report.ParseFromArray(&output[0], output.size()));

    // Check skip_count = numSkipped for push atoms
    ASSERT_EQ(2, report.atom_stats_size());

    const auto& pushedAtomStats = report.atom_stats(0);
    EXPECT_EQ(pushAtomTag, pushedAtomStats.tag());
    EXPECT_EQ(numSkipped, pushedAtomStats.count());
    EXPECT_EQ(numSkipped, pushedAtomStats.skip_count());
    EXPECT_FALSE(pushedAtomStats.has_error_count());

    const auto& nonPlatformPushedAtomStats = report.atom_stats(1);
    EXPECT_EQ(nonPlatformPushAtomTag, nonPlatformPushedAtomStats.tag());
    EXPECT_EQ(numSkipped, nonPlatformPushedAtomStats.count());
    EXPECT_EQ(numSkipped, nonPlatformPushedAtomStats.skip_count());
    EXPECT_FALSE(nonPlatformPushedAtomStats.has_error_count());
}

TEST(StatsdStatsTest, TestAtomLoggedAndDroppedAndSkippedStats) {
    StatsdStats stats;

    const int pushAtomTag = 100;
    const int nonPlatformPushAtomTag = StatsdStats::kMaxPushedAtomId + 100;

    const int numLogged = 10;
    for (int i = 0; i < numLogged; i++) {
        stats.noteAtomLogged(pushAtomTag, /*timeSec*/ 0, false);
        stats.noteAtomLogged(nonPlatformPushAtomTag, /*timeSec*/ 0, false);
    }

    const int numDropped = 10;
    for (int i = 0; i < numDropped; i++) {
        stats.noteEventQueueOverflow(/*oldestEventTimestampNs*/ 0, pushAtomTag, true);
        stats.noteEventQueueOverflow(/*oldestEventTimestampNs*/ 0, nonPlatformPushAtomTag, true);
    }

    vector<uint8_t> output;
    stats.dumpStats(&output, false);
    StatsdStatsReport report;
    EXPECT_TRUE(report.ParseFromArray(&output[0], output.size()));

    // Check dropped_count = numDropped for push atoms
    ASSERT_EQ(2, report.atom_stats_size());

    const auto& pushedAtomStats = report.atom_stats(0);
    EXPECT_EQ(pushAtomTag, pushedAtomStats.tag());
    EXPECT_EQ(numLogged + numDropped, pushedAtomStats.count());
    EXPECT_EQ(numDropped, pushedAtomStats.dropped_count());
    EXPECT_EQ(numDropped, pushedAtomStats.skip_count());
    EXPECT_FALSE(pushedAtomStats.has_error_count());

    const auto& nonPlatformPushedAtomStats = report.atom_stats(1);
    EXPECT_EQ(nonPlatformPushAtomTag, nonPlatformPushedAtomStats.tag());
    EXPECT_EQ(numLogged + numDropped, nonPlatformPushedAtomStats.count());
    EXPECT_EQ(numDropped, nonPlatformPushedAtomStats.dropped_count());
    EXPECT_EQ(numDropped, nonPlatformPushedAtomStats.skip_count());
    EXPECT_FALSE(nonPlatformPushedAtomStats.has_error_count());
}

TEST(StatsdStatsTest, TestShardOffsetProvider) {
    StatsdStats stats;
    ShardOffsetProvider::getInstance().setShardOffset(15);
    vector<uint8_t> output;
    stats.dumpStats(&output, false);

    StatsdStatsReport report;
    EXPECT_TRUE(report.ParseFromArray(&output[0], output.size()));
    EXPECT_EQ(report.shard_offset(), 15);
}

}  // namespace statsd
}  // namespace os
}  // namespace android
#else
GTEST_LOG_(INFO) << "This test does nothing.\n";
#endif
