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

package com.android.libraries.pcc.chronicle.api.cantrip

/** A [Cantrip] of [Cantrips]. */
class MultiCantrip<Data>(private val cantrips: List<Cantrip<Data>>) : Cantrip<Data> {
  constructor(vararg cantrips: Cantrip<Data>) : this(cantrips.toList())

  /** Returns whether or not this [MultiCantrip] is a no-op. */
  fun isNoOp(): Boolean = cantrips.isEmpty()

  override fun invoke(datum: Data): Data? {
    var result = datum
    cantrips.forEach { cantrip -> result = cantrip(result) ?: return null }
    return result
  }
}
