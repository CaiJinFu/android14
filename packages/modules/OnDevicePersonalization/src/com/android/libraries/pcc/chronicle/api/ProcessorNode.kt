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

package com.android.libraries.pcc.chronicle.api

/**
 * A processor node is a description of a component which asks [Chronicle] for [Connection] objects
 * and performs some kind of computation on the data supplied by those connections or writes data
 * back to them (or any mix of the two).
 */
interface ProcessorNode {
  /** The classes of the connections required by this [ProcessorNode]. */
  val requiredConnectionTypes: Set<Class<out Connection>>
}
