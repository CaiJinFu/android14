/*
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

package android.graphics.fonts;

import com.android.layoutlib.bridge.impl.DelegateManager;
import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import android.annotation.NonNull;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Delegate implementing the native methods of android.graphics.fonts.Font$Builder
 * <p>
 * Through the layoutlib_create tool, the original native methods of Font$Builder have been
 * replaced by calls to methods of the same name in this delegate class.
 * <p>
 * This class behaves like the original native implementation, but in Java, keeping previously
 * native data into its own objects and mapping them to int that are sent back and forth between it
 * and the original Font$Builder class.
 *
 * @see DelegateManager
 */
public class Font_Builder_Delegate {

    @LayoutlibDelegate
    /*package*/ static ByteBuffer createBuffer(@NonNull AssetManager am, @NonNull String path,
            boolean isAsset, int cookie) throws IOException {

        if (path.isBlank()) {
            return null;
        }

        try (InputStream assetStream = isAsset ? am.open(path, AssetManager.ACCESS_BUFFER)
                : am.openNonAsset(cookie, path, AssetManager.ACCESS_BUFFER)) {

            int capacity = assetStream.available();
            ByteBuffer buffer = ByteBuffer.allocateDirect(capacity);
            buffer.order(ByteOrder.nativeOrder());
            ReadableByteChannel channel = Channels.newChannel(assetStream);
            channel.read(buffer);

            if (assetStream.read() != -1) {
                throw new IOException("Unable to access full contents of " + path);
            }

            return buffer;
        }
    }
}
