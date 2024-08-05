/**
 * Copyright (c) 2022 The Android Open Source Project
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

package android.telephony.imsmedia;

import android.telephony.imsmedia.AudioConfig;
import android.telephony.imsmedia.MediaQualityThreshold;
import android.telephony.ims.RtpHeaderExtension;

/**
 * See ImsAudioSession for more information.
 *
 * {@hide}
 */
interface IImsAudioSession {
    int getSessionId();
    oneway void modifySession(in AudioConfig config);
    oneway void addConfig(in AudioConfig config);
    oneway void deleteConfig(in AudioConfig config);
    oneway void confirmConfig(in AudioConfig config);
    oneway void sendDtmf(char dtmfDigit, int duration);
    oneway void startDtmf(char dtmfDigit);
    oneway void stopDtmf();
    oneway void sendHeaderExtension(in List<RtpHeaderExtension> extensions);
    oneway void setMediaQualityThreshold(in MediaQualityThreshold threshold);
}

