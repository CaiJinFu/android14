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

package com.android.adservices.service.measurement.noising;

import static com.android.adservices.service.measurement.PrivacyParams.DUAL_DESTINATION_EVENT_NOISE_PROBABILITY;
import static com.android.adservices.service.measurement.PrivacyParams.DUAL_DESTINATION_NAVIGATION_NOISE_PROBABILITY;
import static com.android.adservices.service.measurement.PrivacyParams.EVENT_NOISE_PROBABILITY;
import static com.android.adservices.service.measurement.PrivacyParams.INSTALL_ATTR_DUAL_DESTINATION_EVENT_NOISE_PROBABILITY;
import static com.android.adservices.service.measurement.PrivacyParams.INSTALL_ATTR_DUAL_DESTINATION_NAVIGATION_NOISE_PROBABILITY;
import static com.android.adservices.service.measurement.PrivacyParams.INSTALL_ATTR_EVENT_NOISE_PROBABILITY;
import static com.android.adservices.service.measurement.PrivacyParams.INSTALL_ATTR_NAVIGATION_NOISE_PROBABILITY;
import static com.android.adservices.service.measurement.PrivacyParams.NAVIGATION_NOISE_PROBABILITY;
import static com.android.adservices.service.measurement.SourceFixture.ValidSourceParams.ATTRIBUTION_DESTINATIONS;
import static com.android.adservices.service.measurement.SourceFixture.ValidSourceParams.WEB_DESTINATIONS;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;

import android.net.Uri;

