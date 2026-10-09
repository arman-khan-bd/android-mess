package com.smartmess.android.notification;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.AppNotificationDao;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.NotificationListResponse;
import com.smartmess.android.model.AppNotification;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

public class PushNotificationSyncWorker extends Worker {

    private static final String TAG = "PushNotifWorker";
    private static final String PERIODIC_WORK_TAG = "smartmess_push_sync_periodic";
    private static final String ONE_TIME_WORK_TAG = "smartmess_push_sync_onetime";

    public PushNotificationSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SessionManager sessionManager = new SessionManager(context);

        if (!sessionManager.isLoggedIn() || sessionManager.getMessId() <= 0) {
            return Result.success();
        }

        try {
            ApiService apiService = ApiClient.getApiService(context);
            Response<NotificationListResponse> response = apiService.getOnlineNotifications(null).execute();

            if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                List<NotificationListResponse.NotificationItem> items = response.body().getData();
                AppNotificationDao dao = new AppNotificationDao(DatabaseHelper.getInstance(context));

                long myUserId = sessionManager.getUserId();

                for (NotificationListResponse.NotificationItem item : items) {
                    // Check if relevant to this user
                    Long targetUser = item.getTargetUserId();
                    if (targetUser != null && targetUser > 0 && targetUser != myUserId) {
                        continue;
                    }

                    String uuid = item.getUuid() != null ? item.getUuid() : ("srv_" + item.getId());
                    if (!dao.existsByUuid(uuid)) {
                        AppNotification localNotif = new AppNotification(
                                uuid,
                                item.getMessId() > 0 ? item.getMessId() : sessionManager.getMessId(),
                                targetUser != null ? targetUser : 0,
                                item.getTitle(),
                                item.getMessage(),
                                item.getType(),
                                item.getChannel(),
                                item.getCreatedAt() != null ? item.getCreatedAt() : DateTimeUtils.nowIso()
                        );
                        long rowId = dao.insert(localNotif);

                        // Trigger heads-up system tray notification
                        PushNotificationManager.showSystemNotification(
                                context,
                                rowId > 0 ? rowId : item.getId(),
                                item.getTitle(),
                                item.getMessage(),
                                item.getType(),
                                item.getChannel()
                        );
                    }
                }
            }
            return Result.success();
        } catch (Exception e) {
            Log.w(TAG, "PushNotificationSyncWorker sync error: " + e.getMessage());
            return Result.retry();
        }
    }

    /**
     * Schedules periodic background polling (runs every 15 minutes, the WorkManager minimum).
     */
    public static void schedulePeriodicSync(Context context) {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    PushNotificationSyncWorker.class,
                    15, TimeUnit.MINUTES
            )
                    .setConstraints(constraints)
                    .build();

            WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_TAG,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
            );
            Log.i(TAG, "Push notification periodic sync scheduled successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling periodic sync: " + e.getMessage(), e);
        }
    }

    /**
     * Runs an immediate background check (e.g. on app launch, boot, or entering background).
     */
    public static void runImmediateSync(Context context) {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PushNotificationSyncWorker.class)
                    .setConstraints(constraints)
                    .build();

            WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                    ONE_TIME_WORK_TAG,
                    ExistingWorkPolicy.REPLACE,
                    request
            );
        } catch (Exception e) {
            Log.e(TAG, "Error running immediate push sync: " + e.getMessage(), e);
        }
    }
}
