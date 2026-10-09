package com.smartmess.android.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.smartmess.android.R;
import com.smartmess.android.ui.MainActivity;

public class PushNotificationManager {

    private static final String TAG = "PushNotificationManager";

    public static final String CHANNEL_NOTICES = "channel_smartmess_notices";
    public static final String CHANNEL_DUES = "channel_smartmess_dues";
    public static final String CHANNEL_MEALS = "channel_smartmess_meals";

    public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    public static final String EXTRA_NAV_TARGET = "extra_nav_target";

    /**
     * Initializes notification channels for Android 8.0+ (Oreo, API 26+)
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            // 1. General Notices & Announcements Channel
            NotificationChannel noticeChannel = new NotificationChannel(
                    CHANNEL_NOTICES,
                    "Mess Notices & Announcements",
                    NotificationManager.IMPORTANCE_HIGH
            );
            noticeChannel.setDescription("Important alerts and broadcast notices from Mess Managers");
            noticeChannel.enableLights(true);
            noticeChannel.setLightColor(Color.parseColor("#10B981"));
            noticeChannel.enableVibration(true);
            noticeChannel.setShowBadge(true);
            nm.createNotificationChannel(noticeChannel);

            // 2. Due & Payment Reminders Channel
            NotificationChannel dueChannel = new NotificationChannel(
                    CHANNEL_DUES,
                    "Payment & Due Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            dueChannel.setDescription("Financial balance alerts and due payment reminders");
            dueChannel.enableLights(true);
            dueChannel.setLightColor(Color.parseColor("#EF4444"));
            dueChannel.enableVibration(true);
            dueChannel.setShowBadge(true);
            nm.createNotificationChannel(dueChannel);

            // 3. Meals & Bazar Channel
            NotificationChannel mealChannel = new NotificationChannel(
                    CHANNEL_MEALS,
                    "Meal Alerts & Bazar Duties",
                    NotificationManager.IMPORTANCE_HIGH
            );
            mealChannel.setDescription("Daily meal count confirmations, locks, and bazaar duties");
            mealChannel.enableLights(true);
            mealChannel.setLightColor(Color.parseColor("#F59E0B"));
            mealChannel.enableVibration(true);
            nm.createNotificationChannel(mealChannel);
        }
    }

    /**
     * Displays a system tray heads-up push notification when received online or via background worker.
     */
    public static void showSystemNotification(Context context, long notificationId, String title, String message, String type, String channelType) {
        if (context == null) return;

        createNotificationChannels(context);

        String channelId = CHANNEL_NOTICES;
        if ("reminder".equalsIgnoreCase(type) || "budget".equalsIgnoreCase(type) || "deposit".equalsIgnoreCase(type)) {
            channelId = CHANNEL_DUES;
        } else if ("meal".equalsIgnoreCase(type) || "bazar".equalsIgnoreCase(type) || "vacation".equalsIgnoreCase(type)) {
            channelId = CHANNEL_MEALS;
        }

        // Target intent: Opens MainActivity and triggers Full View Notification Dialog
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_NAV_TARGET, "notifications");
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) notificationId, intent, flags);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        String subText = "both".equalsIgnoreCase(channelType) ? "Push + SMS" : ("sms".equalsIgnoreCase(channelType) ? "SMS" : "Push Notice");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_lucide_bell)
                .setContentTitle(title)
                .setContentText(message)
                .setSubText(subText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 250, 150, 250})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat nmc = NotificationManagerCompat.from(context);
            int notifIntId = (int) (notificationId != 0 ? notificationId : System.currentTimeMillis() % 100000);
            nmc.notify(notifIntId, builder.build());
        } catch (SecurityException se) {
            Log.w(TAG, "Notification permission missing on Android 13+: " + se.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error displaying push notification: " + e.getMessage(), e);
        }
    }
}
