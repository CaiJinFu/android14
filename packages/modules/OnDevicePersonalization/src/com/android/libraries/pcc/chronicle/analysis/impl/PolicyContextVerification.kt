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

package com.android.libraries.pcc.chronicle.analysis.impl

import com.android.libraries.pcc.chronicle.api.policy.Policy
import com.android.libraries.pcc.chronicle.api.policy.builder.PolicyCheck
import com.android.libraries.pcc.chronicle.util.TypedMap

/**
 * Given a [TypedMap] of context, check the `allowedContext` policy rules to verify whether the
 * context is allowed.
 */
internal fun Policy.verifyContext(connectionContext: TypedMap): List<PolicyCheck> {
  if (this.allowedContext(connectionContext)) {
    return emptyList()
  }

  return listOf(PolicyCheck("Connection context fails to meet required policy conditions"))
}
