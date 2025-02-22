/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.car.notification.template;

import android.app.Notification;
import android.os.Bundle;
import android.view.View;

import com.android.car.notification.AlertEntry;
import com.android.car.notification.NotificationClickHandlerFactory;
import com.android.car.notification.R;

/**
 * incoming phone call notification view template that displays notification.
 */
public class CallNotificationViewHolder extends CarNotificationBaseViewHolder {
    private final CarNotificationHeaderView mHeaderView;
    private final CarNotificationBodyView mBodyView;
    private final CarNotificationActionsView mActionsView;
    private final NotificationClickHandlerFactory mClickHandlerFactory;

    public CallNotificationViewHolder(
            View view, NotificationClickHandlerFactory clickHandlerFactory) {
        super(view, clickHandlerFactory);
        mHeaderView = view.findViewById(R.id.notification_header);
        mBodyView = view.findViewById(R.id.notification_body);
        mActionsView = view.findViewById(R.id.notification_actions);
        mClickHandlerFactory = clickHandlerFactory;
    }

    /**
     * Binds a {@link com.android.car.notification.AlertEntry} to a basic car notification template.
     */
    @Override
    public void bind(AlertEntry alertEntry, boolean isInGroup,
            boolean isHeadsUp, boolean isSeen) {
        super.bind(alertEntry, isInGroup, isHeadsUp, isSeen);
        bindBody(alertEntry);
        mHeaderView.bind(alertEntry, isInGroup);
        mActionsView.bind(mClickHandlerFactory, alertEntry);
    }

    /**
     * Private method that binds the data to the view.
     */
    private void bindBody(AlertEntry alertEntry) {
        Notification notification = alertEntry.getNotification();
        Bundle extraData = notification.extras;
        CharSequence title = extraData.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extraData.getCharSequence(Notification.EXTRA_TEXT);
        mBodyView.bind(title, text,
                alertEntry.getStatusBarNotification(),
                notification.getLargeIcon(), /* titleIcon= */ null, /* countText= */ null,
                notification.showsTime() ? notification.when : null);
    }
}