import com.android.adservices.service.Flags;
import com.android.adservices.service.measurement.Source;
import com.android.adservices.service.measurement.SourceFixture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This test is to test {@link SourceNoiseHandler#getRandomAttributionProbability(Source)} through
 * parameters.
 */
@RunWith(Parameterized.class)
public class SourceNoiseHandlerAttributionProbabilityTest {
    private static final double ZERO_DELTA = 0D;
    private static final String DELIMITER = ",";
    private static final long CURRENT_TIME = System.currentTimeMillis();

    private final String mDescription;
    private final boolean mIsEnableConfigurableEventReportingWindows;
    private final Source mSource;
    private final Long[] mEarlyReportingWindows;
    private final double mExpectedProbability;
    private final boolean mEnableConfiguredMaxEventReports;
    private final int mConfiguredMaxEventReportsCount;

    /**
     * The data format is measurement_enable_configurable_event_reporting_windows flag, sourceType,
     * sourceEventReportWindow (limit), cooldown window, appDestination, webDestination
     * configuredEarlyReportingWindows, coarse destination, enable configured conversions,
     * configured conversions and expectedProbability. Each test description has numbers like 1-1-1,
     * 2-1-2, 3-3-3 etc. These signify max reports, trigger data bits and reporting windows count
     * respectively. For e.g., 2-1-2 stands for 2 maximum conversions, 1 trigger data bit (0 or 1)
     * and 2 available reporting windows.
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                    {
                        "non-configured reporting windows, EVENT, 1-1-1, app, fine "
                                + "destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configured max reports
                        1, // configured max reports
                        EVENT_NOISE_PROBABILITY, // probability
                    },
                    {
                        "non-configured reporting windows, EVENT, 2-1-2, app, install "
                                + "detection, fine "
                                + "destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configured max reports
                        1, // configured max reports
                        INSTALL_ATTR_EVENT_NOISE_PROBABILITY, // probability
                    },
                    {
                        "non-configured reporting windows, EVENT, 1-1-1, web, fine "
                                + "destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        null, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configured max reports
                        1, // configured max reports
                        EVENT_NOISE_PROBABILITY, // probability
                    },
                    {
                        "non-configured reporting windows, EVENT, 1-1-1, app and web, "
                                + "fine destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configured max reports
                        1, // configured max reports
                        DUAL_DESTINATION_EVENT_NOISE_PROBABILITY, // probability
                    },
                    {
                        "non-configured reporting windows, EVENT, 2-1-2, app & web, "
                                + "install "
                                + "detection, fine"
                                + " destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // app destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configured max reports
                        1, // configured max reports
                        INSTALL_ATTR_DUAL_DESTINATION_EVENT_NOISE_PROBABILITY,
                    },
                    {
                        "non-configured reporting windows, EVENT, 2-1-2, app & web, "
                                + "install "
                                + "detection, coarse"
                                + " destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // app destination
                        new Long[] {}, // early reporting windows
                        true, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        INSTALL_ATTR_EVENT_NOISE_PROBABILITY,
                    },
                    {
                        "non-configured reporting windows, NAVIGATION, 3-3-3, app, fine "
                                + "destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configured max reports
                        3, // configured max reports
                        NAVIGATION_NOISE_PROBABILITY, // probability
                    },
                    {
                        "non-configured reporting windows, NAVIGATION, 3-3-3, app, install "
                                + "detection, fine "
                                + "destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configured max reports
                        3, // configured max reports
                        INSTALL_ATTR_NAVIGATION_NOISE_PROBABILITY, // probability
                    },
                    {
                        "non-configured reporting windows, NAVIGATION, 3-3-3, web, fine "
                                + "destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        null,
                        WEB_DESTINATIONS, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        NAVIGATION_NOISE_PROBABILITY, // probability
                    },
                    {
                        "non-configured reporting windows, NAVIGATION, 3-3-3, app & web, "
                                + "fine destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        DUAL_DESTINATION_NAVIGATION_NOISE_PROBABILITY, // probability
                    },
                    {
                        "non-configured reporting windows, NAVIGATION, 3-3-3, app & web, "
                                + "coarse destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {}, // early reporting windows
                        true, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        NAVIGATION_NOISE_PROBABILITY, // probability
                    },
                    {
                        "non-configured reporting windows, NAVIGATION, 3-3-3, app & web, "
                                + "install detection,"
                                + " fine destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        INSTALL_ATTR_DUAL_DESTINATION_NAVIGATION_NOISE_PROBABILITY,
                    },
                    {
                        "non-configured reporting windows, NAVIGATION, 3-3-3, app & web, "
                                + "install detection,"
                                + " coarse destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {}, // early reporting windows
                        true, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        INSTALL_ATTR_NAVIGATION_NOISE_PROBABILITY,
                    },
                    {
                        "configured reporting windows, EVENT, 1-1-1, app, fine " + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        EVENT_NOISE_PROBABILITY, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 1-1-2, app, fine " + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {HOURS.toSeconds(1)}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        0.0000042,
                    },
                    {
                        "configured reporting windows, EVENT, 1-1-3, app, fine " + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {HOURS.toSeconds(1), DAYS.toSeconds(1)},
                        false, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        0.0000058, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 1-1-2(1 effective window), "
                                + "app, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {DAYS.toSeconds(15)}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        EVENT_NOISE_PROBABILITY, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 1-1-3(2 effective window), "
                                + "app, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        HOURS.toMillis(6), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {HOURS.toSeconds(1), DAYS.toSeconds(1)},
                        false, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        0.0000042,
                    },
                    {
                        "configured reporting windows, EVENT, 2-1-3(2 effective windows), "
                                + "app, install "
                                + "detection, fine destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        HOURS.toMillis(6), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {
                            HOURS.toSeconds(1), DAYS.toSeconds(1)
                        }, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        INSTALL_ATTR_EVENT_NOISE_PROBABILITY, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 2-1-3, app, install "
                                + "detection, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(6), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {
                            HOURS.toSeconds(1), DAYS.toSeconds(1)
                        }, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        0.0000233, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 2-1-3, app & web, install "
                                + "detection, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(6), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {
                            HOURS.toSeconds(1), DAYS.toSeconds(1)
                        }, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        0.0000757, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 2-1-3, app & web, install "
                                + "detection, coarse "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(6), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {
                            HOURS.toSeconds(1), DAYS.toSeconds(1)
                        }, // early reporting windows
                        true, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        0.0000233, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 1-1-1, web, (install "
                                + "cooldown -"
                                + " unused), fine"
                                + " destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        null, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        EVENT_NOISE_PROBABILITY, // probability
                    },
                    {
                        // It is different from "non-configured reporting windows, 2-1-2,
                        // app
                        // & web, install
                        // detection" because we reject 20 states resulting into only 25
                        // states in
                        // that case. Here we assume all 45 states to be valid.
                        "configured reporting windows, EVENT, 2-1-2, app & web, install "
                                + "detection, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {HOURS.toSeconds(1)}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        0.0000374, // probability
                    },
                    {
                        // It is different from "non-configured reporting windows, 2-1-2,
                        // app
                        // & web, install
                        // detection, coarse destinations" because we reject 20 states
                        // resulting into only 25 states in that case. Here we assume all
                        // 45 states to be valid.
                        "configured reporting windows, EVENT, 2-1-2, app & web, install "
                                + "detection, coarse "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {HOURS.toSeconds(1)}, // early reporting windows
                        true, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        0.0000125, // probability
                    },
                    {
                        "configured (ignored due to empty), EVENT, 2-1-2, app, install "
                                + "detection, fine destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        INSTALL_ATTR_EVENT_NOISE_PROBABILITY, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 1-1-1, app & web, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // app destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        DUAL_DESTINATION_EVENT_NOISE_PROBABILITY, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 1-1-1, app & web, coarse "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // app destination
                        new Long[] {}, // early reporting windows
                        true, // coarse destinations
                        false, // enable configurable max reports
                        1, // configured max reports
                        EVENT_NOISE_PROBABILITY, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-1, app, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        0.0001372, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-2, app, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {HOURS.toSeconds(1)}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        0.0008051, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-3, app, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {HOURS.toSeconds(1), DAYS.toSeconds(1)},
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        NAVIGATION_NOISE_PROBABILITY, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-2 (1 effective "
                                + "window)"
                                + ", app, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(2), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {DAYS.toMillis(3)}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        0.0001372, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-3 (2 effective "
                                + "windows), app, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        HOURS.toMillis(6), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {HOURS.toSeconds(1), DAYS.toSeconds(1)},
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        0.0008051, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-1, app, install "
                                + "detection, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        0.0001372, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-1, web, install "
                                + "detection, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        null, // app destination
                        WEB_DESTINATIONS, // app destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        0.0001372, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-1, app & web, fine"
                                + " destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // app destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        0.0008051, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-1, app & web, "
                                + "coarse destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // app destination
                        new Long[] {}, // early reporting windows
                        true, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        0.0001372, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-1, app & web, "
                                + "install "
                                + "detection, "
                                + "fine destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // app destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        0.0008051, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-1, app & web, "
                                + "install "
                                + "detection, "
                                + "coarse destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // app destination
                        new Long[] {}, // early reporting windows
                        true, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        0.0001372, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-3, app & web, "
                                + "install "
                                + "detection, fine"
                                + " destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {HOURS.toSeconds(2), DAYS.toSeconds(2)},
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        INSTALL_ATTR_DUAL_DESTINATION_NAVIGATION_NOISE_PROBABILITY,
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-3, app & web, "
                                + "install "
                                + "detection, "
                                + "coarse destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {HOURS.toSeconds(2), DAYS.toSeconds(2)},
                        true, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        INSTALL_ATTR_NAVIGATION_NOISE_PROBABILITY,
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-3, app, install "
                                + "detection, fine destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {HOURS.toSeconds(2), DAYS.toSeconds(2)},
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        INSTALL_ATTR_NAVIGATION_NOISE_PROBABILITY, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-3, app & web, fine"
                                + " destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {HOURS.toSeconds(2), DAYS.toSeconds(2)},
                        false, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        DUAL_DESTINATION_NAVIGATION_NOISE_PROBABILITY, // probability
                    },
                    {
                        "configured reporting windows, NAVIGATION, 3-3-3, app & web, "
                                + "coarse destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.NAVIGATION, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {HOURS.toSeconds(2), DAYS.toSeconds(2)},
                        true, // coarse destinations
                        false, // enable configurable max reports
                        3, // configured max reports
                        NAVIGATION_NOISE_PROBABILITY, // probability
                    },
                    {
                        "non-configured reporting windows, EVENT, 3-1-1, app, fine "
                                + "destinations",
                        false, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {}, // early reporting windows
                        false, // coarse destinations
                        true, // enable configured max reports
                        3, // configured max reports
                        0.0000083, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 3-1-3, app, fine " + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {HOURS.toSeconds(1), DAYS.toSeconds(1)},
                        false, // coarse destinations
                        true, // enable configurable max reports
                        3, // configured max reports
                        0.0000698, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 2-1-3, app, fine " + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {HOURS.toSeconds(1), DAYS.toSeconds(1)},
                        false, // coarse destinations
                        true, // enable configurable max reports
                        2, // configured max reports
                        0.0000233, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 3-1-3, app, install "
                                + "detection, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(6), // source event report window
                        // not honored because conversions and windows are overridden
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {
                            HOURS.toSeconds(1), DAYS.toSeconds(1)
                        }, // early reporting windows
                        false, // coarse destinations
                        true, // enable configurable max reports
                        3, // configured max reports
                        0.0000698, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 3-1-3, app & web, install "
                                + "detection, fine "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(6), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {
                            HOURS.toSeconds(1), DAYS.toSeconds(1)
                        }, // early reporting windows
                        false, // coarse destinations
                        true, // enable configurable max reports
                        3, // configured max reports
                        0.0003782, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 3-1-3, app & web, install "
                                + "detection, coarse "
                                + "destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(6), // source event report window
                        DAYS.toMillis(1), // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        WEB_DESTINATIONS, // web destination
                        new Long[] {
                            HOURS.toSeconds(1), DAYS.toSeconds(1)
                        }, // early reporting windows
                        true, // coarse destinations
                        true, // enable configurable max reports
                        3, // configured max reports
                        0.0000698, // probability
                    },
                    {
                        "configured reporting windows, EVENT, 1-1-3, app, fine" + " destinations",
                        true, // measurement_enable_configurable_event_reporting_windows
                        Source.SourceType.EVENT, // source type
                        DAYS.toMillis(10), // source event report window
                        0, // install cooldown window
                        ATTRIBUTION_DESTINATIONS, // app destination
                        null, // web destination
                        new Long[] {HOURS.toSeconds(1), DAYS.toSeconds(1)},
                        false, // coarse destinations
                        true, // enable configurable max reports
                        1, // configured max reports
                        0.0000058, // probability
                    },
                });
    }

    public SourceNoiseHandlerAttributionProbabilityTest(
            String description,
            boolean isEnableConfigurableEventReportingWindows,
            Source.SourceType sourceType,
            long sourceEventReportWindow,
            long coolDownWindow,
            List<Uri> appDestinations,
            List<Uri> webDestinations,
            Long[] earlyReportingWindows,
            boolean coarseDestination,
            boolean isEnableConfigurableMaxEventReports,
            int configuredMaxEventReportsCount,
            double expectedProbability) {
        mDescription = description;
        mIsEnableConfigurableEventReportingWindows = isEnableConfigurableEventReportingWindows;
        mSource =
                SourceFixture.getValidSourceBuilder()
                        .setSourceType(sourceType)
                        .setEventTime(CURRENT_TIME)
                        .setEventReportWindow(CURRENT_TIME + sourceEventReportWindow)
                        .setExpiryTime(CURRENT_TIME + sourceEventReportWindow)
                        .setInstallCooldownWindow(coolDownWindow)
                        .setAppDestinations(appDestinations)
                        .setWebDestinations(webDestinations)
                        .setCoarseEventReportDestinations(coarseDestination)
                        .build();
        mEarlyReportingWindows = earlyReportingWindows;
        mExpectedProbability = expectedProbability;
        mEnableConfiguredMaxEventReports = isEnableConfigurableMaxEventReports;
        mConfiguredMaxEventReportsCount = configuredMaxEventReportsCount;
    }

    @Test
    public void getRandomAttributionProbability_withParameterizedData() {
        // Setup
        Flags flags = mock(Flags.class);
        doReturn(mIsEnableConfigurableEventReportingWindows)
                .when(flags)
                .getMeasurementEnableConfigurableEventReportingWindows();
        doReturn(convertEarlyReportingWindowFlagString(mEarlyReportingWindows))
                .when(flags)
                .getMeasurementEventReportsVtcEarlyReportingWindows();
        doReturn(convertEarlyReportingWindowFlagString(mEarlyReportingWindows))
                .when(flags)
                .getMeasurementEventReportsCtcEarlyReportingWindows();
        doReturn(true).when(flags).getMeasurementEnableCoarseEventReportDestinations();
        doReturn(mEnableConfiguredMaxEventReports)
                .when(flags)
                .getMeasurementEnableVtcConfigurableMaxEventReports();
        doReturn(mConfiguredMaxEventReportsCount)
                .when(flags)
                .getMeasurementVtcConfigurableMaxEventReportsCount();

        // Execution
        double actualProbability =
                new SourceNoiseHandler(flags).getRandomAttributionProbability(mSource);

        // Assertion
        assertEquals(mDescription, mExpectedProbability, actualProbability, ZERO_DELTA);
    }

    private static String convertEarlyReportingWindowFlagString(Long[] earlyReportingWindows) {
        return Arrays.stream(earlyReportingWindows)
                .map(Object::toString)
                .collect(Collectors.joining(DELIMITER));
    }
}
