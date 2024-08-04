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
#pragma once

#include <android/binder_ibinder.h>
#include <android/binder_manager.h>

namespace android::automotive::evs {

class LinkUnlinkToDeathBase {
public:
    virtual ~LinkUnlinkToDeathBase() = default;

    virtual binder_status_t linkToDeath(AIBinder* binder, AIBinder_DeathRecipient* recipient,
                                        void* cookie) = 0;
    virtual binder_status_t unlinkToDeath(AIBinder* binder) = 0;
    virtual void* getCookie() = 0;

protected:
    void* mCookie;
    ndk::ScopedAIBinder_DeathRecipient mDeathRecipient;
};

}  // namespace android::automotive::evs
