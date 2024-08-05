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

package com.android.adservices.concurrency;

import android.annotation.NonNull;
import android.annotation.SuppressLint;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * All executors of the PP API module.
 *
 * @hide
 */
// TODO(b/224987182): set appropriate parameters (priority, size, etc..) for the shared thread pools
// after doing detailed analysis. Ideally the parameters should be backed by PH flags.
public final class AdServicesExecutors {
    // We set the minimal number of threads for background executor to 4 and lightweight & scheduled
    //  executors to 2 since Runtime.getRuntime().availableProcessors() may return 1 or 2 for
    //  low-end devices. This may cause deadlock for starvation in those low-end devices.
    private static final int MIN_BACKGROUND_EXECUTOR_THREADS = 4;
    private static final int MIN_LIGHTWEIGHT_EXECUTOR_THREADS = 2;
    private static final int MAX_SCHEDULED_EXECUTOR_THREADS = 4;

    private static final String LIGHTWEIGHT_NAME = "lightweight";
    private static final String BACKGROUND_NAME = "background";
    private static final String SCHEDULED_NAME = "scheduled";
    private static final String BLOCKING_NAME = "blocking";

    private static ThreadFactory getFactory(final String threadPrefix) {
        return new ThreadFactory() {
            private final AtomicLong mThreadCount = new AtomicLong(0L);

            @SuppressLint("DefaultLocale")
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setName(
                        String.format(
                                Locale.US, "%s-%d", threadPrefix, mThreadCount.incrementAndGet()));
                return thread;
            }
        };
    }

    private static final ListeningExecutorService sLightWeightExecutor =
            // Always use at least two threads, so that clients can't depend on light-weight
            // executor tasks executing sequentially
            MoreExecutors.listeningDecorator(
                    new ThreadPoolExecutor(
                            /* corePoolSize= */ Math.max(
                                    MIN_LIGHTWEIGHT_EXECUTOR_THREADS,
                                    Runtime.getRuntime().availableProcessors() - 2),
                            /* maximumPoolSize */
                            Math.max(
                                    MIN_LIGHTWEIGHT_EXECUTOR_THREADS,
                                    Runtime.getRuntime().availableProcessors() - 2),
                            /* keepAliveTime= */ 60L,
                            TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(),
                            getFactory(LIGHTWEIGHT_NAME)));

    /**
     * Functions that don't do direct I/O and that are fast (under ten milliseconds or thereabouts)
     * should run on this Executor.
     *
     * <p>Most async code in an app should be written to run on this Executor.
     */
    @NonNull
    public static ListeningExecutorService getLightWeightExecutor() {
        return sLightWeightExecutor;
    }

    private static final ListeningExecutorService sBackgroundExecutor =
            MoreExecutors.listeningDecorator(
                    new ThreadPoolExecutor(
                            /* corePoolSize= */ Math.max(
                                    MIN_BACKGROUND_EXECUTOR_THREADS,
                                    Runtime.getRuntime().availableProcessors()),
                            /* maximumPoolSize */ Math.max(
                                    MIN_BACKGROUND_EXECUTOR_THREADS,
                                    Runtime.getRuntime().availableProcessors()),
                            /* keepAliveTime= */ 60L,
                            TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(),
                            getFactory(BACKGROUND_NAME)));

    /**
     * Functions that directly execute disk I/O, or that are CPU bound and long-running (over ten
     * milliseconds or thereabouts) should run on this Executor.
     *
     * <p>Examples include stepping through a database Cursor, or decoding an image into a Bitmap.
     *
     * <p>Functions that block on network I/O must run on BlockingExecutor.
     */
    @NonNull
    public static ListeningExecutorService getBackgroundExecutor() {
        return sBackgroundExecutor;
    }

    private static final ScheduledThreadPoolExecutor sScheduler =
            new ScheduledThreadPoolExecutor(
                    /* corePoolSize= */ Math.min(
                            MAX_SCHEDULED_EXECUTOR_THREADS,
                            Runtime.getRuntime().availableProcessors()),
                    getFactory(SCHEDULED_NAME));

    /**
     * Functions that require to be run with a delay, or have timed executions should run on this
     * Executor.
     *
     * <p>Example includes having timeouts on Futures.
     *
     * @return
     */
    @NonNull
    public static ScheduledThreadPoolExecutor getScheduler() {
        return sScheduler;
    }

    private static final ListeningExecutorService sBlockingExecutor =
            MoreExecutors.listeningDecorator(
                    Executors.newCachedThreadPool(getFactory(BLOCKING_NAME)));

    /**
     * Functions that directly execute network I/O, or that block their thread awaiting the progress
     * of at least one other thread, must run on BlockingExecutor.
     *
     * <p>BlockingExecutor will launch as many threads as there are tasks available to run
     * concurrently, stopping and freeing them when the concurrent task count drops again. This
     * unbounded number of threads negatively impacts performance:
     *
     * <p>Extra threads add execution overhead and increase execution latency. Each thread consumes
     * significant memory for thread-local state and stack, and may increase the total amount of
     * space used by objects on the heap. Each additional BlockingExecutor thread reduces the time
     * available to the fixed-size LightweightExecutor and BackgroundExecutor. While
     * BlockingExecutor's threads have a lower priority to decrease this impact, the extra threads
     * can still compete for resources. Always prefer to refactor a class or API to avoid blocking
     * before falling back to using the blocking Executor.
     */
    @NonNull
    public static ListeningExecutorService getBlockingExecutor() {
        return sBlockingExecutor;
    }

    private AdServicesExecutors() {}
}
