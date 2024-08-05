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

package com.android.telephony.imsmedia;

import android.os.Parcel;
import android.telephony.imsmedia.ImsMediaSession;
import android.util.Log;

import com.android.telephony.imsmedia.Utils.OpenSessionParams;

/**
 * Text service for internal AP based RTP stack. This interacts with native library to open/close
 * {@link TextLocalSession}
 */
public class TextService {
    private static final String LOG_TAG = "TextService";
    private long mNativeObject = 0;
    private TextListener mListener = null;

    TextService() {
        mNativeObject = JNIImsMediaService.getInterface(ImsMediaSession.SESSION_TYPE_RTT);
    }

    /** Returns the native instance identifier of TextManager in libimsmedia */
    public long getNativeObject() {
        return mNativeObject;
    }

    /** Sets JNI listener to get JNI callback from libimsmediajni library */
    public void setListener(final TextListener listener) {
        mListener = listener;
    }

    /**
     * Sends request message with the corresponding arguments to libimsmediajni
     * library to operate
     *
     * @param sessionId : session identifier
     * @param parcel    : parcel argument to send to JNI
     */
    public void sendRequest(final int sessionId, Parcel parcel) {
        if (mNativeObject != 0) {
            byte[] data = parcel.marshall();
            parcel.recycle();
            parcel = null;
            JNIImsMediaService.sendMessage(mNativeObject, sessionId, data);
        }
    }

    /**
     * Opens a RTP session based on local the local sockets with the associated initial remote
     * configuration if there is a valid RtpConfig passed. It starts the media flow if the media
     * direction in the RtpConfig is set to any value other than MEDIA_DIRECTION_NO_FLOW. If the
     * open session is successful then a new TextLocalSession object will be created using the
     * JNIImsMediaListener#onMessage() API. If the open session is failed then a error code will be
     * returned using JNIImsMediaListener#onMessage(int) API.
     *
     * @param sessionId     A unique RTP session identifier
     * @param sessionParams Paratmers including rtp, rtcp socket to send and receive incoming RTP
     *                      packets and RtpConfig to create session.
     */
    public void openSession(final int sessionId, final OpenSessionParams sessionParams) {
        if (mNativeObject == 0 || sessionParams == null) {
            return;
        }
        JNIImsMediaService.setListener(sessionId, mListener);
        Log.d(LOG_TAG, "openSession: sessionId = " + sessionId
                    + "," + sessionParams.getRtpConfig());
        Parcel parcel = Parcel.obtain();
        parcel.writeInt(TextSession.CMD_OPEN_SESSION);
        final int socketFdRtp = sessionParams.getRtpFd().detachFd();
        final int socketFdRtcp = sessionParams.getRtcpFd().detachFd();
        parcel.writeInt(socketFdRtp);
        parcel.writeInt(socketFdRtcp);
        if (sessionParams.getRtpConfig() != null) {
            sessionParams.getRtpConfig().writeToParcel(parcel, ImsMediaSession.SESSION_TYPE_RTT);
        }
        sendRequest(sessionId, parcel);
    }

    /**
     * Closes the RTP session including cleanup of all the resources associated with the session.
     * This will also close the session object and associated callback.
     *
     * @param sessionId The session id to be closed.
     */
    public void closeSession(final int sessionId) {
        Log.d(LOG_TAG, "closeSession");
        Parcel parcel = Parcel.obtain();
        parcel.writeInt(TextSession.CMD_CLOSE_SESSION);
        sendRequest(sessionId, parcel);
    }
}
