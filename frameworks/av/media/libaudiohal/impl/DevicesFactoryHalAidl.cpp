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

#include <memory>

#define LOG_TAG "DevicesFactoryHalAidl"
//#define LOG_NDEBUG 0

#include <aidl/android/hardware/audio/core/IModule.h>
#include <android/binder_manager.h>
#include <binder/IServiceManager.h>
#include <media/AidlConversionNdkCpp.h>
#include <media/AidlConversionUtil.h>
#include <utils/Log.h>

#include "DeviceHalAidl.h"
#include "DevicesFactoryHalAidl.h"

using aidl::android::aidl_utils::statusTFromBinderStatus;
using aidl::android::hardware::audio::core::IConfig;
using aidl::android::hardware::audio::core::IModule;
using aidl::android::hardware::audio::core::SurroundSoundConfig;
using aidl::android::media::audio::common::AudioHalEngineConfig;
using ::android::detail::AudioHalVersionInfo;

namespace android {

namespace {

ConversionResult<media::SurroundSoundConfig::SurroundFormatFamily>
ndk2cpp_SurroundSoundConfigFormatFamily(const SurroundSoundConfig::SurroundFormatFamily& ndk) {
    media::SurroundSoundConfig::SurroundFormatFamily cpp;
    cpp.primaryFormat = VALUE_OR_RETURN(ndk2cpp_AudioFormatDescription(ndk.primaryFormat));
    cpp.subFormats = VALUE_OR_RETURN(::aidl::android::convertContainer<std::vector<
            media::audio::common::AudioFormatDescription>>(ndk.subFormats,
                    ndk2cpp_AudioFormatDescription));
    return cpp;
}

ConversionResult<media::SurroundSoundConfig>
ndk2cpp_SurroundSoundConfig(const SurroundSoundConfig& ndk) {
    media::SurroundSoundConfig cpp;
    cpp.formatFamilies = VALUE_OR_RETURN(::aidl::android::convertContainer<std::vector<
            media::SurroundSoundConfig::SurroundFormatFamily>>(ndk.formatFamilies,
                    ndk2cpp_SurroundSoundConfigFormatFamily));
    return cpp;
}

}  // namespace

DevicesFactoryHalAidl::DevicesFactoryHalAidl(std::shared_ptr<IConfig> config)
    : mConfig(std::move(config)) {
}

status_t DevicesFactoryHalAidl::getDeviceNames(std::vector<std::string> *names) {
    if (names == nullptr) {
        return BAD_VALUE;
    }
    AServiceManager_forEachDeclaredInstance(IModule::descriptor, static_cast<void*>(names),
            [](const char* instance, void* context) {
                if (strcmp(instance, "default") == 0) instance = "primary";
                static_cast<decltype(names)>(context)->push_back(instance);
            });
    return OK;
}

// Opens a device with the specified name. To close the device, it is
// necessary to release references to the returned object.
status_t DevicesFactoryHalAidl::openDevice(const char *name, sp<DeviceHalInterface> *device) {
    if (name == nullptr || device == nullptr) {
        return BAD_VALUE;
    }

    // FIXME: Remove this call and the check for the supported module names
    // after implementing retrieval of module names on the framework side.
    // Currently it is still using the legacy XML config.
    std::vector<std::string> deviceNames;
    if (status_t status = getDeviceNames(&deviceNames); status != OK) {
        return status;
    }
    std::shared_ptr<IModule> service;
    if (std::find(deviceNames.begin(), deviceNames.end(), name) != deviceNames.end()) {
        if (strcmp(name, "primary") == 0) name = "default";
        auto serviceName = std::string(IModule::descriptor) + "/" + name;
        service = IModule::fromBinder(
                ndk::SpAIBinder(AServiceManager_waitForService(serviceName.c_str())));
        ALOGE_IF(service == nullptr, "%s fromBinder %s failed", __func__, serviceName.c_str());
    }
    // If the service is a nullptr, the device object will not be really functional,
    // but will not crash either.
    *device = sp<DeviceHalAidl>::make(name, service);
    return OK;
}

status_t DevicesFactoryHalAidl::getHalPids(std::vector<pid_t> *pids) {
    if (pids == nullptr) {
        return BAD_VALUE;
    }
    // The functionality for retrieving debug infos of services is not exposed via the NDK.
    sp<IServiceManager> sm = defaultServiceManager();
    if (sm == nullptr) {
        return NO_INIT;
    }
    std::set<pid_t> pidsSet;
    const auto moduleServiceName = std::string(IModule::descriptor) + "/";
    auto debugInfos = sm->getServiceDebugInfo();
    for (const auto& info : debugInfos) {
        if (info.pid > 0 &&
                info.name.size() > moduleServiceName.size() && // '>' as there must be instance name
                info.name.substr(0, moduleServiceName.size()) == moduleServiceName) {
            pidsSet.insert(info.pid);
        }
    }
    *pids = {pidsSet.begin(), pidsSet.end()};
    return NO_ERROR;
}

status_t DevicesFactoryHalAidl::setCallbackOnce(sp<DevicesFactoryHalCallback> callback) {
    // Dynamic registration of module instances is not supported. The functionality
    // in the audio server which is related to this callback can be removed together
    // with HIDL support.
    ALOG_ASSERT(callback != nullptr);
    if (callback != nullptr) {
        callback->onNewDevicesAvailable();
    }
    return NO_ERROR;
}

AudioHalVersionInfo DevicesFactoryHalAidl::getHalVersion() const {
    int32_t versionNumber = 0;
    if (ndk::ScopedAStatus status = mConfig->getInterfaceVersion(&versionNumber); !status.isOk()) {
        ALOGE("%s getInterfaceVersion failed: %s", __func__, status.getDescription().c_str());
    }
    // AIDL does not have minor version, fill 0 for all versions
    return AudioHalVersionInfo(AudioHalVersionInfo::Type::AIDL, versionNumber);
}

status_t DevicesFactoryHalAidl::getSurroundSoundConfig(media::SurroundSoundConfig *config) {
    SurroundSoundConfig ndkConfig;
    RETURN_STATUS_IF_ERROR(statusTFromBinderStatus(mConfig->getSurroundSoundConfig(&ndkConfig)));
    *config = VALUE_OR_RETURN_STATUS(ndk2cpp_SurroundSoundConfig(ndkConfig));
    return OK;
}

status_t DevicesFactoryHalAidl::getEngineConfig(
        media::audio::common::AudioHalEngineConfig *config) {
    AudioHalEngineConfig ndkConfig;
    RETURN_STATUS_IF_ERROR(statusTFromBinderStatus(mConfig->getEngineConfig(&ndkConfig)));
    *config = VALUE_OR_RETURN_STATUS(ndk2cpp_AudioHalEngineConfig(ndkConfig));
    return OK;
}

// Main entry-point to the shared library.
extern "C" __attribute__((visibility("default"))) void* createIDevicesFactoryImpl() {
    auto serviceName = std::string(IConfig::descriptor) + "/default";
    auto service = IConfig::fromBinder(
            ndk::SpAIBinder(AServiceManager_waitForService(serviceName.c_str())));
    if (!service) {
        ALOGE("%s binder service %s not exist", __func__, serviceName.c_str());
        return nullptr;
    }
    return new DevicesFactoryHalAidl(service);
}

} // namespace android
