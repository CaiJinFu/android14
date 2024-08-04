/*
 * Copyright (C) 2017 The Android Open Source Project
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

#include <mediadrm/IDrm.h>
#include <mediadrm/DrmStatus.h>

#ifndef DRM_HAL_H_
#define DRM_HAL_H_

namespace android {

struct DrmHal : public IDrm {
    DrmHal();
    virtual ~DrmHal();
    virtual DrmStatus initCheck() const;
    virtual DrmStatus isCryptoSchemeSupported(const uint8_t uuid[16], const String8& mimeType,
                                              DrmPlugin::SecurityLevel securityLevel, bool* result);
    virtual DrmStatus createPlugin(const uint8_t uuid[16],
                                   const String8 &appPackageName);
    virtual DrmStatus destroyPlugin();
    virtual DrmStatus openSession(DrmPlugin::SecurityLevel securityLevel,
            Vector<uint8_t> &sessionId);
    virtual DrmStatus closeSession(Vector<uint8_t> const &sessionId);
    virtual DrmStatus
        getKeyRequest(Vector<uint8_t> const &sessionId,
                      Vector<uint8_t> const &initData,
                      String8 const &mimeType, DrmPlugin::KeyType keyType,
                      KeyedVector<String8, String8> const &optionalParameters,
                      Vector<uint8_t> &request, String8 &defaultUrl,
                      DrmPlugin::KeyRequestType *keyRequestType);
    virtual DrmStatus provideKeyResponse(Vector<uint8_t> const &sessionId,
                                         Vector<uint8_t> const &response,
                                         Vector<uint8_t> &keySetId);
    virtual DrmStatus removeKeys(Vector<uint8_t> const &keySetId);
    virtual DrmStatus restoreKeys(Vector<uint8_t> const &sessionId,
                                  Vector<uint8_t> const &keySetId);
    virtual DrmStatus queryKeyStatus(Vector<uint8_t> const &sessionId,
                                     KeyedVector<String8, String8> &infoMap) const;
    virtual DrmStatus getProvisionRequest(String8 const &certType,
                                          String8 const &certAuthority,
                                          Vector<uint8_t> &request,
                                          String8 &defaultUrl);
    virtual DrmStatus provideProvisionResponse(Vector<uint8_t> const &response,
                                               Vector<uint8_t> &certificate,
                                               Vector<uint8_t> &wrappedKey);
    virtual DrmStatus getSecureStops(List<Vector<uint8_t>> &secureStops);
    virtual DrmStatus getSecureStopIds(List<Vector<uint8_t>> &secureStopIds);
    virtual DrmStatus getSecureStop(Vector<uint8_t> const &ssid, Vector<uint8_t> &secureStop);
    virtual DrmStatus releaseSecureStops(Vector<uint8_t> const &ssRelease);
    virtual DrmStatus removeSecureStop(Vector<uint8_t> const &ssid);
    virtual DrmStatus removeAllSecureStops();
    virtual DrmStatus getHdcpLevels(DrmPlugin::HdcpLevel *connectedLevel,
            DrmPlugin::HdcpLevel *maxLevel) const;
    virtual DrmStatus getNumberOfSessions(uint32_t *currentSessions,
            uint32_t *maxSessions) const;
    virtual DrmStatus getSecurityLevel(Vector<uint8_t> const &sessionId,
            DrmPlugin::SecurityLevel *level) const;
    virtual DrmStatus getOfflineLicenseKeySetIds(List<Vector<uint8_t>> &keySetIds) const;
    virtual DrmStatus removeOfflineLicense(Vector<uint8_t> const &keySetId);
    virtual DrmStatus getOfflineLicenseState(Vector<uint8_t> const &keySetId,
            DrmPlugin::OfflineLicenseState *licenseState) const;
    virtual DrmStatus getPropertyString(String8 const &name, String8 &value) const;
    virtual DrmStatus getPropertyByteArray(String8 const &name,
                                           Vector<uint8_t> &value) const;
    virtual DrmStatus setPropertyString(String8 const &name,
                                        String8 const &value ) const;
    virtual DrmStatus setPropertyByteArray(String8 const &name,
                                           Vector<uint8_t> const &value) const;
    virtual DrmStatus getMetrics(const sp<IDrmMetricsConsumer> &consumer);
    virtual DrmStatus setCipherAlgorithm(Vector<uint8_t> const &sessionId,
                                         String8 const &algorithm);
    virtual DrmStatus setMacAlgorithm(Vector<uint8_t> const &sessionId,
                                      String8 const &algorithm);
    virtual DrmStatus encrypt(Vector<uint8_t> const &sessionId,
                              Vector<uint8_t> const &keyId,
                              Vector<uint8_t> const &input,
                              Vector<uint8_t> const &iv,
                              Vector<uint8_t> &output);
    virtual DrmStatus decrypt(Vector<uint8_t> const &sessionId,
                              Vector<uint8_t> const &keyId,
                              Vector<uint8_t> const &input,
                              Vector<uint8_t> const &iv,
                              Vector<uint8_t> &output);
    virtual DrmStatus sign(Vector<uint8_t> const &sessionId,
                           Vector<uint8_t> const &keyId,
                           Vector<uint8_t> const &message,
                           Vector<uint8_t> &signature);
    virtual DrmStatus verify(Vector<uint8_t> const &sessionId,
                             Vector<uint8_t> const &keyId,
                             Vector<uint8_t> const &message,
                             Vector<uint8_t> const &signature,
                             bool &match);
    virtual DrmStatus signRSA(Vector<uint8_t> const &sessionId,
                              String8 const &algorithm,
                              Vector<uint8_t> const &message,
                              Vector<uint8_t> const &wrappedKey,
                              Vector<uint8_t> &signature);
    virtual DrmStatus setListener(const sp<IDrmClient>& listener);
    virtual DrmStatus requiresSecureDecoder(const char *mime, bool *required) const;
    virtual DrmStatus requiresSecureDecoder(const char *mime,
                                            DrmPlugin::SecurityLevel securityLevel,
                                            bool *required) const;
    virtual DrmStatus setPlaybackId(
            Vector<uint8_t> const &sessionId,
            const char *playbackId);
    virtual DrmStatus getLogMessages(Vector<drm::V1_4::LogMessage> &logs) const;
    virtual DrmStatus getSupportedSchemes(std::vector<uint8_t> &schemes) const;

private:
    sp<IDrm> mDrmHalHidl;
    sp<IDrm> mDrmHalAidl;
    DISALLOW_EVIL_CONSTRUCTORS(DrmHal);
};

} // namespace android

#endif  // DRM_HAL_H_