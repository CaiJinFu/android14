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

package com.android.permissioncontroller;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.android.permissioncontroller.hibernation.HibernationJobService;
import com.android.permissioncontroller.permission.service.PermissionEventCleanupJobService;
import com.android.permissioncontroller.permission.service.v34.SafetyLabelChangesJobService;

/**
 * App-global constants
 */
public class Constants {

    /**
     * ID for the periodic job in
     * {@link com.android.permissioncontroller.permission.service.LocationAccessCheck}.
     */
    public static final int PERIODIC_LOCATION_ACCESS_CHECK_JOB_ID = 0;

    /**
     * ID for the on-demand, but delayed job in
     * {@link com.android.permissioncontroller.permission.service.LocationAccessCheck}.
     */
    public static final int LOCATION_ACCESS_CHECK_JOB_ID = 1;

    /**
     * ID of the periodic job
     * {@link HibernationJobService}
     */
    public static final int HIBERNATION_JOB_ID = 2;

    /**
     * ID of the periodic job
     * {@link PermissionEventCleanupJobService}
     */
    public static final int OLD_PERMISSION_EVENT_CLEANUP_JOB_ID = 3;

    /**
     * ID for the periodic job in
     * {@link com.android.permissioncontroller.privacysources.NotificationListenerCheck}.
     */
    public static final int PERIODIC_NOTIFICATION_LISTENER_CHECK_JOB_ID = 4;

    /**
     * ID for the on-demand, but delayed job in
     * {@link com.android.permissioncontroller.privacysources.NotificationListenerCheck}.
     */
    public static final int NOTIFICATION_LISTENER_CHECK_JOB_ID = 5;

    /**
     * ID for the periodic job in
     * {@link com.android.permissioncontroller.privacysources.AccessibilitySourceService}.
     */
    public static final int PERIODIC_ACCESSIBILITY_CHECK_JOB_ID = 6;

     /**
     * ID for Safety Centers periodic background refresh job, scheduled after boot and after Safety
     * Center is enabled, in {@link
     * com.android.permissioncontroller.safetycenter.service.SafetyCenterBackgroundRefreshJobService
     * }.
     */
    public static final int SAFETY_CENTER_BACKGROUND_REFRESH_JOB_ID = 7;


    /**
     * ID for the detect updates job in {@link SafetyLabelChangesJobService}.
     */
    public static final int SAFETY_LABEL_CHANGES_DETECT_UPDATES_JOB_ID = 8;

    /**
     * ID for the periodic notification job in {@link SafetyLabelChangesJobService}.
     */
    public static final int SAFETY_LABEL_CHANGES_PERIODIC_NOTIFICATION_JOB_ID = 9;

    /**
     * Name of file to containing the packages we already showed a notification for.
     *
     * @see com.android.permissioncontroller.permission.service.LocationAccessCheck
     */
    public static final String LOCATION_ACCESS_CHECK_ALREADY_NOTIFIED_FILE =
            "packages_already_notified_location_access";

    /**
     * ID for notification shown by
     * {@link com.android.permissioncontroller.permission.service.LocationAccessCheck}.
     */
    public static final int LOCATION_ACCESS_CHECK_NOTIFICATION_ID = 0;

    /**
     * ID for notification shown by
     * {@link HibernationJobService}.
     */
    public static final int UNUSED_APPS_NOTIFICATION_ID = 1;

    /**
     * ID for notification shown by
     * {@link com.android.permissioncontroller.auto.DrivingDecisionReminderService}.
     */
    public static final int PERMISSION_DECISION_REMINDER_NOTIFICATION_ID = 2;

    /**
     * ID for notification shown by
     * {@link com.android.permissioncontroller.privacysources.NotificationListenerCheck}.
     */
    public static final int NOTIFICATION_LISTENER_CHECK_NOTIFICATION_ID = 3;

    /**
     * ID for notification shown by
     * {@link com.android.permissioncontroller.privacysources.AccessibilitySourceService}.
     */
    public static final int ACCESSIBILITY_CHECK_NOTIFICATION_ID = 4;

