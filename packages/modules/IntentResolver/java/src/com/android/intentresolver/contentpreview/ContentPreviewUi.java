/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.intentresolver.contentpreview;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.intentresolver.R;
import com.android.intentresolver.widget.ActionRow;
import com.android.intentresolver.widget.ScrollableImagePreviewView;

abstract class ContentPreviewUi {
    private static final int IMAGE_FADE_IN_MILLIS = 150;
    static final String TAG = "ChooserPreview";

    @ContentPreviewType
    public abstract int getType();

    public abstract ViewGroup display(
            Resources resources, LayoutInflater layoutInflater, ViewGroup parent);

    protected static void updateViewWithImage(ImageView imageView, Bitmap image) {
        if (image == null) {
            imageView.setVisibility(View.GONE);
            return;
        }
        imageView.setVisibility(View.VISIBLE);
        imageView.setAlpha(0.0f);
        imageView.setImageBitmap(image);

        ValueAnimator fadeAnim = ObjectAnimator.ofFloat(imageView, "alpha", 0.0f, 1.0f);
        fadeAnim.setInterpolator(new DecelerateInterpolator(1.0f));
        fadeAnim.setDuration(IMAGE_FADE_IN_MILLIS);
        fadeAnim.start();
    }

    protected static void displayHeadline(ViewGroup layout, String headline) {
        if (layout != null) {
            TextView titleView = layout.findViewById(R.id.headline);
            if (titleView != null) {
                if (!TextUtils.isEmpty(headline)) {
                    titleView.setText(headline);
                    titleView.setVisibility(View.VISIBLE);
                } else {
                    titleView.setVisibility(View.GONE);
                }
            }
        }
    }

    protected static void displayModifyShareAction(
            ViewGroup layout,
            ChooserContentPreviewUi.ActionFactory actionFactory) {
        ActionRow.Action modifyShareAction = actionFactory.getModifyShareAction();
        if (modifyShareAction != null && layout != null) {
            TextView modifyShareView = layout.findViewById(R.id.reselection_action);
            if (modifyShareView != null) {
                modifyShareView.setText(modifyShareAction.getLabel());
                modifyShareView.setVisibility(View.VISIBLE);
                modifyShareView.setOnClickListener(view -> modifyShareAction.getOnClicked().run());
            }
        }
    }

    protected static ScrollableImagePreviewView.PreviewType getPreviewType(
            MimeTypeClassifier typeClassifier, String mimeType) {
        if (mimeType == null) {
            return ScrollableImagePreviewView.PreviewType.File;
        }
        if (typeClassifier.isImageType(mimeType)) {
            return ScrollableImagePreviewView.PreviewType.Image;
        }
        if (typeClassifier.isVideoType(mimeType)) {
            return ScrollableImagePreviewView.PreviewType.Video;
        }
        return ScrollableImagePreviewView.PreviewType.File;
    }
}
