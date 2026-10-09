package com.smartmess.android.data.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.smartmess.android.data.local.DatabaseManager;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.SyncPayload;
import com.smartmess.android.data.remote.dto.SyncPullResponse;
import com.smartmess.android.data.remote.dto.SyncPushResponse;
import com.smartmess.android.utils.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Response;

public class SyncManager {

    private static final String TAG = "SyncManager";
    private static SyncManager sInstance;

    public interface SyncCallback {
        void onSyncStarted();
        void onSyncSuccess(String message);
        void onSyncFailed(String error);
    }

    private final Context context;
    private final ApiService apiService;
    private final DatabaseManager databaseManager;
    private final SessionManager sessionManager;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final AtomicBoolean isSyncing = new AtomicBoolean(false);

    public static synchronized SyncManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SyncManager(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Static helper to trigger sync on background thread from BroadcastReceiver or Services.
     */
    public static void triggerSync(Context context) {
        getInstance(context).triggerSync((SyncCallback) null);
    }

    /**
     * Static helper with callback.
     */
    public static void triggerSync(Context context, SyncCallback callback) {
        getInstance(context).triggerSync(callback);
    }

    public SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = ApiClient.getApiService(this.context);
        this.databaseManager = DatabaseManager.getInstance(this.context);
        this.sessionManager = new SessionManager(this.context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Executes two-way background sync on a background thread.
     * Guaranteed non-blocking for UI thread on older Android devices.
     */
    public void triggerSync(final SyncCallback callback) {
        if (!isNetworkAvailable()) {
            notifyFailed(callback, "No active internet connection. Operating offline.");
            return;
        }

        if (!sessionManager.isLoggedIn()) {
            notifyFailed(callback, "User not logged in. Sync paused.");
            return;
        }

        if (isSyncing.getAndSet(true)) {
            Log.d(TAG, "Sync already in progress. Skipping duplicate request.");
            return;
        }

        notifyStarted(callback);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int pushedRecords = 0;

                    // ==========================================
                    // 1. PUSH: Upload Local Offline Changes
                    // ==========================================
                    SyncPayload payload = databaseManager.getUnsyncedRecords();
                    if (payload != null && payload.totalRecordCount() > 0) {
                        Log.i(TAG, "Pushing " + payload.totalRecordCount() + " pending records to server...");
                        try {
                            Response<SyncPushResponse> pushResponse = apiService.pushData(payload).execute();
                            if (pushResponse.isSuccessful() && pushResponse.body() != null) {
                                SyncPushResponse body = pushResponse.body();
                                if (body.isSuccess()) {
                                    databaseManager.markRecordsAsSynced(body.getSyncedUuids());
                                    pushedRecords = body.getSyncedUuids().size();
                                    if (body.getPlanCapabilities() != null) {
                                        sessionManager.updatePlanCapabilities(com.smartmess.android.model.PlanCapabilities.fromJson(body.getPlanCapabilities()));
                                        sessionManager.broadcastCapabilitiesUpdated(context);
                                    }
                                    Log.i(TAG, "Successfully pushed and acknowledged " + pushedRecords + " records.");
                                } else {
                                    Log.w(TAG, "Server reported push error: " + body.getMessage());
                                }
                            } else {
                                Log.e(TAG, "Push failed with HTTP code: " + pushResponse.code());
                            }
                        } catch (Exception pushEx) {
                            Log.e(TAG, "Exception during push sync: " + pushEx.getMessage());
                            // Do not abort pull; continue to pull if possible
                        }
                    } else {
                        Log.d(TAG, "No pending local records to push.");
                    }

                    // ==========================================
                    // 2. PULL: Fetch Cloud Delta Updates
                    // ==========================================
                    String lastSyncedAt = sessionManager.getLastSyncTimestamp();
                    Log.i(TAG, "Pulling cloud updates since: " + lastSyncedAt);

                    try {
                        Response<SyncPullResponse> pullResponse = apiService.pullData(lastSyncedAt).execute();
                        if (pullResponse.isSuccessful() && pullResponse.body() != null) {
                            SyncPullResponse pullData = pullResponse.body();
                            if (pullData.isSuccess()) {
                                databaseManager.applyPulledRecords(pullData);

                                if (pullData.getPlanCapabilities() != null) {
                                    sessionManager.updatePlanCapabilities(com.smartmess.android.model.PlanCapabilities.fromJson(pullData.getPlanCapabilities()));
                                    sessionManager.broadcastCapabilitiesUpdated(context);
                                }

                                Log.i(TAG, "Successfully applied pulled cloud updates.");
                                notifySuccess(callback, "Sync complete. Pushed " + pushedRecords + " records.");
                            } else {
                                notifyFailed(callback, "Pull failed: Server returned unsuccessful status.");
                            }
                        } else {
                            notifyFailed(callback, "Pull failed with HTTP " + pullResponse.code());
                        }
                    } catch (Exception pullEx) {
                        Log.e(TAG, "Exception during pull sync: " + pullEx.getMessage());
                        notifyFailed(callback, "Sync network error: " + pullEx.getMessage());
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error in sync routine: " + e.getMessage());
                    notifyFailed(callback, "Sync exception: " + e.getMessage());
                } finally {
                    isSyncing.set(false);
                }
            }
        });
    }

    public void triggerTwoWaySync(SyncCallback callback) {
        triggerSync(callback);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void notifyStarted(final SyncCallback callback) {
        if (callback != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSyncStarted();
                }
            });
        }
    }

    private void notifySuccess(final SyncCallback callback, final String message) {
        if (callback != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSyncSuccess(message);
                }
            });
        }
    }

    private void notifyFailed(final SyncCallback callback, final String error) {
        if (callback != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onSyncFailed(error);
                }
            });
        }
    }
}
