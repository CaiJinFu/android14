/*
 * Copyright (C) 2020 Google Inc.
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

package com.android.car.carlauncher.homescreen.audio;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Size;
import android.view.View;
import android.view.ViewStub;
import android.widget.Chronometer;
import android.widget.TextView;

import com.android.car.apps.common.BitmapUtils;
import com.android.car.apps.common.ImageUtils;
import com.android.car.carlauncher.R;
import com.android.car.carlauncher.homescreen.HomeCardFragment;
import com.android.car.carlauncher.homescreen.HomeCardInterface;
import com.android.car.carlauncher.homescreen.ui.CardContent;
import com.android.car.carlauncher.homescreen.ui.DescriptiveTextWithControlsView;


/**
 * {@link HomeCardInterface.View} for the audio card. Displays and controls the current audio source
 * such as the currently playing (or last played) media item or an ongoing phone call.
 */
public class AudioFragment extends HomeCardFragment {

    private AudioPresenter mPresenter;
    private Chronometer mChronometer;
    private View mChronometerSeparator;
    private float mBlurRadius;
    private CardContent.CardBackgroundImage mDefaultCardBackgroundImage;

    // Views from card_content_media.xml, which is used only for the media card
    private View mMediaLayoutView;
    private TextView mMediaTitle;
    private TextView mMediaSubtitle;

    private boolean mShowSeekBar;

    @Override
    public void setPresenter(HomeCardInterface.Presenter presenter) {
        super.setPresenter(presenter);
        mPresenter = (AudioPresenter) presenter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBlurRadius = getResources().getFloat(R.dimen.card_background_image_blur_radius);
        mDefaultCardBackgroundImage = new CardContent.CardBackgroundImage(
                getContext().getDrawable(R.drawable.default_audio_background),
                getContext().getDrawable(R.drawable.control_bar_image_background));
        mShowSeekBar = getResources().getBoolean(R.bool.show_seek_bar);
    }

    @Override
    public void updateContentViewInternal(CardContent content) {
        if (content.getType() == CardContent.HomeCardContentType.DESCRIPTIVE_TEXT_WITH_CONTROLS) {
            DescriptiveTextWithControlsView audioContent =
                    (DescriptiveTextWithControlsView) content;
            updateBackgroundImage(audioContent.getImage());
            if (audioContent.getCenterControl() == null) {
                updateMediaView(audioContent.getTitle(), audioContent.getSubtitle());
            } else {
                updateDescriptiveTextWithControlsView(audioContent.getTitle(),
                        audioContent.getSubtitle(),
                        /* optionalImage= */ null, audioContent.getLeftControl(),
                        audioContent.getCenterControl(), audioContent.getRightControl());
                updateAudioDuration(audioContent);
            }
            updateSeekBarAndTimes(audioContent, false);
        } else {
            super.updateContentViewInternal(content);
        }
    }

    @Override
    protected void hideAllViews() {
        super.hideAllViews();
        getCardBackground().setVisibility(View.GONE);
        getMediaLayoutView().setVisibility(View.GONE);
    }

    private Chronometer getChronometer() {
        if (mChronometer == null) {
            mChronometer = getDescriptiveTextWithControlsLayoutView().findViewById(
                    R.id.optional_timer);
            mChronometerSeparator = getDescriptiveTextWithControlsLayoutView().findViewById(
                    R.id.optional_timer_separator);
        }
        return mChronometer;
    }

    private View getMediaLayoutView() {
        if (mMediaLayoutView == null) {
            ViewStub stub = getRootView().findViewById(R.id.media_layout);
            mMediaLayoutView = stub.inflate();
            mMediaTitle = mMediaLayoutView.findViewById(R.id.primary_text);
            mMediaSubtitle = mMediaLayoutView.findViewById(R.id.secondary_text);
            View mediaControlBarView = mMediaLayoutView.findViewById(
                    R.id.media_playback_controls_bar);
            mPresenter.initializeControlsActionBar(mediaControlBarView);
        }
        return mMediaLayoutView;
    }

    private void updateBackgroundImage(CardContent.CardBackgroundImage cardBackgroundImage) {
        if (getCardSize() != null) {
            if (cardBackgroundImage.getForeground() == null) {
                cardBackgroundImage = mDefaultCardBackgroundImage;
            }
            int maxDimen = Math.max(getCardBackgroundImage().getWidth(),
                    getCardBackgroundImage().getHeight());
            // Prioritize size of background image view. Otherwise, use size of whole card
            if (maxDimen == 0) {
                maxDimen = Math.max(getCardSize().getWidth(), getCardSize().getHeight());
            }
            Size scaledSize = new Size(maxDimen, maxDimen);
            Bitmap imageBitmap = BitmapUtils.fromDrawable(cardBackgroundImage.getForeground(),
                    scaledSize);
            Bitmap blurredBackground = ImageUtils.blur(getContext(), imageBitmap, scaledSize,
                    mBlurRadius);

            if (cardBackgroundImage.getBackground() != null) {
                getCardBackgroundImage().setBackground(cardBackgroundImage.getBackground());
                getCardBackgroundImage().setClipToOutline(true);
            }
            getCardBackgroundImage().setImageBitmap(blurredBackground, /* showAnimation= */ true);
            getCardBackground().setVisibility(View.VISIBLE);
        }
    }

    private void updateMediaView(CharSequence title, CharSequence subtitle) {
        getMediaLayoutView().setVisibility(View.VISIBLE);
        mMediaTitle.setText(title);
        mMediaSubtitle.setText(subtitle);
        mMediaSubtitle.setVisibility(TextUtils.isEmpty(subtitle) ? View.GONE : View.VISIBLE);
        if (getOptionalSeekbarWithTimesContainer() != null) {
            getOptionalSeekbarWithTimesContainer().setVisibility(
                    mShowSeekBar ? View.VISIBLE : View.GONE);
        }
    }

    private void updateAudioDuration(DescriptiveTextWithControlsView content) {
        if (content.getStartTime() > 0) {
            getChronometer().setVisibility(View.VISIBLE);
            getChronometer().setBase(content.getStartTime());
            getChronometer().start();
            mChronometerSeparator.setVisibility(View.VISIBLE);
        } else {
            getChronometer().setVisibility(View.GONE);
            mChronometerSeparator.setVisibility(View.GONE);
        }
    }
}
