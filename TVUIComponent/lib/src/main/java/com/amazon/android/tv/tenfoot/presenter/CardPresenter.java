/**
 * Copyright 2015-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.amazon.android.tv.tenfoot.presenter;

import com.amazon.android.contentbrowser.ContentBrowser;
import com.amazon.android.model.Action;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.tv.tenfoot.base.TenFootApp;
import com.amazon.android.tv.tenfoot.utils.ContentHelper;
import com.amazon.android.utils.GlideHelper;
import com.amazon.android.utils.Helpers;
import com.amazon.android.tv.tenfoot.R;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.zype.fire.api.ZypeConfiguration;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.BaseCardView;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {

    private static final String TAG = CardPresenter.class.getSimpleName();

    private int mCardWidthDp;
    private int mCardHeightDp;

    private Drawable mDefaultCardImage;
    private static Drawable sFocusedFadeMask;
    private View mInfoField;
    private Context mContext;
    /* Zype, Evgeny Cherkasov */
    private Drawable imageLocked;
    private Drawable imageUnlocked;
    private static Drawable infoFieldWithProgressBarBackground;
    private ContentBrowser contentBrowser;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {

        mContext = parent.getContext();
        /* Zype, Evgeny Cherkasov */
        contentBrowser = ContentBrowser.getInstance((Activity) mContext);
        try {
            mDefaultCardImage = ContextCompat.getDrawable(mContext, R.drawable.movie);
            sFocusedFadeMask = ContextCompat.getDrawable(mContext, R.drawable.content_fade_focused);
            /* Zype, Evgeny Cherkasov */
            infoFieldWithProgressBarBackground = ContextCompat.getDrawable(mContext, R.drawable.content_fade_focused_progress_bar);
            imageLocked = ContextCompat.getDrawable(mContext, R.drawable.locked);
            imageUnlocked = ContextCompat.getDrawable(mContext, R.drawable.unlocked);
        }
        catch (Resources.NotFoundException e) {
            Log.e(TAG, "Could not find resource ", e);
            throw e;
        }

        ImageCardView cardView = new ImageCardView(mContext) {
            @Override
            public void setSelected(boolean selected) {

                super.setSelected(selected);
//                if (mInfoField != null) {
//                    mInfoField.setBackground(sFocusedFadeMask);
//                }
            }
        };
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);

        // Set the type and visibility of the info area.
        cardView.setCardType(BaseCardView.CARD_TYPE_INFO_OVER);
        cardView.setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_ALWAYS);

        /* Zype, Evgeny Cherkasov */
        // Make card size 16:9
//        int CARD_WIDTH_PX = 160;
        int CARD_WIDTH_PX = 210;
        mCardWidthDp = Helpers.convertPixelToDp(mContext, CARD_WIDTH_PX);

        int CARD_HEIGHT_PX = 120;
        mCardHeightDp = Helpers.convertPixelToDp(mContext, CARD_HEIGHT_PX);

        TextView subtitle = (TextView) cardView.findViewById(R.id.content_text);
        if (subtitle != null) {
            subtitle.setEllipsize(TextUtils.TruncateAt.END);
        }

        mInfoField = cardView.findViewById(R.id.info_field);
        if (mInfoField != null) {
            mInfoField.setBackground(sFocusedFadeMask);
        }

        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {

        ImageCardView cardView = (ImageCardView) viewHolder.view;

        if (item instanceof Content) {
            Content content = (Content) item;

            if (content.getCardImageUrl() != null) {

                // The word 'Title' is not logically correct in setTitleText,
                // the 'TitleText' is actually smaller text compared to 'ContentText',
                // so we are using TitleText to show subtitle and ContentText to show the
                // actual Title.
                cardView.setTitleText(ContentHelper.getCardViewSubtitle(mContext, content));


                cardView.setContentText(content.getTitle());
                cardView.setMainImageDimensions(mCardWidthDp, mCardHeightDp);
                /* Zype, Evgeny Cherkasov */
                double playbackPercentage = content.getExtraValueAsDouble(Content.EXTRA_PLAYBACK_POSITION_PERCENTAGE);
                if (ZypeConfiguration.displayWatchedBarOnVideoThumbnails() && playbackPercentage > 0) {
                    SimpleTarget<Bitmap> bitmapTarget = new SimpleTarget<Bitmap>(mCardWidthDp, mCardHeightDp) {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                            cardView.setInfoAreaBackground(infoFieldWithProgressBarBackground);
                            Bitmap bitmap = Helpers.addProgressToThumbnail((Activity) mContext, resource, playbackPercentage, 0);
                            cardView.getMainImageView().setImageBitmap(bitmap);
                        }
                    };
                    GlideHelper.loadImageIntoSimpleTargetBitmap(
                            viewHolder.view.getContext(),
                            content.getCardImageUrl(),
                            new GlideHelper.LoggingListener<>(),
                            R.drawable.movie,
                            bitmapTarget);
                }
                else {
                    GlideHelper.loadImageIntoView(
                            cardView.getMainImageView(),
                            viewHolder.view.getContext(),
                            content.getCardImageUrl(),
                            new GlideHelper.LoggingListener<>(),
                            R.drawable.movie);
                    cardView.setInfoAreaBackground(sFocusedFadeMask);
                }

                /* Zype, Evgeny Cherkasov */
                // Display lock icon for subscription video
                if (content.isSubscriptionRequired()) {
                    if (contentBrowser.isUserSubscribed()) {
                        cardView.setBadgeImage(imageUnlocked);
                    }
                    else {
                        cardView.setBadgeImage(imageLocked);
                    }
                }
                else {
                    cardView.setBadgeImage(null);
                }
            }
        }
        else if (item instanceof ContentContainer) {
            ContentContainer contentContainer = (ContentContainer) item;
            cardView.setContentText(contentContainer.getName());
            cardView.setMainImageDimensions(mCardWidthDp, mCardHeightDp);
            /* Zype, Evgeny Cherkasov */
            // Show image for playlist
            if (contentContainer.getExtraStringValue(Content.CARD_IMAGE_URL_FIELD_NAME) != null) {
                GlideHelper.loadImageIntoView(cardView.getMainImageView(),
                        viewHolder.view.getContext(),
                        contentContainer.getExtraStringValue(Content.CARD_IMAGE_URL_FIELD_NAME),
                        new GlideHelper.LoggingListener<>(),
                        R.drawable.movie);
//                Glide.with(viewHolder.view.getContext())
//                        .load(contentContainer.getExtraStringValue(Content.CARD_IMAGE_URL_FIELD_NAME))
//                        .listener(new GlideHelper.LoggingListener<>())
//                        .centerCrop()
//                        .error(mDefaultCardImage)
//                        .into(cardView.getMainImageView());
            }
            else {
                cardView.getMainImageView().setImageDrawable(mDefaultCardImage);
            }
        }
        /* Zype, Evgeny CHerkasov */
        else if (item instanceof Action) {
            Action action = (Action) item;
            cardView.setContentText(action.getLabel1());
            cardView.setMainImageScaleType(ImageView.ScaleType.CENTER);
            cardView.setMainImageDimensions(mCardWidthDp, mCardHeightDp);
            try {
                cardView.setMainImage(ContextCompat.getDrawable(TenFootApp.getInstance().getApplicationContext(),
                        action.getIconResourceId()));
            }
            catch (Resources.NotFoundException e) {
                Log.e(TAG, "Resource not found", e);
                throw e;
            }
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {

        ImageCardView cardView = (ImageCardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}