    /**
     * ID for notification shown by
     * {@link SafetyLabelChangesJobService}.
     */
    public static final int SAFETY_LABEL_CHANGES_NOTIFICATION_ID = 5;

    /**
     * ID for notification of auto-granted permissions shown by
     * {@link com.android.permissioncontroller.permission.ui.AutoGrantPermissionsNotifier}.
     */
    public static final int PERMISSION_GRANTED_BY_ADMIN_NOTIFICATION_ID = 6;

    /**
     * Summary notification ID for the group of admin auto-granted permission notifications
     */
    public static final int ADMIN_AUTO_GRANTED_PERMISSIONS_NOTIFICATION_SUMMARY_ID = 7;

    /**
     * Group ID for all admin auto-granted permission notifications
     */
    public static final String ADMIN_AUTO_GRANTED_PERMISSIONS_NOTIFICATION_GROUP_ID =
            "auto granted permission group id";

    /**
     * String action for navigating to the auto revoke screen.
     */
    public static final String ACTION_MANAGE_AUTO_REVOKE = "manageAutoRevoke";

    /**
     * Key for Notification.Builder.setGroup() for the incident report approval notification.
     */
    public static final String INCIDENT_NOTIFICATION_GROUP_KEY = "incident confirmation";

    /**
     * Key for Notification.Builder.setChannelId() for the incident report approval notification.
     */
    public static final String INCIDENT_NOTIFICATION_CHANNEL_ID = "incident_confirmation";

    /**
     * ID for our notification.  We always post it with a tag which is the uri in string form.
     */
    public static final int INCIDENT_NOTIFICATION_ID = 66900652;

    /**
     * Channel of the notifications shown by
     * {@link com.android.permissioncontroller.permission.service.LocationAccessCheck},
     * {@link com.android.permissioncontroller.privacysources.NotificationListenerCheck},
     * {@link com.android.permissioncontroller.hibernation.HibernationPolicyKt},
     * {@link com.android.permissioncontroller.auto.DrivingDecisionReminderService}, and
     * {@link SafetyLabelChangesJobService}
     */
    public static final String PERMISSION_REMINDER_CHANNEL_ID = "permission reminders";

    /**
     * Name of generic shared preferences file.
     */
    public static final String PREFERENCES_FILE = "preferences";

    /**
     * Key in the generic shared preferences that stores when the location access feature
     * was enabled, specifically when it was picked up by the code managing the feature.
     */
    public static final String KEY_LOCATION_ACCESS_CHECK_ENABLED_TIME =
            "location_access_check_enabled_time";

    /**
     * Key in the generic shared preferences that stores when the last notification was shown by
     * {@link com.android.permissioncontroller.permission.service.LocationAccessCheck}
     */
    public static final String KEY_LAST_LOCATION_ACCESS_NOTIFICATION_SHOWN =
            "last_location_access_notification_shown";

    /**
     * Key in the generic shared preferences that stores when the last notification was shown by
     * {@link com.android.permissioncontroller.privacysources.NotificationListenerCheck}
     */
    public static final String KEY_LAST_NOTIFICATION_LISTENER_NOTIFICATION_SHOWN =
            "last_notification_listener_notification_shown";

    /**
     * Key in the generic shared preferences that stores if the user manually selected the "none"
     * role holder for a role.
     */
    public static final String IS_NONE_ROLE_HOLDER_SELECTED_KEY = "is_none_role_holder_selected:";

    /**
     * Key in the generic shared preferences that stores if the user manually selected the "none"
     * role holder for a role.
     */
    public static final String SEARCH_INDEXABLE_PROVIDER_PASSWORD_KEY =
            "search_indexable_provider_password";

    /**
     * Key in the generic shared preferences that stores the name of the packages that are currently
     * have an overridden user sensitivity.
     */
    public static final String FORCED_USER_SENSITIVE_UIDS_KEY = "forced_user_sensitive_uids_key";

