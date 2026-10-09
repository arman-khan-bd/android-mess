package com.smartmess.android.engine;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.TelemetryReportRequest;
import com.smartmess.android.utils.SessionManager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class CrashTelemetryHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashTelemetry";
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
            reportCrash(thread, throwable);
        } catch (Throwable t) {
            Log.e(TAG, "Failed during crash telemetry dispatch", t);
        } finally {
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        }
    }

    private void reportCrash(Thread thread, Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        String title = (throwable.getMessage() != null && !throwable.getMessage().isEmpty())
                ? throwable.getClass().getSimpleName() + ": " + throwable.getMessage()
                : throwable.getClass().getSimpleName() + " in thread " + thread.getName();

        Map<String, String> metadata = getDeviceDiagnostics(context);

        TelemetryReportRequest request = new TelemetryReportRequest(
                "crash_auto",
                title,
                "Uncaught fatal exception occurred in thread: " + thread.getName(),
                stackTrace,
                metadata,
                "critical"
        );

        try {
            SessionManager session = new SessionManager(context);
            if (session.isLoggedIn()) {
                request.setMessId(session.getMessId());
                request.setUserId(session.getUserId());
            }
        } catch (Throwable ignored) {}

        // Send crash report on background thread to prevent NetworkOnMainThreadException
        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ApiService apiService = ApiClient.getApiService(context);
                    apiService.sendTelemetryReport(request).execute();
                    Log.i(TAG, "Crash report successfully transmitted to server.");
                } catch (Throwable netEx) {
                    Log.w(TAG, "Could not upload crash report: " + netEx.getMessage());
                }
            }
        });
        networkThread.start();
        try {
            networkThread.join(2000);
        } catch (InterruptedException ignored) {}
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
