package com.smartmess.android.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.SQLiteContract.DepositEntry;
import com.smartmess.android.data.local.SQLiteContract.ExpenseEntry;
import com.smartmess.android.data.local.SQLiteContract.MealEntry;
import com.smartmess.android.data.local.SQLiteContract.MessEntry;
import com.smartmess.android.data.local.SQLiteContract.SaasPlanEntry;
import com.smartmess.android.data.local.SQLiteContract.SmsLogEntry;
import com.smartmess.android.data.local.SQLiteContract.UserEntry;
import com.smartmess.android.data.remote.dto.SyncPayload;
import com.smartmess.android.data.remote.dto.SyncPullResponse;
import com.smartmess.android.model.Deposit;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.SaasPlan;
import com.smartmess.android.model.SmsLog;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static DatabaseManager sInstance;
    private final DatabaseHelper dbHelper;
    private final SessionManager sessionManager;

    public static synchronized DatabaseManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseManager(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.sessionManager = new SessionManager(context);
    }

    /**
     * Queries SQLite where sync_status = 0 across all tables
     * and constructs the SyncPayload object.
     */
    public SyncPayload getUnsyncedRecords() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String messUuid = sessionManager.getMessUuid();

        List<Deposit> pendingDeposits = new ArrayList<>();
        List<Expense> pendingExpenses = new ArrayList<>();
        List<Meal> pendingMeals = new ArrayList<>();
        List<SmsLog> pendingSms = new ArrayList<>();

        // 1. Deposits with sync_status = 0
        Cursor depCursor = db.query(DepositEntry.TABLE_NAME, null,
                DepositEntry.COL_SYNC_STATUS + " = 0", null, null, null, null);
        if (depCursor != null) {
            while (depCursor.moveToNext()) {
                Deposit d = new Deposit();
                d.setId(depCursor.getLong(depCursor.getColumnIndexOrThrow(DepositEntry.COL_ID)));
                d.setUuid(depCursor.getString(depCursor.getColumnIndexOrThrow(DepositEntry.COL_UUID)));
                d.setMessId(depCursor.getLong(depCursor.getColumnIndexOrThrow(DepositEntry.COL_MESS_ID)));
                d.setUserId(depCursor.getLong(depCursor.getColumnIndexOrThrow(DepositEntry.COL_USER_ID)));
                d.setAmount(depCursor.getDouble(depCursor.getColumnIndexOrThrow(DepositEntry.COL_AMOUNT)));
                d.setDepositDate(depCursor.getString(depCursor.getColumnIndexOrThrow(DepositEntry.COL_DEPOSIT_DATE)));
                d.setNote(depCursor.getString(depCursor.getColumnIndexOrThrow(DepositEntry.COL_NOTE)));
                d.setSyncStatus(0);
                d.setCreatedAt(depCursor.getString(depCursor.getColumnIndexOrThrow(DepositEntry.COL_CREATED_AT)));
                d.setUpdatedAt(depCursor.getString(depCursor.getColumnIndexOrThrow(DepositEntry.COL_UPDATED_AT)));
                pendingDeposits.add(d);
            }
            depCursor.close();
        }

        // 2. Expenses with sync_status = 0
        Cursor expCursor = db.query(ExpenseEntry.TABLE_NAME, null,
                ExpenseEntry.COL_SYNC_STATUS + " = 0", null, null, null, null);
        if (expCursor != null) {
            while (expCursor.moveToNext()) {
                Expense e = new Expense();
                e.setId(expCursor.getLong(expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_ID)));
                e.setUuid(expCursor.getString(expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_UUID)));
                e.setMessId(expCursor.getLong(expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_MESS_ID)));
                e.setBuyerUserId(expCursor.getLong(expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_BUYER_USER_ID)));
                e.setExpenseCategory(expCursor.getString(expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_EXPENSE_CATEGORY)));
                e.setAmount(expCursor.getDouble(expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_AMOUNT)));
                e.setExpenseDate(expCursor.getString(expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_EXPENSE_DATE)));
                e.setTitle(expCursor.getString(expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_TITLE)));
                e.setVoucherImageUrl(expCursor.getString(expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_VOUCHER_IMAGE_URL)));
                e.setSplitType(expCursor.getString(expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_SPLIT_TYPE)));
                int targetCol = expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_TARGET_USER_ID);
                if (!expCursor.isNull(targetCol)) {
                    e.setTargetUserId(expCursor.getLong(targetCol));
                }
                e.setSyncStatus(0);
                e.setCreatedAt(expCursor.getString(expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_CREATED_AT)));
                e.setUpdatedAt(expCursor.getString(expCursor.getColumnIndexOrThrow(ExpenseEntry.COL_UPDATED_AT)));
                pendingExpenses.add(e);
            }
            expCursor.close();
        }

        // 3. Meals with sync_status = 0
        Cursor mealCursor = db.query(MealEntry.TABLE_NAME, null,
                MealEntry.COL_SYNC_STATUS + " = 0", null, null, null, null);
        if (mealCursor != null) {
            while (mealCursor.moveToNext()) {
                Meal m = new Meal();
                m.setId(mealCursor.getLong(mealCursor.getColumnIndexOrThrow(MealEntry.COL_ID)));
                m.setUuid(mealCursor.getString(mealCursor.getColumnIndexOrThrow(MealEntry.COL_UUID)));
                m.setMessId(mealCursor.getLong(mealCursor.getColumnIndexOrThrow(MealEntry.COL_MESS_ID)));
                m.setUserId(mealCursor.getLong(mealCursor.getColumnIndexOrThrow(MealEntry.COL_USER_ID)));
                m.setMealDate(mealCursor.getString(mealCursor.getColumnIndexOrThrow(MealEntry.COL_MEAL_DATE)));
                m.setBreakfastCount(mealCursor.getDouble(mealCursor.getColumnIndexOrThrow(MealEntry.COL_BREAKFAST_COUNT)));
                m.setLunchCount(mealCursor.getDouble(mealCursor.getColumnIndexOrThrow(MealEntry.COL_LUNCH_COUNT)));
                m.setDinnerCount(mealCursor.getDouble(mealCursor.getColumnIndexOrThrow(MealEntry.COL_DINNER_COUNT)));
                m.setGuestMealCount(mealCursor.getDouble(mealCursor.getColumnIndexOrThrow(MealEntry.COL_GUEST_MEAL_COUNT)));
                m.setIsLocked(mealCursor.getInt(mealCursor.getColumnIndexOrThrow(MealEntry.COL_IS_LOCKED)));
                m.setSyncStatus(0);
                m.setCreatedAt(mealCursor.getString(mealCursor.getColumnIndexOrThrow(MealEntry.COL_CREATED_AT)));
                m.setUpdatedAt(mealCursor.getString(mealCursor.getColumnIndexOrThrow(MealEntry.COL_UPDATED_AT)));
                pendingMeals.add(m);
            }
            mealCursor.close();
        }

        // 4. SMS Logs with sync_status = 0
        Cursor smsCursor = db.query(SmsLogEntry.TABLE_NAME, null,
                SmsLogEntry.COL_SYNC_STATUS + " = 0", null, null, null, null);
        if (smsCursor != null) {
            while (smsCursor.moveToNext()) {
                SmsLog s = new SmsLog();
                s.setId(smsCursor.getLong(smsCursor.getColumnIndexOrThrow(SmsLogEntry.COL_ID)));
                s.setUuid(smsCursor.getString(smsCursor.getColumnIndexOrThrow(SmsLogEntry.COL_UUID)));
                s.setMessId(smsCursor.getLong(smsCursor.getColumnIndexOrThrow(SmsLogEntry.COL_MESS_ID)));
                s.setSenderUserId(smsCursor.getLong(smsCursor.getColumnIndexOrThrow(SmsLogEntry.COL_SENDER_USER_ID)));
                s.setRecipientPhone(smsCursor.getString(smsCursor.getColumnIndexOrThrow(SmsLogEntry.COL_RECIPIENT_PHONE)));
                int targetCol = smsCursor.getColumnIndexOrThrow(SmsLogEntry.COL_TARGET_USER_ID);
                if (!smsCursor.isNull(targetCol)) {
                    s.setTargetUserId(smsCursor.getLong(targetCol));
                }
                s.setMessageContent(smsCursor.getString(smsCursor.getColumnIndexOrThrow(SmsLogEntry.COL_MESSAGE_CONTENT)));
                s.setCostApplied(smsCursor.getDouble(smsCursor.getColumnIndexOrThrow(SmsLogEntry.COL_COST_APPLIED)));
                s.setDispatchType(smsCursor.getString(smsCursor.getColumnIndexOrThrow(SmsLogEntry.COL_DISPATCH_TYPE)));
                s.setDeliveryStatus(smsCursor.getString(smsCursor.getColumnIndexOrThrow(SmsLogEntry.COL_DELIVERY_STATUS)));
                s.setSyncStatus(0);
                s.setCreatedAt(smsCursor.getString(smsCursor.getColumnIndexOrThrow(SmsLogEntry.COL_CREATED_AT)));
                s.setUpdatedAt(smsCursor.getString(smsCursor.getColumnIndexOrThrow(SmsLogEntry.COL_UPDATED_AT)));
                pendingSms.add(s);
            }
            smsCursor.close();
        }

        return new SyncPayload(messUuid, pendingDeposits, pendingExpenses, pendingMeals, pendingSms);
    }

    /**
     * Updates sync_status = 1 for confirmed synced UUIDs.
     */
    public void markRecordsAsSynced(List<String> syncedUuids) {
        if (syncedUuids == null || syncedUuids.isEmpty()) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("sync_status", 1);

            for (String uuid : syncedUuids) {
                db.update(DepositEntry.TABLE_NAME, values, DepositEntry.COL_UUID + " = ?", new String[]{uuid});
                db.update(ExpenseEntry.TABLE_NAME, values, ExpenseEntry.COL_UUID + " = ?", new String[]{uuid});
                db.update(MealEntry.TABLE_NAME, values, MealEntry.COL_UUID + " = ?", new String[]{uuid});
                db.update(SmsLogEntry.TABLE_NAME, values, SmsLogEntry.COL_UUID + " = ?", new String[]{uuid});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Uses SQLiteDatabase.insertWithOnConflict(..., CONFLICT_REPLACE)
     * inside an atomic transaction to write pulled records to local storage.
     */
    public void applyPulledRecords(SyncPullResponse data) {
        if (data == null) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Mess & Plan
            if (data.getPlan() != null) {
                SaasPlan plan = data.getPlan();
                ContentValues pv = new ContentValues();
                if (plan.getId() > 0) pv.put(SaasPlanEntry.COL_ID, plan.getId());
                pv.put(SaasPlanEntry.COL_NAME, plan.getName());
                pv.put(SaasPlanEntry.COL_PRICE, plan.getPrice());
                pv.put(SaasPlanEntry.COL_DURATION_IN_DAYS, plan.getDurationInDays());
                pv.put(SaasPlanEntry.COL_FEATURES, plan.getFeaturesJson());
                pv.put(SaasPlanEntry.COL_STATUS, plan.getStatus());
                db.insertWithOnConflict(SaasPlanEntry.TABLE_NAME, null, pv, SQLiteDatabase.CONFLICT_REPLACE);
            }

            if (data.getMess() != null) {
                Mess mess = data.getMess();
                ContentValues mv = new ContentValues();
                if (mess.getId() > 0) mv.put(MessEntry.COL_ID, mess.getId());
                mv.put(MessEntry.COL_UUID, mess.getUuid());
                mv.put(MessEntry.COL_NAME, mess.getName());
                mv.put(MessEntry.COL_INVITE_CODE, mess.getInviteCode());
                mv.put(MessEntry.COL_BILLING_CYCLE, mess.getBillingCycle());
                mv.put(MessEntry.COL_CYCLE_START_DAY, mess.getCycleStartDay());
                mv.put(MessEntry.COL_MEAL_CUTOFF_TIME, mess.getMealCutoffTime());
                mv.put(MessEntry.COL_PER_SMS_COST, mess.getPerSmsCost());
                mv.put(MessEntry.COL_CURRENT_PLAN_ID, mess.getCurrentPlanId());
                mv.put(MessEntry.COL_PLAN_EXPIRES_AT, mess.getPlanExpiresAt());
                if (mess.getTargetMealBudget() > 0) {
                    mv.put(MessEntry.COL_TARGET_MEAL_BUDGET, mess.getTargetMealBudget());
                }
                db.insertWithOnConflict(MessEntry.TABLE_NAME, null, mv, SQLiteDatabase.CONFLICT_REPLACE);
            }

            // 2. Members / Users
            if (data.getUsers() != null) {
                for (User u : data.getUsers()) {
                    ContentValues uv = new ContentValues();
                    if (u.getId() > 0) uv.put(UserEntry.COL_ID, u.getId());
                    uv.put(UserEntry.COL_UUID, u.getUuid());
                    uv.put(UserEntry.COL_MESS_ID, u.getMessId());
                    uv.put(UserEntry.COL_NAME, u.getName());
                    uv.put(UserEntry.COL_PHONE, u.getPhone());
                    uv.put(UserEntry.COL_ROLE, u.getRole());
                    uv.put(UserEntry.COL_STATUS, u.getStatus());
                    if (u.getAvatarUrl() != null) uv.put(UserEntry.COL_AVATAR_URL, u.getAvatarUrl());
                    db.insertWithOnConflict(UserEntry.TABLE_NAME, null, uv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // 3. Deposits
            if (data.getDeposits() != null) {
                for (Deposit d : data.getDeposits()) {
                    ContentValues dv = new ContentValues();
                    if (d.getId() > 0) dv.put(DepositEntry.COL_ID, d.getId());
                    dv.put(DepositEntry.COL_UUID, d.getUuid());
                    dv.put(DepositEntry.COL_MESS_ID, d.getMessId());
                    dv.put(DepositEntry.COL_USER_ID, d.getUserId());
                    dv.put(DepositEntry.COL_AMOUNT, d.getAmount());
                    dv.put(DepositEntry.COL_DEPOSIT_DATE, d.getDepositDate());
                    dv.put(DepositEntry.COL_NOTE, d.getNote());
                    dv.put(DepositEntry.COL_SYNC_STATUS, 1);
                    db.insertWithOnConflict(DepositEntry.TABLE_NAME, null, dv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // 4. Expenses
            if (data.getExpenses() != null) {
                for (Expense e : data.getExpenses()) {
                    ContentValues ev = new ContentValues();
                    if (e.getId() > 0) ev.put(ExpenseEntry.COL_ID, e.getId());
                    ev.put(ExpenseEntry.COL_UUID, e.getUuid());
                    ev.put(ExpenseEntry.COL_MESS_ID, e.getMessId());
                    ev.put(ExpenseEntry.COL_BUYER_USER_ID, e.getBuyerUserId());
                    ev.put(ExpenseEntry.COL_EXPENSE_CATEGORY, e.getExpenseCategory());
                    ev.put(ExpenseEntry.COL_AMOUNT, e.getAmount());
                    ev.put(ExpenseEntry.COL_EXPENSE_DATE, e.getExpenseDate());
                    ev.put(ExpenseEntry.COL_TITLE, e.getTitle());
                    ev.put(ExpenseEntry.COL_VOUCHER_IMAGE_URL, e.getVoucherImageUrl());
                    ev.put(ExpenseEntry.COL_SPLIT_TYPE, e.getSplitType());
                    if (e.getTargetUserId() != null) {
                        ev.put(ExpenseEntry.COL_TARGET_USER_ID, e.getTargetUserId());
                    } else {
                        ev.putNull(ExpenseEntry.COL_TARGET_USER_ID);
                    }
                    ev.put(ExpenseEntry.COL_SYNC_STATUS, 1);
                    db.insertWithOnConflict(ExpenseEntry.TABLE_NAME, null, ev, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // 5. Meals
            if (data.getMeals() != null) {
                for (Meal m : data.getMeals()) {
                    ContentValues mv = new ContentValues();
                    if (m.getId() > 0) mv.put(MealEntry.COL_ID, m.getId());
                    mv.put(MealEntry.COL_UUID, m.getUuid());
                    mv.put(MealEntry.COL_MESS_ID, m.getMessId());
                    mv.put(MealEntry.COL_USER_ID, m.getUserId());
                    mv.put(MealEntry.COL_MEAL_DATE, m.getMealDate());
                    mv.put(MealEntry.COL_BREAKFAST_COUNT, m.getBreakfastCount());
                    mv.put(MealEntry.COL_LUNCH_COUNT, m.getLunchCount());
                    mv.put(MealEntry.COL_DINNER_COUNT, m.getDinnerCount());
                    mv.put(MealEntry.COL_GUEST_MEAL_COUNT, m.getGuestMealCount());
                    mv.put(MealEntry.COL_IS_LOCKED, m.getIsLocked());
                    mv.put(MealEntry.COL_SYNC_STATUS, 1);
                    db.insertWithOnConflict(MealEntry.TABLE_NAME, null, mv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // 6. SMS Logs
            if (data.getSmsLogs() != null) {
                for (SmsLog s : data.getSmsLogs()) {
                    ContentValues sv = new ContentValues();
                    if (s.getId() > 0) sv.put(SmsLogEntry.COL_ID, s.getId());
                    sv.put(SmsLogEntry.COL_UUID, s.getUuid());
                    sv.put(SmsLogEntry.COL_MESS_ID, s.getMessId());
                    sv.put(SmsLogEntry.COL_SENDER_USER_ID, s.getSenderUserId());
                    sv.put(SmsLogEntry.COL_RECIPIENT_PHONE, s.getRecipientPhone());
                    if (s.getTargetUserId() != null) {
                        sv.put(SmsLogEntry.COL_TARGET_USER_ID, s.getTargetUserId());
                    } else {
                        sv.putNull(SmsLogEntry.COL_TARGET_USER_ID);
                    }
                    sv.put(SmsLogEntry.COL_MESSAGE_CONTENT, s.getMessageContent());
                    sv.put(SmsLogEntry.COL_COST_APPLIED, s.getCostApplied());
                    sv.put(SmsLogEntry.COL_DISPATCH_TYPE, s.getDispatchType());
                    sv.put(SmsLogEntry.COL_DELIVERY_STATUS, s.getDeliveryStatus());
                    sv.put(SmsLogEntry.COL_SYNC_STATUS, 1);
                    db.insertWithOnConflict(SmsLogEntry.TABLE_NAME, null, sv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            db.setTransactionSuccessful();

            if (data.getServerTimestamp() != null && !data.getServerTimestamp().trim().isEmpty()) {
                sessionManager.setLastSyncTimestamp(data.getServerTimestamp());
            }
        } finally {
            db.endTransaction();
        }
    }
}
