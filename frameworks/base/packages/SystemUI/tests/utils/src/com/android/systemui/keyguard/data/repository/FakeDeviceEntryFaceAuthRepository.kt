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

package com.android.systemui.keyguard.data.repository

import com.android.keyguard.FaceAuthUiEvent
import com.android.systemui.keyguard.shared.model.AuthenticationStatus
import com.android.systemui.keyguard.shared.model.DetectionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull

class FakeDeviceEntryFaceAuthRepository : DeviceEntryFaceAuthRepository {

    override val isAuthenticated = MutableStateFlow(false)
    override val canRunFaceAuth = MutableStateFlow(false)
    private val _authenticationStatus = MutableStateFlow<AuthenticationStatus?>(null)
    override val authenticationStatus: Flow<AuthenticationStatus> =
        _authenticationStatus.filterNotNull()
    fun setAuthenticationStatus(status: AuthenticationStatus) {
        _authenticationStatus.value = status
    }
    private val _detectionStatus = MutableStateFlow<DetectionStatus?>(null)
    override val detectionStatus: Flow<DetectionStatus>
        get() = _detectionStatus.filterNotNull()
    fun setDetectionStatus(status: DetectionStatus) {
        _detectionStatus.value = status
    }
    override val isLockedOut = MutableStateFlow(false)
    private val _runningAuthRequest = MutableStateFlow<Pair<FaceAuthUiEvent, Boolean>?>(null)
    val runningAuthRequest: StateFlow<Pair<FaceAuthUiEvent, Boolean>?> =
        _runningAuthRequest.asStateFlow()

    private val _isAuthRunning = MutableStateFlow(false)
    override val isAuthRunning: StateFlow<Boolean> = _isAuthRunning

    override val isBypassEnabled = MutableStateFlow(false)

    override suspend fun authenticate(uiEvent: FaceAuthUiEvent, fallbackToDetection: Boolean) {
        _runningAuthRequest.value = uiEvent to fallbackToDetection
        _isAuthRunning.value = true
    }

    override fun cancel() {
        _isAuthRunning.value = false
        _runningAuthRequest.value = null
    }
}