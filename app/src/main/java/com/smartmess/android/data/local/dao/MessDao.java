package com.smartmess.android.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.SQLiteContract.MessEntry;
import com.smartmess.android.model.Mess;

public class MessDao {
    private final DatabaseHelper dbHelper;

    public MessDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertOrUpdate(Mess mess) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MessEntry.COL_UUID, mess.getUuid());
        values.put(MessEntry.COL_NAME, mess.getName());
        values.put(MessEntry.COL_INVITE_CODE, mess.getInviteCode());
        values.put(MessEntry.COL_BILLING_CYCLE, mess.getBillingCycle());
        values.put(MessEntry.COL_CYCLE_START_DAY, mess.getCycleStartDay());
        values.put(MessEntry.COL_MEAL_CUTOFF_TIME, mess.getMealCutoffTime());
        values.put(MessEntry.COL_PER_SMS_COST, mess.getPerSmsCost());
        values.put(MessEntry.COL_TARGET_MEAL_BUDGET, mess.getTargetMealBudget());
        values.put(MessEntry.COL_CURRENT_PLAN_ID, mess.getCurrentPlanId());
        values.put(MessEntry.COL_PLAN_EXPIRES_AT, mess.getPlanExpiresAt());
        values.put(MessEntry.COL_CREATED_AT, mess.getCreatedAt());
        values.put(MessEntry.COL_UPDATED_AT, mess.getUpdatedAt());

        return db.insertWithOnConflict(MessEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Mess getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(MessEntry.TABLE_NAME, null,
                MessEntry.COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);
        Mess mess = null;
        if (cursor != null && cursor.moveToFirst()) {
            mess = cursorToMess(cursor);
            cursor.close();
        }
        return mess;
    }

    public Mess getByUuid(String uuid) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(MessEntry.TABLE_NAME, null,
                MessEntry.COL_UUID + " = ?", new String[]{uuid},
                null, null, null);
        Mess mess = null;
        if (cursor != null && cursor.moveToFirst()) {
            mess = cursorToMess(cursor);
            cursor.close();
        }
        return mess;
    }

    public Mess getFirstMess() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(MessEntry.TABLE_NAME, null, null, null, null, null, MessEntry.COL_ID + " ASC", "1");
        Mess mess = null;
        if (cursor != null && cursor.moveToFirst()) {
            mess = cursorToMess(cursor);
            cursor.close();
        }
        return mess;
    }

    public int updateCutoffTime(long messId, String newTime) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MessEntry.COL_MEAL_CUTOFF_TIME, newTime);
        return db.update(MessEntry.TABLE_NAME, values, MessEntry.COL_ID + " = ?", new String[]{String.valueOf(messId)});
    }

    public int updateSmsCost(long messId, double cost) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MessEntry.COL_PER_SMS_COST, cost);
        return db.update(MessEntry.TABLE_NAME, values, MessEntry.COL_ID + " = ?", new String[]{String.valueOf(messId)});
    }

    public int updateTargetBudget(long messId, double budget) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MessEntry.COL_TARGET_MEAL_BUDGET, budget);
        return db.update(MessEntry.TABLE_NAME, values, MessEntry.COL_ID + " = ?", new String[]{String.valueOf(messId)});
    }

    private Mess cursorToMess(Cursor cursor) {
        Mess mess = new Mess();
        mess.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MessEntry.COL_ID)));
        mess.setUuid(cursor.getString(cursor.getColumnIndexOrThrow(MessEntry.COL_UUID)));
        mess.setName(cursor.getString(cursor.getColumnIndexOrThrow(MessEntry.COL_NAME)));
        mess.setInviteCode(cursor.getString(cursor.getColumnIndexOrThrow(MessEntry.COL_INVITE_CODE)));
        mess.setBillingCycle(cursor.getString(cursor.getColumnIndexOrThrow(MessEntry.COL_BILLING_CYCLE)));
        mess.setCycleStartDay(cursor.getInt(cursor.getColumnIndexOrThrow(MessEntry.COL_CYCLE_START_DAY)));
        mess.setMealCutoffTime(cursor.getString(cursor.getColumnIndexOrThrow(MessEntry.COL_MEAL_CUTOFF_TIME)));
        mess.setPerSmsCost(cursor.getDouble(cursor.getColumnIndexOrThrow(MessEntry.COL_PER_SMS_COST)));
        int budgetCol = cursor.getColumnIndex(MessEntry.COL_TARGET_MEAL_BUDGET);
        if (budgetCol != -1 && !cursor.isNull(budgetCol)) {
            mess.setTargetMealBudget(cursor.getDouble(budgetCol));
        } else {
            mess.setTargetMealBudget(70.0);
        }
        int planCol = cursor.getColumnIndexOrThrow(MessEntry.COL_CURRENT_PLAN_ID);
        if (!cursor.isNull(planCol)) {
            mess.setCurrentPlanId(cursor.getLong(planCol));
        }
        mess.setPlanExpiresAt(cursor.getString(cursor.getColumnIndexOrThrow(MessEntry.COL_PLAN_EXPIRES_AT)));
        mess.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(MessEntry.COL_CREATED_AT)));
        mess.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(MessEntry.COL_UPDATED_AT)));
        return mess;
    }
}
