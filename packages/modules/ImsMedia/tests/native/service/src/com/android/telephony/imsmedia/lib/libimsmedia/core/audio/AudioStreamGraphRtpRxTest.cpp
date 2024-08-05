/**
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

#include <gtest/gtest.h>
#include <ImsMediaNetworkUtil.h>
#include <AudioConfig.h>
#include <MediaQualityThreshold.h>
#include <AudioStreamGraphRtpRx.h>

using namespace android::telephony::imsmedia;

// RtpConfig
const int32_t kMediaDirection = RtpConfig::MEDIA_DIRECTION_RECEIVE_ONLY;
const android::String8 kRemoteAddress("127.0.0.1");
const int32_t kRemotePort = 10000;
const int8_t kDscp = 0;
const int8_t kRxPayload = 96;
const int8_t kTxPayload = 96;
const int8_t kSamplingRate = 16;

// RtcpConfig
const android::String8 kCanonicalName("name");
const int32_t kTransmitPort = 1001;
const int32_t kIntervalSec = 5;
const int32_t kRtcpXrBlockTypes = RtcpConfig::FLAG_RTCPXR_STATISTICS_SUMMARY_REPORT_BLOCK |
        RtcpConfig::FLAG_RTCPXR_VOIP_METRICS_REPORT_BLOCK;

// AudioConfig
const int8_t kPTimeMillis = 20;
const int32_t kMaxPtimeMillis = 100;
const int8_t kcodecModeRequest = 15;
const bool kDtxEnabled = true;
const int32_t kCodecType = AudioConfig::CODEC_AMR_WB;
const int8_t kDtmfPayloadTypeNumber = 100;
const int8_t kDtmfsamplingRateKHz = 16;

// AmrParam
const int32_t kAmrMode = 8;
const bool kOctetAligned = false;
const int32_t kMaxRedundancyMillis = 240;

// EvsParam
const int32_t kEvsBandwidth = EvsParams::EVS_BAND_NONE;
const int32_t kEvsMode = 8;
const int8_t kChannelAwareMode = 3;
const bool kUseHeaderFullOnly = false;

class AudioStreamGraphRtpRxTest : public ::testing::Test
{
public:
    AudioStreamGraphRtpRxTest()
    {
        graph = nullptr;
        socketRtpFd = -1;
    }
    ~AudioStreamGraphRtpRxTest() {}

protected:
    AudioStreamGraphRtpRx* graph;
    AudioConfig config;
    RtcpConfig rtcp;
    AmrParams amr;
    EvsParams evs;
    int socketRtpFd;

    virtual void SetUp() override
    {
        rtcp.setCanonicalName(kCanonicalName);
        rtcp.setTransmitPort(kTransmitPort);
        rtcp.setIntervalSec(kIntervalSec);
        rtcp.setRtcpXrBlockTypes(kRtcpXrBlockTypes);

        amr.setAmrMode(kAmrMode);
        amr.setOctetAligned(kOctetAligned);
        amr.setMaxRedundancyMillis(kMaxRedundancyMillis);

        evs.setEvsBandwidth(kEvsBandwidth);
        evs.setEvsMode(kEvsMode);
        evs.setChannelAwareMode(kChannelAwareMode);
        evs.setUseHeaderFullOnly(kUseHeaderFullOnly);
        evs.setCodecModeRequest(kcodecModeRequest);

        config.setMediaDirection(kMediaDirection);
        config.setRemoteAddress(kRemoteAddress);
        config.setRemotePort(kRemotePort);
        config.setRtcpConfig(rtcp);
        config.setDscp(kDscp);
        config.setRxPayloadTypeNumber(kRxPayload);
        config.setTxPayloadTypeNumber(kTxPayload);
        config.setSamplingRateKHz(kSamplingRate);
        config.setPtimeMillis(kPTimeMillis);
        config.setMaxPtimeMillis(kMaxPtimeMillis);
        config.setDtxEnabled(kDtxEnabled);
        config.setCodecType(kCodecType);
        config.setTxDtmfPayloadTypeNumber(kDtmfPayloadTypeNumber);
        config.setRxDtmfPayloadTypeNumber(kDtmfPayloadTypeNumber);
        config.setDtmfsamplingRateKHz(kDtmfsamplingRateKHz);
        config.setAmrParams(amr);
        config.setEvsParams(evs);

        const char testIp[] = "127.0.0.1";
        unsigned int testPort = 30000;
        socketRtpFd = ImsMediaNetworkUtil::openSocket(testIp, testPort, AF_INET);
        EXPECT_NE(socketRtpFd, -1);

        graph = new AudioStreamGraphRtpRx(nullptr, socketRtpFd);
    }

    virtual void TearDown() override
    {
        if (graph != nullptr)
        {
            delete graph;
        }

        if (socketRtpFd != -1)
        {
            ImsMediaNetworkUtil::closeSocket(socketRtpFd);
        }
    }
};

TEST_F(AudioStreamGraphRtpRxTest, TestGraphError)
{
    EXPECT_EQ(graph->create(nullptr), RESULT_INVALID_PARAM);
    EXPECT_EQ(graph->getState(), kStreamStateIdle);
}

TEST_F(AudioStreamGraphRtpRxTest, TestRtpRxStreamDirectionUpdate)
{
    EXPECT_EQ(graph->create(&config), RESULT_SUCCESS);
    EXPECT_EQ(graph->start(), RESULT_SUCCESS);
    EXPECT_EQ(graph->getState(), kStreamStateRunning);

    config.setMediaDirection(RtpConfig::MEDIA_DIRECTION_INACTIVE);
    EXPECT_EQ(graph->update(&config), RESULT_SUCCESS);
    EXPECT_EQ(graph->getState(), kStreamStateCreated);

    config.setMediaDirection(RtpConfig::MEDIA_DIRECTION_SEND_RECEIVE);
    EXPECT_EQ(graph->update(&config), RESULT_SUCCESS);
    EXPECT_EQ(graph->getState(), kStreamStateRunning);

    config.setMediaDirection(RtpConfig::MEDIA_DIRECTION_NO_FLOW);
    EXPECT_EQ(graph->update(&config), RESULT_SUCCESS);

    EXPECT_EQ(graph->stop(), RESULT_SUCCESS);
    EXPECT_EQ(graph->getState(), kStreamStateCreated);
}

TEST_F(AudioStreamGraphRtpRxTest, TestRtpRxStreamCodecUpdate)
{
    config.setMediaDirection(RtpConfig::MEDIA_DIRECTION_SEND_RECEIVE);
    EXPECT_EQ(graph->create(&config), RESULT_SUCCESS);
    EXPECT_EQ(graph->start(), RESULT_SUCCESS);
    EXPECT_EQ(graph->getState(), kStreamStateRunning);

    amr.setAmrMode(7);
    amr.setOctetAligned(true);
    config.setCodecType(AudioConfig::CODEC_AMR);
    config.setAmrParams(amr);
    EXPECT_EQ(graph->update(&config), RESULT_SUCCESS);
    EXPECT_EQ(graph->getState(), kStreamStateRunning);

    EXPECT_EQ(graph->stop(), RESULT_SUCCESS);
    EXPECT_EQ(graph->getState(), kStreamStateCreated);
}
