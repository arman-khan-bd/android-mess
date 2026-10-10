package com.smartmess.android;

import android.app.Application;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.DepositDao;
import com.smartmess.android.data.local.dao.ExpenseDao;
import com.smartmess.android.data.local.dao.MealDao;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.SaasPlanDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.sync.SyncWorker;
import com.smartmess.android.model.Deposit;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.SaasPlan;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.UUID;

public class SmartMessApp extends Application {

    static {
        try {
            androidx.appcompat.app.AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        } catch (Throwable ignored) {}
    }

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(base);
        try {
            androidx.multidex.MultiDex.install(this);
        } catch (Throwable ignored) {}
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply Dark/Light Material Theme from Preferences or System
        try {
            com.smartmess.android.utils.ThemeManager.applyTheme(this);
        } catch (Throwable ignored) {}

        // Install Global Uncaught Exception & Crash Telemetry Engine
        try {
            com.smartmess.android.engine.CrashTelemetryHandler.install(this);
        } catch (Throwable ignored) {}

        // Install Google Play Services Security Provider for Android 6.0.1 (API 23) compatibility
        try {
            com.smartmess.android.utils.SecurityProviderHelper.installIfNeeded(this);
        } catch (Throwable ignored) {}

        try {
            DatabaseHelper.getInstance(this);
        } catch (Throwable ignored) {}

        // Schedule periodic sync with cloud
        try {
            SyncWorker.schedulePeriodicSync(this);
        } catch (Throwable ignored) {}

        // Initialize Push Notification Channels & Background Sync Worker
        try {
            com.smartmess.android.notification.PushNotificationManager.createNotificationChannels(this);
            com.smartmess.android.notification.PushNotificationSyncWorker.schedulePeriodicSync(this);
            com.smartmess.android.notification.PushNotificationSyncWorker.runImmediateSync(this);
        } catch (Throwable ignored) {}

        // Prepopulate demo mess & default plans if fresh database
        try {
            seedInitialDataIfEmpty();
        } catch (Throwable t) {
            android.util.Log.e("SmartMessApp", "Error during DB initial seed: " + t.getMessage(), t);
        }
    }

    private void seedInitialDataIfEmpty() {
        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        SaasPlanDao planDao = new SaasPlanDao(helper);

        // Seed default SaaS Plans if fresh database
        if (planDao.getAllActivePlans().isEmpty()) {
            SaasPlan basic = new SaasPlan();
            basic.setName("বেসিক টিয়ার");
            basic.setPrice(0.0);
            basic.setDurationInDays(30);
            basic.setFeaturesJson("{\"max_members\": 15, \"sms_sim\": true, \"sms_cloud\": false, \"ocr_receipt\": false, \"pdf_branding\": false, \"ad_free\": false}");
            basic.setStatus(1);
            planDao.insertOrUpdate(basic);

            SaasPlan enterprise = new SaasPlan();
            enterprise.setName("এন্টারপ্রাইজ মেস");
            enterprise.setPrice(499.0);
            enterprise.setDurationInDays(365);
            enterprise.setFeaturesJson("{\"max_members\": 50, \"sms_sim\": true, \"sms_cloud\": true, \"ocr_receipt\": true, \"pdf_branding\": true, \"ad_free\": true}");
            enterprise.setStatus(1);
            planDao.insertOrUpdate(enterprise);
        }
    }
}
