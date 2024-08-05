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

package android.net.http.cts.util

import android.net.http.UrlRequest.StatusListener
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertSame

private const val TIMEOUT_MS = 12000L

/** Test status listener for requests */
class TestStatusListener : StatusListener {
    private val statusFuture = CompletableFuture<Int>()

    override fun onStatus(status: Int) {
        statusFuture.complete(status)
    }

    /** Fails if the expected status is not the returned status */
    fun expectStatus(expected: Int) {
        assertSame(expected, statusFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS))
    }
}
