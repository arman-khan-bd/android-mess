package com.smartmess.android.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.SQLiteContract.ItemizedConsumptionEntry;
import com.smartmess.android.model.ItemizedConsumptionLog;

import java.util.ArrayList;
import java.util.List;

public class ItemizedConsumptionDao {

    private final DatabaseHelper dbHelper;

    public ItemizedConsumptionDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public long insertOrUpdate(ItemizedConsumptionLog log) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ItemizedConsumptionEntry.COL_UUID, log.getUuid());
        values.put(ItemizedConsumptionEntry.COL_MESS_ID, log.getMessId());
        values.put(ItemizedConsumptionEntry.COL_BAZAR_SESSION_UUID, log.getBazarSessionUuid());
        values.put(ItemizedConsumptionEntry.COL_BAZAR_ID, log.getBazarId());
        values.put(ItemizedConsumptionEntry.COL_MEAL_ID, log.getMealId());
        values.put(ItemizedConsumptionEntry.COL_USER_ID, log.getUserId());
        values.put(ItemizedConsumptionEntry.COL_USER_NAME, log.getUserName());
        values.put(ItemizedConsumptionEntry.COL_ITEM_NAME, log.getItemName());
        values.put(ItemizedConsumptionEntry.COL_ITEM_CATEGORY, log.getItemCategory());
        values.put(ItemizedConsumptionEntry.COL_ITEM_PRICE, log.getItemPrice());
        values.put(ItemizedConsumptionEntry.COL_MEAL_DATE, log.getMealDate());
        values.put(ItemizedConsumptionEntry.COL_MEAL_TYPE, log.getMealType());
        values.put(ItemizedConsumptionEntry.COL_ACTIVE_DINERS_COUNT, log.getActiveDinersCount());
        values.put(ItemizedConsumptionEntry.COL_COST_SHARE, log.getCostShare());
        values.put(ItemizedConsumptionEntry.COL_IS_CONSUMED, log.getIsConsumed());
        values.put(ItemizedConsumptionEntry.COL_CREATED_AT, log.getCreatedAt());

        long id = -1;
        if (log.getUuid() != null) {
            Cursor cursor = db.query(ItemizedConsumptionEntry.TABLE_NAME, new String[]{ItemizedConsumptionEntry.COL_ID},
                    ItemizedConsumptionEntry.COL_UUID + " = ?", new String[]{log.getUuid()},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                id = cursor.getLong(0);
                cursor.close();
                db.update(ItemizedConsumptionEntry.TABLE_NAME, values, ItemizedConsumptionEntry.COL_ID + " = ?", new String[]{String.valueOf(id)});
            } else {
                if (cursor != null) cursor.close();
                id = db.insert(ItemizedConsumptionEntry.TABLE_NAME, null, values);
            }
        } else {
            id = db.insert(ItemizedConsumptionEntry.TABLE_NAME, null, values);
        }
        log.setId(id);
        return id;
    }

    public void insertBatch(List<ItemizedConsumptionLog> logs) {
        if (logs == null || logs.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ItemizedConsumptionLog log : logs) {
                ContentValues values = new ContentValues();
                values.put(ItemizedConsumptionEntry.COL_UUID, log.getUuid());
                values.put(ItemizedConsumptionEntry.COL_MESS_ID, log.getMessId());
                values.put(ItemizedConsumptionEntry.COL_BAZAR_SESSION_UUID, log.getBazarSessionUuid());
                values.put(ItemizedConsumptionEntry.COL_BAZAR_ID, log.getBazarId());
                values.put(ItemizedConsumptionEntry.COL_MEAL_ID, log.getMealId());
                values.put(ItemizedConsumptionEntry.COL_USER_ID, log.getUserId());
                values.put(ItemizedConsumptionEntry.COL_USER_NAME, log.getUserName());
                values.put(ItemizedConsumptionEntry.COL_ITEM_NAME, log.getItemName());
                values.put(ItemizedConsumptionEntry.COL_ITEM_CATEGORY, log.getItemCategory());
                values.put(ItemizedConsumptionEntry.COL_ITEM_PRICE, log.getItemPrice());
                values.put(ItemizedConsumptionEntry.COL_MEAL_DATE, log.getMealDate());
                values.put(ItemizedConsumptionEntry.COL_MEAL_TYPE, log.getMealType());
                values.put(ItemizedConsumptionEntry.COL_ACTIVE_DINERS_COUNT, log.getActiveDinersCount());
                values.put(ItemizedConsumptionEntry.COL_COST_SHARE, log.getCostShare());
                values.put(ItemizedConsumptionEntry.COL_IS_CONSUMED, log.getIsConsumed());
                values.put(ItemizedConsumptionEntry.COL_CREATED_AT, log.getCreatedAt());
                db.insert(ItemizedConsumptionEntry.TABLE_NAME, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteByBazarSessionUuid(String sessionUuid) {
        if (sessionUuid == null) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(ItemizedConsumptionEntry.TABLE_NAME,
                ItemizedConsumptionEntry.COL_BAZAR_SESSION_UUID + " = ?",
                new String[]{sessionUuid});
    }

    public void insertOrUpdateBatch(List<ItemizedConsumptionLog> logs) {
        insertBatch(logs);
    }

    public void deleteByMealDetails(long messId, String mealDate, String mealType) {
        deleteByDateAndMealType(messId, mealDate, mealType);
    }

    public void deleteByDateAndMealType(long messId, String mealDate, String mealType) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(ItemizedConsumptionEntry.TABLE_NAME,
                ItemizedConsumptionEntry.COL_MESS_ID + " = ? AND " +
                ItemizedConsumptionEntry.COL_MEAL_DATE + " = ? AND " +
                ItemizedConsumptionEntry.COL_MEAL_TYPE + " = ?",
                new String[]{String.valueOf(messId), mealDate, mealType});
    }

    public List<ItemizedConsumptionLog> getByBazarSessionUuid(String sessionUuid) {
        List<ItemizedConsumptionLog> list = new ArrayList<>();
        if (sessionUuid == null) return list;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(ItemizedConsumptionEntry.TABLE_NAME, null,
                ItemizedConsumptionEntry.COL_BAZAR_SESSION_UUID + " = ?",
                new String[]{sessionUuid}, null, null, ItemizedConsumptionEntry.COL_ITEM_NAME + " ASC, " + ItemizedConsumptionEntry.COL_USER_NAME + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToLog(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<ItemizedConsumptionLog> getLogsForUser(long messId, long userId, String startDate, String endDate) {
        List<ItemizedConsumptionLog> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(ItemizedConsumptionEntry.TABLE_NAME, null,
                ItemizedConsumptionEntry.COL_MESS_ID + " = ? AND " +
                ItemizedConsumptionEntry.COL_USER_ID + " = ? AND " +
                ItemizedConsumptionEntry.COL_MEAL_DATE + " BETWEEN ? AND ?",
                new String[]{String.valueOf(messId), String.valueOf(userId), startDate, endDate},
                null, null, ItemizedConsumptionEntry.COL_MEAL_DATE + " DESC, " + ItemizedConsumptionEntry.COL_ID + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToLog(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<ItemizedConsumptionLog> getLogsForMess(long messId, String startDate, String endDate) {
        List<ItemizedConsumptionLog> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(ItemizedConsumptionEntry.TABLE_NAME, null,
                ItemizedConsumptionEntry.COL_MESS_ID + " = ? AND " +
                ItemizedConsumptionEntry.COL_MEAL_DATE + " BETWEEN ? AND ?",
                new String[]{String.valueOf(messId), startDate, endDate},
                null, null, ItemizedConsumptionEntry.COL_MEAL_DATE + " DESC, " + ItemizedConsumptionEntry.COL_ID + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToLog(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public double getTotalCostShareForUser(long messId, long userId, String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM(" + ItemizedConsumptionEntry.COL_COST_SHARE + ") FROM " + ItemizedConsumptionEntry.TABLE_NAME
                + " WHERE " + ItemizedConsumptionEntry.COL_MESS_ID + " = ? AND "
                + ItemizedConsumptionEntry.COL_USER_ID + " = ? AND "
                + ItemizedConsumptionEntry.COL_IS_CONSUMED + " = 1 AND "
                + ItemizedConsumptionEntry.COL_MEAL_DATE + " BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(userId), startDate, endDate});
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public double getTotalItemizedCostForMess(long messId, String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM(" + ItemizedConsumptionEntry.COL_COST_SHARE + ") FROM " + ItemizedConsumptionEntry.TABLE_NAME
                + " WHERE " + ItemizedConsumptionEntry.COL_MESS_ID + " = ? AND "
                + ItemizedConsumptionEntry.COL_IS_CONSUMED + " = 1 AND "
                + ItemizedConsumptionEntry.COL_MEAL_DATE + " BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), startDate, endDate});
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public ItemizedConsumptionLog cursorToLog(Cursor cursor) {
        ItemizedConsumptionLog log = new ItemizedConsumptionLog();
        int idIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_ID);
        if (idIdx >= 0) log.setId(cursor.getLong(idIdx));

        int uuidIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_UUID);
        if (uuidIdx >= 0) log.setUuid(cursor.getString(uuidIdx));

        int messIdIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_MESS_ID);
        if (messIdIdx >= 0) log.setMessId(cursor.getLong(messIdIdx));

        int sessIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_BAZAR_SESSION_UUID);
        if (sessIdx >= 0) log.setBazarSessionUuid(cursor.getString(sessIdx));

        int bIdIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_BAZAR_ID);
        if (bIdIdx >= 0) log.setBazarId(cursor.getLong(bIdIdx));

        int mIdIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_MEAL_ID);
        if (mIdIdx >= 0) log.setMealId(cursor.getLong(mIdIdx));

        int uIdIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_USER_ID);
        if (uIdIdx >= 0) log.setUserId(cursor.getLong(uIdIdx));

        int uNameIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_USER_NAME);
        if (uNameIdx >= 0) log.setUserName(cursor.getString(uNameIdx));

        int iNameIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_ITEM_NAME);
        if (iNameIdx >= 0) log.setItemName(cursor.getString(iNameIdx));

        int iCatIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_ITEM_CATEGORY);
        if (iCatIdx >= 0) log.setItemCategory(cursor.getString(iCatIdx));

        int iPriceIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_ITEM_PRICE);
        if (iPriceIdx >= 0) log.setItemPrice(cursor.getDouble(iPriceIdx));

        int mDateIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_MEAL_DATE);
        if (mDateIdx >= 0) log.setMealDate(cursor.getString(mDateIdx));

        int mTypeIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_MEAL_TYPE);
        if (mTypeIdx >= 0) log.setMealType(cursor.getString(mTypeIdx));

        int dinCountIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_ACTIVE_DINERS_COUNT);
        if (dinCountIdx >= 0) log.setActiveDinersCount(cursor.getInt(dinCountIdx));

        int shareIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_COST_SHARE);
        if (shareIdx >= 0) log.setCostShare(cursor.getDouble(shareIdx));

        int isConsIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_IS_CONSUMED);
        if (isConsIdx >= 0) log.setIsConsumed(cursor.getInt(isConsIdx));

        int createdIdx = cursor.getColumnIndex(ItemizedConsumptionEntry.COL_CREATED_AT);
        if (createdIdx >= 0) log.setCreatedAt(cursor.getString(createdIdx));

        return log;
    }
}
