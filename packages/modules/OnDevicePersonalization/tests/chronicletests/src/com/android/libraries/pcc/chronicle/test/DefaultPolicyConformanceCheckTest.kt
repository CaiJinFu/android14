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

package com.android.libraries.pcc.chronicle.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.libraries.pcc.chronicle.api.dataTypeDescriptor
import com.android.libraries.pcc.chronicle.api.error.MalformedPolicySet
import com.android.libraries.pcc.chronicle.api.policy.builder.policy
import com.android.libraries.pcc.chronicle.test.data.Foobar
import com.android.libraries.pcc.chronicle.api.policy.StorageMedium
import com.android.libraries.pcc.chronicle.api.policy.DefaultPolicyConformanceCheck
import com.android.libraries.pcc.chronicle.api.FieldType
import com.android.libraries.pcc.chronicle.api.dataTypeDescriptor
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class DefaultPolicyConformanceCheckTest {
    private val check = DefaultPolicyConformanceCheck()
    val FOOBAR_DTD = dataTypeDescriptor("Foobar", Foobar::class) {
        "name" to FieldType.String
    }

    @Test
    fun testIfDescriptionMissing() {
        val policies = setOf(
            policy("MyFirstPolicy", "TestEgress") {
                // Pretend we forgot a description here.
                target(FOOBAR_DTD, maxAge = Duration.ZERO) { retention(StorageMedium.RAM) }
            },
            policy("MySecondPolicy", "TestEgress") {
                description = "I didn't forget a description!"
                target(FOOBAR_DTD, maxAge = Duration.ZERO) { retention(StorageMedium.RAM) }
            },
        )

        val e = assertFailsWith<MalformedPolicySet> { check.checkPoliciesConform(policies) }
        assertThat(e).hasMessageThat().contains("Policy: \"MyFirstPolicy\" has an empty description")
    }

    @Test
    fun testIfRetentionRulesElided() {
        val policies = setOf(
            policy("MyFirstPolicy", "TestEgress") {
            description = "I didn't forget a description!"
            target(FOOBAR_DTD, maxAge = Duration.ZERO) {
                // Forgot to put a retention rule here.
            }
            },
            policy("MySecondPolicy", "TestEgress") {
            description = "I didn't forget a description!"
            target(FOOBAR_DTD, maxAge = Duration.ZERO) { retention(StorageMedium.RAM) }
            },
        )

        val e = assertFailsWith<MalformedPolicySet> { check.checkPoliciesConform(policies) }
        assertThat(e)
        .hasMessageThat()
        .contains(
            "Malformed policies found: Target \"Foobar\" from policy: \"MyFirstPolicy\" does not specify any retention rules"
        )
    }

    @Test
    fun testWhenPoliciesConform() {
        val policies = setOf(
            policy("MyFirstPolicy", "TestEgress") {
                description = "I didn't forget a description!"
                target(FOOBAR_DTD, maxAge = Duration.ZERO) { retention(StorageMedium.RAM) }
            },
            policy("MySecondPolicy", "TestEgress") {
                description = "I didn't forget a description!"
                target(FOOBAR_DTD, maxAge = Duration.ZERO) { retention(StorageMedium.RAM) }
            },
        )

        check.checkPoliciesConform(policies)
    }
}
