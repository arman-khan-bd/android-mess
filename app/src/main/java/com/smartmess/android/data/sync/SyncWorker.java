package com.smartmess.android.data.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SyncManager syncManager = new SyncManager(context);

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = new boolean[]{false};

        syncManager.triggerTwoWaySync(new SyncManager.SyncCallback() {
            @Override
            public void onSyncStarted() {}

            @Override
            public void onSyncSuccess(String message) {
                success[0] = true;
                latch.countDown();
            }

            @Override
            public void onSyncFailed(String error) {
                success[0] = false;
                latch.countDown();
            }
        });

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return Result.retry();
        }

        return success[0] ? Result.success() : Result.retry();
    }

    public static void schedulePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueue(syncRequest);
    }
}
