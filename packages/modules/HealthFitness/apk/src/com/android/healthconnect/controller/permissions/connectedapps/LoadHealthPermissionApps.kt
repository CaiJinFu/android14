/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions.connectedapps

import com.android.healthconnect.controller.permissions.api.GetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.shared.QueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.service.IoDispatcher
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.shared.app.GetContributorAppInfoUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class LoadHealthPermissionApps
@Inject
constructor(
    private val healthPermissionReader: HealthPermissionReader,
    private val loadGrantedHealthPermissionsUseCase: GetGrantedHealthPermissionsUseCase,
    private val getContributorAppInfoUseCase: GetContributorAppInfoUseCase,
    private val queryRecentAccessLogsUseCase: QueryRecentAccessLogsUseCase,
    private val appInfoReader: AppInfoReader,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ILoadHealthPermissionApps {

    /** Returns a list of ConnectedAppMetadata. */
    override suspend operator fun invoke(): List<ConnectedAppMetadata> =
        withContext(dispatcher) {
            val appsWithHealthPermissions = healthPermissionReader.getAppsWithHealthPermissions()
            val appsWithData = getContributorAppInfoUseCase.invoke()
            val connectedApps = mutableListOf<ConnectedAppMetadata>()
            val recentAccess = queryRecentAccessLogsUseCase.invoke()

            connectedApps.addAll(
                appsWithHealthPermissions.map { packageName ->
                    val metadata = appInfoReader.getAppMetadata(packageName)
                    val grantedPermissions = loadGrantedHealthPermissionsUseCase(packageName)
                    val isConnected =
                        if (grantedPermissions.isNotEmpty()) {
                            ConnectedAppStatus.ALLOWED
                        } else {
                            ConnectedAppStatus.DENIED
                        }
                    ConnectedAppMetadata(metadata, isConnected, recentAccess[metadata.packageName])
                })

            val inactiveApps =
                appsWithData.values
                    .filter { !appsWithHealthPermissions.contains(it.packageName) }
                    .map { ConnectedAppMetadata(it, ConnectedAppStatus.INACTIVE) }

            connectedApps.addAll(inactiveApps)
            connectedApps
        }
}

interface ILoadHealthPermissionApps {
    suspend fun invoke(): List<ConnectedAppMetadata>
}
