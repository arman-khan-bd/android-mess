package com.smartmess.android.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.model.Mess;
import com.smartmess.android.utils.SessionManager;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Log.i(TAG, "Device rebooted or package updated. Re-registering daily meal reminder alarms...");

            SessionManager sessionManager = new SessionManager(context);
            if (sessionManager.isLoggedIn()) {
                DatabaseHelper helper = DatabaseHelper.getInstance(context);
                MessDao messDao = new MessDao(helper);
                Mess mess = messDao.getById(sessionManager.getMessId());
                String cutoff = mess != null ? mess.getMealCutoffTime() : "22:00:00";

                NotificationScheduler.scheduleDailyReminder(context, cutoff);
            }
        }
    }
}
