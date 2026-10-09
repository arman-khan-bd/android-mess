package com.smartmess.android.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class NotificationScheduler {

    private static final String TAG = "NotificationScheduler";
    public static final int DAILY_REMINDER_REQUEST_CODE = 3001;

    /**
     * Schedules the daily alarm to ring at the specified cutoff time (or default 22:00:00).
     */
    public static void scheduleDailyReminder(Context context, String cutoffTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        int targetHour = 22;
        int targetMinute = 0;

        if (cutoffTime != null && cutoffTime.contains(":")) {
            try {
                String[] parts = cutoffTime.split(":");
                targetHour = Integer.parseInt(parts[0]);
                targetMinute = Integer.parseInt(parts[1]);
            } catch (Exception e) {
                targetHour = 22;
                targetMinute = 0;
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, targetHour);
        calendar.set(Calendar.MINUTE, targetMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(context, DailyReminderReceiver.class);
        intent.putExtra("cutoff_time", cutoffTime);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_REMINDER_REQUEST_CODE,
                intent,
                flags
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
            }
            Log.i(TAG, "Daily meal reminder alarm scheduled for: " + calendar.getTime().toString());
        } catch (SecurityException se) {
            try {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } catch (Throwable ignored) {}
            Log.w(TAG, "Exact alarm permission not granted, fell back to standard alarm: " + se.getMessage());
        } catch (Throwable t) {
            Log.w(TAG, "Could not schedule reminder alarm: " + t.getMessage());
        }
    }

    /**
     * Cancels the daily reminder alarm.
     */
    public static void cancelDailyReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, DailyReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_REMINDER_REQUEST_CODE,
                intent,
                flags
        );

        alarmManager.cancel(pendingIntent);
        Log.i(TAG, "Daily meal reminder alarm cancelled.");
    }
}
