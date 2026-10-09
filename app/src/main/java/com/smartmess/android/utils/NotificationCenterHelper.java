package com.smartmess.android.utils;

import android.content.Context;
import android.util.Log;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.AppNotificationDao;
import com.smartmess.android.model.AppNotification;

import java.util.UUID;

public class NotificationCenterHelper {

    private static final String TAG = "NotificationCenter";

    public static final String TYPE_NOTICE = "notice";
    public static final String TYPE_REMINDER = "reminder";
    public static final String TYPE_EXPENSE = "expense";
    public static final String TYPE_ROLE = "role";
    public static final String TYPE_BUDGET = "budget_alert";
    public static final String TYPE_VACATION = "vacation";
    public static final String TYPE_DEPOSIT = "deposit";
    public static final String TYPE_SYSTEM = "system";

    public static void postNotification(Context context, long messId, String title, String message, String type) {
        if (context == null || messId <= 0) return;

        try {
            AppNotificationDao dao = new AppNotificationDao(DatabaseHelper.getInstance(context));
            AppNotification notification = new AppNotification(
                    UUID.randomUUID().toString(),
                    messId,
                    0, // broadcast / all members
                    title,
                    message,
                    type,
                    "push",
                    DateTimeUtils.nowIso()
            );
            long rowId = dao.insert(notification);
            Log.d(TAG, "Notification stored: [" + type + "] " + title);

            // Display heads-up system notification
            com.smartmess.android.notification.PushNotificationManager.showSystemNotification(
                    context, rowId, title, message, type, "push"
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert notification: " + e.getMessage());
        }
    }
}
