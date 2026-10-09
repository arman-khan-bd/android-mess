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

        // Install Global Uncaught Exception & Crash Telemetry Engine
        try {
            com.smartmess.android.engine.CrashTelemetryHandler.install(this);
        } catch (Throwable ignored) {}

        try {
            DatabaseHelper.getInstance(this);
        } catch (Throwable ignored) {}

        // Schedule periodic sync with cloud
        try {
            SyncWorker.schedulePeriodicSync(this);
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
        MessDao messDao = new MessDao(helper);
        UserDao userDao = new UserDao(helper);
        SaasPlanDao planDao = new SaasPlanDao(helper);
        MealDao mealDao = new MealDao(helper);
        ExpenseDao expenseDao = new ExpenseDao(helper);
        DepositDao depositDao = new DepositDao(helper);

        // Seed Plans
        if (planDao.getAllActivePlans().isEmpty()) {
            SaasPlan basic = new SaasPlan();
            basic.setName("Basic Tier");
            basic.setPrice(0.0);
            basic.setDurationInDays(30);
            basic.setFeaturesJson("{\"max_members\": 15, \"sms_sim\": true, \"sms_cloud\": false, \"ocr_receipt\": false, \"pdf_branding\": false, \"ad_free\": false}");
            basic.setStatus(1);
            planDao.insertOrUpdate(basic);

            SaasPlan enterprise = new SaasPlan();
            enterprise.setName("Enterprise Mess");
            enterprise.setPrice(499.0);
            enterprise.setDurationInDays(365);
            enterprise.setFeaturesJson("{\"max_members\": 50, \"sms_sim\": true, \"sms_cloud\": true, \"ocr_receipt\": true, \"pdf_branding\": true, \"ad_free\": true}");
            enterprise.setStatus(1);
            planDao.insertOrUpdate(enterprise);
        }

        // Seed default mess if none
        if (messDao.getFirstMess() == null) {
            String messUuid = UUID.randomUUID().toString();
            Mess defaultMess = new Mess(messUuid, "Green Paradise Mess", "MESS101", "monthly", 1, "22:00:00", 0.50);
            defaultMess.setCreatedAt(DateTimeUtils.nowIso());
            defaultMess.setUpdatedAt(DateTimeUtils.nowIso());
            long messId = messDao.insertOrUpdate(defaultMess);

            // Create Manager User
            User manager = new User(UUID.randomUUID().toString(), messId, "Tanvir Ahmed (Manager)", "01711000001", "manager", "active");
            manager.setPassword("123456");
            manager.setCreatedAt(DateTimeUtils.nowIso());
            manager.setUpdatedAt(DateTimeUtils.nowIso());
            long managerId = userDao.insertOrUpdate(manager);

            // Create Bazar Boy User
            User assistant = new User(UUID.randomUUID().toString(), messId, "Rafiqul Islam (Bazar Boy)", "01711000002", "assistant", "active");
            assistant.setPassword("123456");
            assistant.setCreatedAt(DateTimeUtils.nowIso());
            assistant.setUpdatedAt(DateTimeUtils.nowIso());
            long assistantId = userDao.insertOrUpdate(assistant);

            // Create General Member User
            User member = new User(UUID.randomUUID().toString(), messId, "Sabbir Hossain", "01711000003", "member", "active");
            member.setPassword("123456");
            member.setCreatedAt(DateTimeUtils.nowIso());
            member.setUpdatedAt(DateTimeUtils.nowIso());
            long memberId = userDao.insertOrUpdate(member);

            // Seed sample deposits
            String today = DateTimeUtils.currentDate();
            depositDao.insertOrUpdate(new Deposit(UUID.randomUUID().toString(), messId, managerId, 3000.0, today, "Initial Deposit"));
            depositDao.insertOrUpdate(new Deposit(UUID.randomUUID().toString(), messId, assistantId, 3000.0, today, "Initial Deposit"));
            depositDao.insertOrUpdate(new Deposit(UUID.randomUUID().toString(), messId, memberId, 2500.0, today, "Initial Deposit"));

            // Seed sample expenses (Dual Pool Demonstration)
            // 1. Raw Meal (Fish, Meat, Veg) -> Factored strictly into meal rate
            expenseDao.insertOrUpdate(new Expense(UUID.randomUUID().toString(), messId, assistantId, Expense.CAT_RAW_MEAL, 1250.0, today, "Fish & Fresh Vegetables", Expense.SPLIT_MEAL_DEPENDENT));
            // 2. Shared Food (Oil, Salt, Gas, Onion) -> Split equally among all active members
            expenseDao.insertOrUpdate(new Expense(UUID.randomUUID().toString(), messId, managerId, Expense.CAT_SHARED_FOOD, 600.0, today, "5L Soybean Oil & Spices", Expense.SPLIT_ALL_EQUAL));
            // 3. Asset & Utility (Cook Salary, Wi-Fi, Bulbs) -> Split equally
            expenseDao.insertOrUpdate(new Expense(UUID.randomUUID().toString(), messId, managerId, Expense.CAT_UTILITY_ASSET, 900.0, today, "Wi-Fi Monthly Bill", Expense.SPLIT_ALL_EQUAL));

            // Seed sample meals for today
            mealDao.insertOrUpdate(new Meal(UUID.randomUUID().toString(), messId, managerId, today, 1.0, 1.0, 1.0, 0.0));
            mealDao.insertOrUpdate(new Meal(UUID.randomUUID().toString(), messId, assistantId, today, 1.0, 1.0, 1.0, 0.0));
            mealDao.insertOrUpdate(new Meal(UUID.randomUUID().toString(), messId, memberId, today, 0.5, 1.0, 1.0, 0.0));

            // Default auto-login to manager for effortless out-of-the-box demo
            SessionManager session = new SessionManager(this);
            if (!session.isLoggedIn()) {
                session.createSession(managerId, manager.getUuid(), manager.getName(), manager.getPhone(), manager.getRole(),
                        messId, defaultMess.getUuid(), defaultMess.getName(), "demo_offline_token");
            }
        }
    }
}
