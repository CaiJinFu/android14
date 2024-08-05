/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.utils

import java.time.Instant
import java.time.Instant.ofEpochMilli
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Returns an Instant with the specified year, month and day-of-month. The day must be valid for the
 * year and month, otherwise an exception will be thrown.
 *
 * @param year the year to represent, from MIN_YEAR to MAX_YEAR
 * @param month the month-of-year to represent, from 1 (January) to 12 (December)
 * @param day the day-of-month to represent, from 1 to 31
 */
fun getInstant(year: Int, month: Int, day: Int): Instant {
    val date = LocalDate.of(year, month, day)
    return date.atTime(LocalTime.MIDNIGHT).atZone(ZoneId.systemDefault()).toInstant()
}

fun Long.toInstant(): Instant {
    return ofEpochMilli(this)
}

fun Instant.toLocalDate(): LocalDate {
    return atZone(ZoneId.systemDefault()).toLocalDate()
}

fun Instant.toLocalTime(): LocalTime {
    return atZone(ZoneId.systemDefault()).toLocalTime()
}
