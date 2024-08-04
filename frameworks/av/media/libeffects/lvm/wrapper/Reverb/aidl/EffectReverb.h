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

#pragma once

#include <aidl/android/hardware/audio/effect/BnEffect.h>

#include "effect-impl/EffectImpl.h"
#include "ReverbContext.h"

namespace aidl::android::hardware::audio::effect {

class EffectReverb final : public EffectImpl {
  public:
    explicit EffectReverb(const AudioUuid& uuid);
    ~EffectReverb() override;

    ndk::ScopedAStatus getDescriptor(Descriptor* _aidl_return) override;

    ndk::ScopedAStatus setParameterSpecific(const Parameter::Specific& specific) override;
    ndk::ScopedAStatus getParameterSpecific(const Parameter::Id& id,
                                            Parameter::Specific* specific) override;

    std::shared_ptr<EffectContext> createContext(const Parameter::Common& common) override;
    std::shared_ptr<EffectContext> getContext() override;
    RetCode releaseContext() override;

    IEffect::Status effectProcessImpl(float* in, float* out, int samples) override;

    ndk::ScopedAStatus commandImpl(CommandId command) override;

    std::string getEffectName() override { return *mEffectName; }

  private:
    std::shared_ptr<ReverbContext> mContext;
    const Descriptor* mDescriptor;
    const std::string* mEffectName;
    lvm::ReverbEffectType mType;

    IEffect::Status status(binder_status_t status, size_t consumed, size_t produced);

    ndk::ScopedAStatus setParameterPresetReverb(const Parameter::Specific& specific);
    ndk::ScopedAStatus getParameterPresetReverb(const PresetReverb::Id& id,
                                                Parameter::Specific* specific);

    ndk::ScopedAStatus setParameterEnvironmentalReverb(const Parameter::Specific& specific);
    ndk::ScopedAStatus getParameterEnvironmentalReverb(const EnvironmentalReverb::Id& id,
                                                       Parameter::Specific* specific);
};

}  // namespace aidl::android::hardware::audio::effect
