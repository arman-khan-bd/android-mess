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
                int bSessCol = expCursor.getColumnIndex(ExpenseEntry.COL_BAZAR_SESSION_UUID);
                if (bSessCol >= 0 && !expCursor.isNull(bSessCol)) {
                    e.setBazarSessionUuid(expCursor.getString(bSessCol));
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
                int isFlexIdx = mealCursor.getColumnIndex(MealEntry.COL_IS_FLEXIBLE);
                if (isFlexIdx >= 0) m.setIsFlexible(mealCursor.getInt(isFlexIdx));
                int mealTypeIdx = mealCursor.getColumnIndex(MealEntry.COL_MEAL_TYPE);
                if (mealTypeIdx >= 0) m.setMealType(mealCursor.getString(mealTypeIdx));
                int rawNameIdx = mealCursor.getColumnIndex(MealEntry.COL_RAW_BAZAR_NAME);
                if (rawNameIdx >= 0) m.setRawBazarName(mealCursor.getString(rawNameIdx));
                int rawAmtIdx = mealCursor.getColumnIndex(MealEntry.COL_RAW_BAZAR_AMOUNT);
                if (rawAmtIdx >= 0) m.setRawBazarAmount(mealCursor.getDouble(rawAmtIdx));
                int sharedPriceIdx = mealCursor.getColumnIndex(MealEntry.COL_SHARED_PRICE);
                if (sharedPriceIdx >= 0) m.setSharedPrice(mealCursor.getDouble(sharedPriceIdx));
                int bazarIdIdx = mealCursor.getColumnIndex(MealEntry.COL_BAZAR_ID);
                if (bazarIdIdx >= 0 && !mealCursor.isNull(bazarIdIdx)) m.setBazarId(mealCursor.getLong(bazarIdIdx));
                int bSessMealIdx = mealCursor.getColumnIndex(MealEntry.COL_BAZAR_SESSION_UUID);
                if (bSessMealIdx >= 0 && !mealCursor.isNull(bSessMealIdx)) m.setBazarSessionUuid(mealCursor.getString(bSessMealIdx));
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

        SyncPayload payload = new SyncPayload(messUuid, pendingDeposits, pendingExpenses, pendingMeals, pendingSms);
        try {
            long messId = sessionManager.getMessId();
            if (messId > 0) {
                Mess currentMess = new com.smartmess.android.data.local.dao.MessDao(dbHelper).getById(messId);
                if (currentMess != null) {
                    if ((messUuid == null || messUuid.trim().isEmpty()) && currentMess.getUuid() != null && !currentMess.getUuid().trim().isEmpty()) {
                        payload.setMessUuid(currentMess.getUuid().trim());
                        sessionManager.setMessUuid(currentMess.getUuid().trim());
                    }
                    if (currentMess.getCurrentPlanId() > 1) {
                        payload.setCurrentPlanId(currentMess.getCurrentPlanId());
                        payload.setPlanTier("pro");
                    } else if (sessionManager.isPro()) {
                        payload.setCurrentPlanId(2L);
                        payload.setPlanTier("pro");
                    }
                } else if (sessionManager.isPro()) {
                    payload.setCurrentPlanId(2L);
                    payload.setPlanTier("pro");
                }
            } else if (sessionManager.isPro()) {
                payload.setCurrentPlanId(2L);
                payload.setPlanTier("pro");
            }
        } catch (Exception ignored) {}

        return payload;
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
            long currentMessId = sessionManager.getMessId();
            long currentUserId = sessionManager.getUserId();
            String todayDate = com.smartmess.android.utils.DateTimeUtils.currentDate();

            // 1. Mess & Plan
            if (data.getPlan() != null) {
                SaasPlan plan = data.getPlan();
                ContentValues pv = new ContentValues();
                if (plan.getId() > 0) pv.put(SaasPlanEntry.COL_ID, plan.getId());
                pv.put(SaasPlanEntry.COL_NAME, plan.getName() != null ? plan.getName() : "SmartMess Plan");
                pv.put(SaasPlanEntry.COL_PRICE, plan.getPrice());
                pv.put(SaasPlanEntry.COL_DURATION_IN_DAYS, plan.getDurationInDays());
                pv.put(SaasPlanEntry.COL_FEATURES, plan.getFeaturesJson());
                pv.put(SaasPlanEntry.COL_STATUS, plan.getStatus());
                db.insertWithOnConflict(SaasPlanEntry.TABLE_NAME, null, pv, SQLiteDatabase.CONFLICT_REPLACE);
            }

            if (data.getMess() != null) {
                Mess mess = data.getMess();
                ContentValues mv = new ContentValues();
                if (mess.getId() > 0) {
                    mv.put(MessEntry.COL_ID, mess.getId());
                    if (currentMessId <= 0) {
                        currentMessId = mess.getId();
                        sessionManager.setMessId(mess.getId());
                    }
                }
                String mUuid = (mess.getUuid() != null && !mess.getUuid().trim().isEmpty())
                        ? mess.getUuid().trim() : (sessionManager.getMessUuid() != null && !sessionManager.getMessUuid().trim().isEmpty() ? sessionManager.getMessUuid() : java.util.UUID.randomUUID().toString());
                mv.put(MessEntry.COL_UUID, mUuid);
                if (sessionManager.getMessUuid() == null || sessionManager.getMessUuid().trim().isEmpty()) {
                    sessionManager.setMessUuid(mUuid);
                }

                String mName = (mess.getName() != null && !mess.getName().trim().isEmpty()) ? mess.getName().trim() : sessionManager.getMessName();
                mv.put(MessEntry.COL_NAME, mName);
                mv.put(MessEntry.COL_INVITE_CODE, mess.getInviteCode());
                mv.put(MessEntry.COL_BILLING_CYCLE, mess.getBillingCycle() != null ? mess.getBillingCycle() : "monthly");
                mv.put(MessEntry.COL_CYCLE_START_DAY, mess.getCycleStartDay() > 0 ? mess.getCycleStartDay() : 1);
                mv.put(MessEntry.COL_MEAL_CUTOFF_TIME, mess.getMealCutoffTime() != null ? mess.getMealCutoffTime() : "22:00:00");
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
                    String uUuid = (u.getUuid() != null && !u.getUuid().trim().isEmpty()) ? u.getUuid().trim() : java.util.UUID.randomUUID().toString();
                    uv.put(UserEntry.COL_UUID, uUuid);
                    uv.put(UserEntry.COL_MESS_ID, u.getMessId() > 0 ? u.getMessId() : currentMessId);
                    uv.put(UserEntry.COL_NAME, (u.getName() != null && !u.getName().trim().isEmpty()) ? u.getName().trim() : "Member");
                    uv.put(UserEntry.COL_PHONE, u.getPhone() != null ? u.getPhone().trim() : "");
                    uv.put(UserEntry.COL_ROLE, u.getRole() != null ? u.getRole() : "member");
                    uv.put(UserEntry.COL_STATUS, u.getStatus() != null ? u.getStatus() : "active");
                    if (u.getAvatarUrl() != null) uv.put(UserEntry.COL_AVATAR_URL, u.getAvatarUrl());
                    db.insertWithOnConflict(UserEntry.TABLE_NAME, null, uv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // 3. Deposits
            if (data.getDeposits() != null) {
                for (Deposit d : data.getDeposits()) {
                    ContentValues dv = new ContentValues();
                    if (d.getId() > 0) dv.put(DepositEntry.COL_ID, d.getId());
                    String dUuid = (d.getUuid() != null && !d.getUuid().trim().isEmpty()) ? d.getUuid().trim() : java.util.UUID.randomUUID().toString();
                    dv.put(DepositEntry.COL_UUID, dUuid);
                    dv.put(DepositEntry.COL_MESS_ID, d.getMessId() > 0 ? d.getMessId() : currentMessId);
                    dv.put(DepositEntry.COL_USER_ID, d.getUserId() > 0 ? d.getUserId() : currentUserId);
                    dv.put(DepositEntry.COL_AMOUNT, d.getAmount());
                    String depDate = (d.getDepositDate() != null && !d.getDepositDate().trim().isEmpty()) ? d.getDepositDate().trim() : todayDate;
                    dv.put(DepositEntry.COL_DEPOSIT_DATE, depDate);
                    dv.put(DepositEntry.COL_NOTE, d.getNote());
                    dv.put(DepositEntry.COL_SYNC_STATUS, 1);
                    if (d.getCreatedAt() != null) dv.put(DepositEntry.COL_CREATED_AT, d.getCreatedAt());
                    if (d.getUpdatedAt() != null) dv.put(DepositEntry.COL_UPDATED_AT, d.getUpdatedAt());
                    db.insertWithOnConflict(DepositEntry.TABLE_NAME, null, dv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // 4. Expenses
            if (data.getExpenses() != null) {
                for (Expense e : data.getExpenses()) {
                    ContentValues ev = new ContentValues();
                    if (e.getId() > 0) ev.put(ExpenseEntry.COL_ID, e.getId());
                    String eUuid = (e.getUuid() != null && !e.getUuid().trim().isEmpty()) ? e.getUuid().trim() : java.util.UUID.randomUUID().toString();
                    ev.put(ExpenseEntry.COL_UUID, eUuid);
                    ev.put(ExpenseEntry.COL_MESS_ID, e.getMessId() > 0 ? e.getMessId() : currentMessId);
                    ev.put(ExpenseEntry.COL_BUYER_USER_ID, e.getBuyerUserId() > 0 ? e.getBuyerUserId() : currentUserId);
                    String cat = (e.getExpenseCategory() != null && !e.getExpenseCategory().trim().isEmpty()) ? e.getExpenseCategory().trim() : Expense.CAT_RAW_MEAL;
                    ev.put(ExpenseEntry.COL_EXPENSE_CATEGORY, cat);
                    ev.put(ExpenseEntry.COL_AMOUNT, e.getAmount());
                    String expDate = (e.getExpenseDate() != null && !e.getExpenseDate().trim().isEmpty()) ? e.getExpenseDate().trim() : todayDate;
                    ev.put(ExpenseEntry.COL_EXPENSE_DATE, expDate);
                    ev.put(ExpenseEntry.COL_TITLE, (e.getTitle() != null && !e.getTitle().trim().isEmpty()) ? e.getTitle().trim() : "Expense");
                    ev.put(ExpenseEntry.COL_VOUCHER_IMAGE_URL, e.getVoucherImageUrl());
                    String split = (e.getSplitType() != null && !e.getSplitType().trim().isEmpty()) ? e.getSplitType().trim() : Expense.SPLIT_MEAL_DEPENDENT;
                    ev.put(ExpenseEntry.COL_SPLIT_TYPE, split);
                    if (e.getTargetUserId() != null && e.getTargetUserId() > 0) {
                        ev.put(ExpenseEntry.COL_TARGET_USER_ID, e.getTargetUserId());
                    } else {
                        ev.putNull(ExpenseEntry.COL_TARGET_USER_ID);
                    }
                    if (e.getBazarSessionUuid() != null) {
                        ev.put(ExpenseEntry.COL_BAZAR_SESSION_UUID, e.getBazarSessionUuid());
                    }
                    ev.put(ExpenseEntry.COL_SYNC_STATUS, 1);
                    if (e.getCreatedAt() != null) ev.put(ExpenseEntry.COL_CREATED_AT, e.getCreatedAt());
                    if (e.getUpdatedAt() != null) ev.put(ExpenseEntry.COL_UPDATED_AT, e.getUpdatedAt());
                    db.insertWithOnConflict(ExpenseEntry.TABLE_NAME, null, ev, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // 5. Meals
            if (data.getMeals() != null) {
                for (Meal m : data.getMeals()) {
                    ContentValues mv = new ContentValues();
                    if (m.getId() > 0) mv.put(MealEntry.COL_ID, m.getId());
                    String mUuid = (m.getUuid() != null && !m.getUuid().trim().isEmpty()) ? m.getUuid().trim() : java.util.UUID.randomUUID().toString();
                    mv.put(MealEntry.COL_UUID, mUuid);
                    mv.put(MealEntry.COL_MESS_ID, m.getMessId() > 0 ? m.getMessId() : currentMessId);
                    mv.put(MealEntry.COL_USER_ID, m.getUserId() > 0 ? m.getUserId() : currentUserId);
                    String mDate = (m.getMealDate() != null && !m.getMealDate().trim().isEmpty()) ? m.getMealDate().trim() : todayDate;
                    mv.put(MealEntry.COL_MEAL_DATE, mDate);
                    mv.put(MealEntry.COL_BREAKFAST_COUNT, m.getBreakfastCount());
                    mv.put(MealEntry.COL_LUNCH_COUNT, m.getLunchCount());
                    mv.put(MealEntry.COL_DINNER_COUNT, m.getDinnerCount());
                    mv.put(MealEntry.COL_GUEST_MEAL_COUNT, m.getGuestMealCount());
                    mv.put(MealEntry.COL_IS_LOCKED, m.getIsLocked());
                    mv.put(MealEntry.COL_IS_FLEXIBLE, m.getIsFlexible());
                    mv.put(MealEntry.COL_MEAL_TYPE, m.getMealType());
                    mv.put(MealEntry.COL_RAW_BAZAR_NAME, m.getRawBazarName());
                    mv.put(MealEntry.COL_RAW_BAZAR_AMOUNT, m.getRawBazarAmount());
                    mv.put(MealEntry.COL_SHARED_PRICE, m.getSharedPrice());
                    if (m.getBazarId() > 0) {
                        mv.put(MealEntry.COL_BAZAR_ID, m.getBazarId());
                    } else {
                        mv.putNull(MealEntry.COL_BAZAR_ID);
                    }
                    if (m.getBazarSessionUuid() != null) {
                        mv.put(MealEntry.COL_BAZAR_SESSION_UUID, m.getBazarSessionUuid());
                    }
                    mv.put(MealEntry.COL_SYNC_STATUS, 1);
                    if (m.getCreatedAt() != null) mv.put(MealEntry.COL_CREATED_AT, m.getCreatedAt());
                    if (m.getUpdatedAt() != null) mv.put(MealEntry.COL_UPDATED_AT, m.getUpdatedAt());
                    db.insertWithOnConflict(MealEntry.TABLE_NAME, null, mv, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            // 6. SMS Logs
            if (data.getSmsLogs() != null) {
                for (SmsLog s : data.getSmsLogs()) {
                    ContentValues sv = new ContentValues();
                    if (s.getId() > 0) sv.put(SmsLogEntry.COL_ID, s.getId());
                    String sUuid = (s.getUuid() != null && !s.getUuid().trim().isEmpty()) ? s.getUuid().trim() : java.util.UUID.randomUUID().toString();
                    sv.put(SmsLogEntry.COL_UUID, sUuid);
                    sv.put(SmsLogEntry.COL_MESS_ID, s.getMessId() > 0 ? s.getMessId() : currentMessId);
                    sv.put(SmsLogEntry.COL_SENDER_USER_ID, s.getSenderUserId() > 0 ? s.getSenderUserId() : currentUserId);
                    sv.put(SmsLogEntry.COL_RECIPIENT_PHONE, s.getRecipientPhone() != null ? s.getRecipientPhone().trim() : "");
                    if (s.getTargetUserId() != null && s.getTargetUserId() > 0) {
                        sv.put(SmsLogEntry.COL_TARGET_USER_ID, s.getTargetUserId());
                    } else {
                        sv.putNull(SmsLogEntry.COL_TARGET_USER_ID);
                    }
                    sv.put(SmsLogEntry.COL_MESSAGE_CONTENT, s.getMessageContent() != null ? s.getMessageContent() : "");
                    sv.put(SmsLogEntry.COL_COST_APPLIED, s.getCostApplied());
                    sv.put(SmsLogEntry.COL_DISPATCH_TYPE, s.getDispatchType() != null ? s.getDispatchType() : "device_sim");
                    sv.put(SmsLogEntry.COL_DELIVERY_STATUS, s.getDeliveryStatus() != null ? s.getDeliveryStatus() : "sent");
                    sv.put(SmsLogEntry.COL_SYNC_STATUS, 1);
                    if (s.getCreatedAt() != null) sv.put(SmsLogEntry.COL_CREATED_AT, s.getCreatedAt());
                    if (s.getUpdatedAt() != null) sv.put(SmsLogEntry.COL_UPDATED_AT, s.getUpdatedAt());
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

    /**
     * Updates an expense record's voucher_image_url in SQLite with the uploaded cloud URL.
     */
    public void updateExpenseVoucherUrl(String uuid, String cloudVoucherUrl) {
        if (uuid == null || cloudVoucherUrl == null) return;
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(ExpenseEntry.COL_VOUCHER_IMAGE_URL, cloudVoucherUrl);
            db.update(ExpenseEntry.TABLE_NAME, values, ExpenseEntry.COL_UUID + " = ?", new String[]{uuid});
        } catch (Exception e) {
            android.util.Log.e("DatabaseManager", "Error updating expense voucher URL: " + e.getMessage());
        }
    }
}
