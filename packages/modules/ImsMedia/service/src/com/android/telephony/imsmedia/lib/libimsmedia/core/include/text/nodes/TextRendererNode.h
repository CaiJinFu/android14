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

#ifndef TEXT_RENDERER_NODE_H_INCLUDED
#define TEXT_RENDERER_NODE_H_INCLUDED

#include <JitterBufferControlNode.h>

/**
 * @brief This class describes an interface between depacketization module
 */
class TextRendererNode : public JitterBufferControlNode
{
public:
    TextRendererNode(BaseSessionCallback* callback = nullptr);
    virtual ~TextRendererNode();
    virtual kBaseNodeId GetNodeId();
    virtual ImsMediaResult Start();
    virtual void Stop();
    virtual bool IsRunTime();
    virtual bool IsSourceNode();
    virtual void SetConfig(void* config);
    virtual bool IsSameConfig(void* config);
    virtual void ProcessData();

private:
    char mBuffer[MAX_RTT_LEN + 1];
    int32_t mCodecType;
    int8_t mRedundantLevel;
    bool mBOMReceived;
    uint16_t mLastPlayedSeq;
    uint32_t mLossWaitTime;
    bool mFirstFrameReceived;
};

#endif
