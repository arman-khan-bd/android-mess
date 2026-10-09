package com.smartmess.android.data.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkChangeReceiver";
    private static long lastTriggerTime = 0;
    private static final long DEBOUNCE_MS = 10000; // 10 seconds debounce to prevent spam

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (isConnected) {
                long now = SystemClock.elapsedRealtime();
                if (now - lastTriggerTime > DEBOUNCE_MS) {
                    lastTriggerTime = now;
                    Log.i(TAG, "Network connection restored. Triggering background auto-sync...");
                    SyncManager.triggerSync(context);
                }
            } else {
                Log.d(TAG, "Device disconnected from network. Operating in offline SQLite mode.");
            }
        }
    }
}
