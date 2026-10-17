package com.smartmess.android.engine;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.smartmess.android.ui.telemetry.CrashReportActivity;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class CrashTelemetryHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashTelemetry";
    public static final String PREF_NAME = "smartmess_crash_prefs";
    public static final String KEY_HAS_CRASH = "has_crash";
    public static final String KEY_CRASH_TITLE = "crash_title";
    public static final String KEY_CRASH_MESSAGE = "crash_message";
    public static final String KEY_CRASH_STACK = "crash_stack";
    public static final String KEY_CRASH_THREAD = "crash_thread";
    public static final String KEY_CRASH_TIME = "crash_time";
    public static final String KEY_CRASH_SENT = "crash_sent";
    public static final String KEY_CRASH_REPORT_ID = "crash_report_id";

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public static void install(Context context) {
        Thread.UncaughtExceptionHandler current = Thread.getDefaultUncaughtExceptionHandler();
        if (!(current instanceof CrashTelemetryHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CrashTelemetryHandler(context.getApplicationContext(), current));
            Log.i(TAG, "Global CrashTelemetryHandler initialized successfully.");
        }
    }

    public CrashTelemetryHandler(Context context, Thread.UncaughtExceptionHandler defaultHandler) {
        this.context = context;
        this.defaultHandler = defaultHandler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            handleCrashAndLaunchRecovery(thread, throwable);
        } catch (Throwable t) {
            Log.e(TAG, "Failed during crash telemetry dispatch", t);
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        } finally {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        }
    }

    private void handleCrashAndLaunchRecovery(Thread thread, Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        String title = (throwable.getMessage() != null && !throwable.getMessage().isEmpty())
                ? throwable.getClass().getSimpleName() + ": " + throwable.getMessage()
                : throwable.getClass().getSimpleName() + " in thread " + thread.getName();

        long crashTime = System.currentTimeMillis();

        // 1. Save crash details into SharedPreferences for persistent recovery
        saveCrashRecord(context, title, throwable.getMessage(), stackTrace, thread.getName(), crashTime);

        // 2. Launch CrashReportActivity in a separate clean process
        try {
            Intent intent = new Intent(context, CrashReportActivity.class);
            intent.putExtra(CrashReportActivity.EXTRA_CRASH_TITLE, title);
            intent.putExtra(CrashReportActivity.EXTRA_CRASH_MESSAGE, throwable.getMessage());
            intent.putExtra(CrashReportActivity.EXTRA_CRASH_STACK, stackTrace);
            intent.putExtra(CrashReportActivity.EXTRA_CRASH_THREAD, thread.getName());
            intent.putExtra(CrashReportActivity.EXTRA_CRASH_TIME, crashTime);
            intent.putExtra(CrashReportActivity.EXTRA_AUTO_SHOW_DIALOG, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
            Log.i(TAG, "Crash recovery page launched successfully.");
        } catch (Throwable launchEx) {
            Log.e(TAG, "Failed to launch CrashReportActivity", launchEx);
        }
    }

    public static SharedPreferences getCrashPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveCrashRecord(Context context, String title, String message, String stackTrace, String threadName, long time) {
        try {
            getCrashPrefs(context).edit()
                    .putBoolean(KEY_HAS_CRASH, true)
                    .putString(KEY_CRASH_TITLE, title)
                    .putString(KEY_CRASH_MESSAGE, message != null ? message : "")
                    .putString(KEY_CRASH_STACK, stackTrace)
                    .putString(KEY_CRASH_THREAD, threadName != null ? threadName : "main")
                    .putLong(KEY_CRASH_TIME, time)
                    .putBoolean(KEY_CRASH_SENT, false)
                    .apply();
        } catch (Throwable ignored) {}
    }

    public static void markCrashSent(Context context, long reportId) {
        try {
            getCrashPrefs(context).edit()
                    .putBoolean(KEY_CRASH_SENT, true)
                    .putLong(KEY_CRASH_REPORT_ID, reportId)
                    .apply();
        } catch (Throwable ignored) {}
    }

    public static void clearCrashRecord(Context context) {
        try {
            getCrashPrefs(context).edit()
                    .putBoolean(KEY_HAS_CRASH, false)
                    .putBoolean(KEY_CRASH_SENT, true)
                    .apply();
        } catch (Throwable ignored) {}
    }

    public static Map<String, String> getDeviceDiagnostics(Context context) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("device_brand", Build.BRAND);
        metadata.put("device_model", Build.MODEL);
        metadata.put("device_manufacturer", Build.MANUFACTURER);
        metadata.put("android_release", Build.VERSION.RELEASE);
        metadata.put("sdk_int", String.valueOf(Build.VERSION.SDK_INT));

        try {
            ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            if (actManager != null) {
                actManager.getMemoryInfo(memInfo);
                long availMb = memInfo.availMem / (1024 * 1024);
                metadata.put("ram_free_mb", String.valueOf(availMb));
            }
        } catch (Throwable ignored) {}

        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long bytesAvailable = stat.getAvailableBytes();
            long freeMb = bytesAvailable / (1024 * 1024);
            metadata.put("storage_free_mb", String.valueOf(freeMb));
        } catch (Throwable ignored) {}

        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                metadata.put("network_type", (netInfo != null && netInfo.isConnected()) ? netInfo.getTypeName() : "offline");
            }
        } catch (Throwable ignored) {}

        return metadata;
    }
}