    /**
     * Key in the generic shared preferences that stores if all packages should be considered user
     * sensitive
     */
    public static final String ALLOW_OVERRIDE_USER_SENSITIVE_KEY =
            "allow_override_user_sensitive_key";

    /**
     * Key in the generic shared preferences that controls if the
     * {@link android.Manifest.permission#RECORD_AUDIO} of the currently registered assistant is
     * user sensitive.
     */
    public static final String ASSISTANT_RECORD_AUDIO_IS_USER_SENSITIVE_KEY =
            "assistant_record_audio_is_user_sensitive_key";

    /**
     * Name of file containing the permissions that should be restored, but have not been restored
     * yet.
     */
    public static final String DELAYED_RESTORE_PERMISSIONS_FILE = "delayed_restore_permissions.xml";

    /**
     * Name of file containing the user denied status for requesting roles.
     */
    public static final String REQUEST_ROLE_USER_DENIED_FILE = "request_role_user_denied";

    /**
     * Logs to dump
     */
    public static final String LOGS_TO_DUMP_FILE = "LogToDump.log";

    /**
     * Key in the user denied status for requesting roles shared preferences that stores a string
     * set for the names of the roles that an application has been denied for once.
     */
    public static final String REQUEST_ROLE_USER_DENIED_ONCE_KEY_PREFIX = "denied_once:";

    /**
     * Key in the user denied status for requesting roles shared preferences that stores a string
     * set for the names of the roles that an application is always denied for.
     */
    public static final String REQUEST_ROLE_USER_DENIED_ALWAYS_KEY_PREFIX = "denied_always:";

    /**
     * Intent extra used to pass current sessionId between Permission Controller fragments.
     */
    public static final String EXTRA_SESSION_ID =
            "com.android.permissioncontroller.extra.SESSION_ID";

    /**
     * Intent extra used to pass privacy source details to safety center.
     */
    public static final String EXTRA_PRIVACY_SOURCE =
            "com.android.permissioncontroller.extra.PRIVACY_SOURCE";

    /**
     * Invalid session id.
     */
    public static final long INVALID_SESSION_ID = 0;

    /**
     * Key for NotificationManager.notify for auto-granted permissions notification,
     * when silently displayed to the user.
     */
    public static final String ADMIN_AUTO_GRANTED_PERMISSIONS_NOTIFICATION_CHANNEL_ID =
            "auto granted permissions";

    /**
     * Key for NotificationManager.notify the auto-granted permissions notification,
     * when alerting the user (with sound and vibration).
     */
    public static final String ADMIN_AUTO_GRANTED_PERMISSIONS_ALERTING_NOTIFICATION_CHANNEL_ID =
            "alerting auto granted permissions";

    /**
     * Package name of the Android platform.
     */
    public static final String OS_PACKAGE_NAME = "android";

    /**
     * Source id for safety center source for unused apps.
     */
    public static final String UNUSED_APPS_SAFETY_CENTER_SOURCE_ID = "AndroidPermissionAutoRevoke";

    /**
     * Issue id for safety center issue for unused apps.
     */
    public static final String UNUSED_APPS_SAFETY_CENTER_ISSUE_ID = "unused_apps_issue";

    /**
     * Action id for safety center "See unused apps" action.
     */
    public static final String UNUSED_APPS_SAFETY_CENTER_SEE_UNUSED_APPS_ID = "see_unused_apps";

    // TODO(b/231624295) add to API
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public static final String OPSTR_RECEIVE_AMBIENT_TRIGGER_AUDIO =
            "android:receive_ambient_trigger_audio";

    /**
     * Extra used by Settings to indicate an Intent should be treated as if opened directly by
     * Settings app itself.
     */
    public static final String EXTRA_FROM_SETTINGS = "is_from_settings_homepage";

    /**
     * Extra used by Settings to indicate an Intent should be treated as if opened by a slice
     * within Settings.
     *
     * <p>Slices are opened within settings by firing a PendingIntent, so we can use this extra to
     * allow the same UX path to be taken as for slices.
     */
    public static final String EXTRA_IS_FROM_SLICE = "is_from_slice";
}
