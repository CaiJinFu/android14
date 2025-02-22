/**
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.radio.platform;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.android.car.broadcastradio.support.platform.ImageResolver;
import com.android.internal.annotations.GuardedBy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves metadata image cache for specific {@link RadioManagerExt}
 */
public final class ImageMemoryCache implements ImageResolver {
    private final Object mLock = new Object();

    private final RadioManagerExt mRadioManager;

    @GuardedBy("mLock")
    private final Map<Long, Bitmap> mCache;

    public ImageMemoryCache(RadioManagerExt radioManager, int cacheSize) {
        mRadioManager = Objects.requireNonNull(radioManager, "RadioManager cannot be null");
        mCache = new CacheMap<>(cacheSize);
    }

    /**
     * Gets metadata image cache
     *
     * @param globalId Metadata image id
     * @return Metadata image
     */
    @Nullable
    public Bitmap resolve(long globalId) {
        synchronized (mLock) {
            if (mCache.containsKey(globalId)) {
                return mCache.get(globalId);
            }

            Bitmap bm = mRadioManager.getMetadataImage(globalId);
            mCache.put(globalId, bm);
            return bm;
        }
    }

    private static class CacheMap<K, V> extends LinkedHashMap<K, V> {
        private final int mMaxSize;

        public CacheMap(int maxSize) {
            if (maxSize < 0) {
                throw new IllegalArgumentException("maxSize must not be negative");
            }
            mMaxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > mMaxSize;
        }
    }
}
