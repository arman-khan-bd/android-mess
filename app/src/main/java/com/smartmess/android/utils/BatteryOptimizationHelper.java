package com.smartmess.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BatteryOptimizationHelper {

    private static final String TAG = "BatteryOptimization";
    private static final String PREF_NAME = "battery_optimization_pref";
    private static final String KEY_PROMPT_SHOWN = "has_prompted_battery_opt";

    /**
     * Checks if battery optimization is currently ignored / disabled for SmartMess.
     * On Android versions older than Marshmallow (API 23), returns true (no Doze restrictions).
     */
    public static boolean isBatteryOptimizationIgnored(Context context) {
        if (context == null) return true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    return pm.isIgnoringBatteryOptimizations(context.getPackageName());
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed checking battery optimization status: " + e.getMessage());
            }
        }
        return true;
    }

    /**
     * Launches the system dialog requesting to whitelist SmartMess from battery optimizations.
     */
    public static void requestIgnoreBatteryOptimization(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent();
                String packageName = activity.getPackageName();
                if (!isBatteryOptimizationIgnored(activity)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    activity.startActivity(intent);
                } else {
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    activity.startActivity(intent);
                }
            } catch (Exception e) {
                Log.w(TAG, "Direct battery optimization intent failed, falling back to general settings: " + e.getMessage());
                try {
                    Intent fallback = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    fallback.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(fallback);
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Shows an informative user-facing dialog explaining why background battery optimization
     * should be disabled so push notifications are never delayed when the app is closed.
     */
    public static void showBatteryOptimizationDialog(Activity activity, Runnable onDismiss) {
        if (activity == null || activity.isFinishing()) return;

        if (isBatteryOptimizationIgnored(activity)) {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle("✅ ব্যাকগ্রাউন্ড নোটিফিকেশন চালু আছে")
                    .setMessage("স্মার্ট মেস অ্যাপে ব্যাকগ্রাউন্ড সিঙ্ক চালু আছে। অ্যাপ বন্ধ থাকলেও আপনি তাৎক্ষণিক নোটিফিকেশন পাবেন।")
                    .setPositiveButton("OK", (dialog, which) -> {
                        if (onDismiss != null) onDismiss.run();
                    })
                    .show();
            return;
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle("⚡ অ্যাপ বন্ধ থাকলেও নোটিফিকেশন পান")
                .setMessage("অ্যান্ড্রয়েডের ব্যাটারি সেভার অ্যাপ বন্ধ থাকলে ব্যাকগ্রাউন্ড সিঙ্ক পিছিয়ে দিতে পারে।\n\nতাত্ক্ষণিক মেস নোটিশ, মিল লক সতর্কতা ও বাজারের দায়িত্ব পেতে স্মার্ট মেসকে ব্যাকগ্রাউন্ডে চলতে অনুমতি দিন।")
                .setPositiveButton("অনুমতি দিন", (dialog, which) -> {
                    markPromptShown(activity);
                    requestIgnoreBatteryOptimization(activity);
                    if (onDismiss != null) onDismiss.run();
                })
                .setNegativeButton("পরে করব", (dialog, which) -> {
                    markPromptShown(activity);
                    if (onDismiss != null) onDismiss.run();
                })
                .setCancelable(true)
                .show();
    }

    /**
     * Prompts the user once per app session if battery optimizations are active.
     */
    public static void promptOnceIfNeeded(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        if (isBatteryOptimizationIgnored(activity)) return;

        SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean hasPrompted = prefs.getBoolean(KEY_PROMPT_SHOWN, false);
        if (!hasPrompted) {
            showBatteryOptimizationDialog(activity, null);
        }
    }

    private static void markPromptShown(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_PROMPT_SHOWN, true).apply();
        } catch (Exception ignored) {}
    }
}
