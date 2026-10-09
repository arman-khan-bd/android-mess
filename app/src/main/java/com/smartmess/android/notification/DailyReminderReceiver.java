package com.smartmess.android.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.smartmess.android.R;
import com.smartmess.android.ui.MainActivity;

public class DailyReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "DailyReminderReceiver";
    public static final String CHANNEL_ID = "smartmess_meal_reminder_channel";
    public static final int NOTIFICATION_ID = 2026;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Daily cut-off alarm received! Firing meal confirmation notification...");

        createNotificationChannel(context);
        showNotification(context);

        // Reschedule alarm for the next day
        String cutoff = intent != null ? intent.getStringExtra("cutoff_time") : "22:00:00";
        NotificationScheduler.scheduleDailyReminder(context, cutoff);
    }

    private void showNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        // Intent directly opening FragmentMealSheet inside MainActivity
        Intent targetIntent = new Intent(context, MainActivity.class);
        targetIntent.putExtra(MainActivity.EXTRA_OPEN_TAB, "meals");
        targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                targetIntent,
                flags
        );

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_meal)
                .setContentTitle("Daily Meal Status Reminder")
                .setContentText("Please confirm your meals for tomorrow before the lock time.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Please confirm your meals for tomorrow before the lock time. Unbooked meals may not be prepared by the cook."))
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Daily Meal Reminder Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts reminding members before 10 PM daily meal cutoff lock");
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
