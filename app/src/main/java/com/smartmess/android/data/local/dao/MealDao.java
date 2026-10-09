package com.smartmess.android.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.SQLiteContract.MealEntry;
import com.smartmess.android.data.local.SQLiteContract.UserEntry;
import com.smartmess.android.model.Meal;

import java.util.ArrayList;
import java.util.List;

public class MealDao {
    private final DatabaseHelper dbHelper;

    public MealDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertOrUpdate(Meal meal) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MealEntry.COL_UUID, meal.getUuid());
        values.put(MealEntry.COL_MESS_ID, meal.getMessId());
        values.put(MealEntry.COL_USER_ID, meal.getUserId());
        values.put(MealEntry.COL_MEAL_DATE, meal.getMealDate());
        values.put(MealEntry.COL_BREAKFAST_COUNT, meal.getBreakfastCount());
        values.put(MealEntry.COL_LUNCH_COUNT, meal.getLunchCount());
        values.put(MealEntry.COL_DINNER_COUNT, meal.getDinnerCount());
        values.put(MealEntry.COL_GUEST_MEAL_COUNT, meal.getGuestMealCount());
        values.put(MealEntry.COL_IS_LOCKED, meal.getIsLocked());
        values.put(MealEntry.COL_SYNC_STATUS, meal.getSyncStatus());
        values.put(MealEntry.COL_CREATED_AT, meal.getCreatedAt());
        values.put(MealEntry.COL_UPDATED_AT, meal.getUpdatedAt());

        // Check if meal exists for user and date
        Cursor cursor = db.query(MealEntry.TABLE_NAME, new String[]{MealEntry.COL_ID},
                MealEntry.COL_MESS_ID + " = ? AND " + MealEntry.COL_USER_ID + " = ? AND " + MealEntry.COL_MEAL_DATE + " = ?",
                new String[]{String.valueOf(meal.getMessId()), String.valueOf(meal.getUserId()), meal.getMealDate()},
                null, null, null);
        long id = -1;
        if (cursor != null && cursor.moveToFirst()) {
            id = cursor.getLong(0);
            cursor.close();
            db.update(MealEntry.TABLE_NAME, values, MealEntry.COL_ID + " = ?", new String[]{String.valueOf(id)});
        } else {
            if (cursor != null) cursor.close();
            id = db.insert(MealEntry.TABLE_NAME, null, values);
        }
        return id;
    }

    public Meal getUserMealForDate(long messId, long userId, String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(MealEntry.TABLE_NAME, null,
                MealEntry.COL_MESS_ID + " = ? AND " + MealEntry.COL_USER_ID + " = ? AND " + MealEntry.COL_MEAL_DATE + " = ?",
                new String[]{String.valueOf(messId), String.valueOf(userId), date},
                null, null, null);
        Meal meal = null;
        if (cursor != null && cursor.moveToFirst()) {
            meal = cursorToMeal(cursor);
            cursor.close();
        }
        return meal;
    }

    public List<Meal> getMealsForDate(long messId, String date) {
        List<Meal> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT m.*, u." + UserEntry.COL_NAME + " as user_name FROM " + MealEntry.TABLE_NAME + " m "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON m." + MealEntry.COL_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "WHERE m." + MealEntry.COL_MESS_ID + " = ? AND m." + MealEntry.COL_MEAL_DATE + " = ? "
                + "ORDER BY u." + UserEntry.COL_NAME + " ASC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), date});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Meal meal = cursorToMeal(cursor);
                int nameIdx = cursor.getColumnIndex("user_name");
                if (nameIdx >= 0) meal.setUserName(cursor.getString(nameIdx));
                list.add(meal);
            }
            cursor.close();
        }
        return list;
    }

    public double getTotalMessMeals(long messId, String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM(" + MealEntry.COL_BREAKFAST_COUNT + " + "
                + MealEntry.COL_LUNCH_COUNT + " + "
                + MealEntry.COL_DINNER_COUNT + " + "
                + MealEntry.COL_GUEST_MEAL_COUNT + ") FROM " + MealEntry.TABLE_NAME
                + " WHERE " + MealEntry.COL_MESS_ID + " = ? AND "
                + MealEntry.COL_MEAL_DATE + " BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), startDate, endDate});
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public double getTotalUserMeals(long messId, long userId, String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM(" + MealEntry.COL_BREAKFAST_COUNT + " + "
                + MealEntry.COL_LUNCH_COUNT + " + "
                + MealEntry.COL_DINNER_COUNT + " + "
                + MealEntry.COL_GUEST_MEAL_COUNT + ") FROM " + MealEntry.TABLE_NAME
                + " WHERE " + MealEntry.COL_MESS_ID + " = ? AND "
                + MealEntry.COL_USER_ID + " = ? AND "
                + MealEntry.COL_MEAL_DATE + " BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(userId), startDate, endDate});
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public List<Meal> getRecentMeals(long messId, int limit) {
        List<Meal> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT m.*, u." + UserEntry.COL_NAME + " as user_name FROM " + MealEntry.TABLE_NAME + " m "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON m." + MealEntry.COL_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "WHERE m." + MealEntry.COL_MESS_ID + " = ? "
                + "ORDER BY m." + MealEntry.COL_MEAL_DATE + " DESC LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(limit)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Meal meal = cursorToMeal(cursor);
                int nameIdx = cursor.getColumnIndex("user_name");
                if (nameIdx >= 0) meal.setUserName(cursor.getString(nameIdx));
                list.add(meal);
            }
            cursor.close();
        }
        return list;
    }

    public List<Meal> getPendingSync() {
        List<Meal> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(MealEntry.TABLE_NAME, null,
                MealEntry.COL_SYNC_STATUS + " = 0", null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToMeal(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public void markSynced(List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MealEntry.COL_SYNC_STATUS, 1);
        for (String uuid : uuids) {
            db.update(MealEntry.TABLE_NAME, values, MealEntry.COL_UUID + " = ?", new String[]{uuid});
        }
    }

    private Meal cursorToMeal(Cursor cursor) {
        Meal meal = new Meal();
        meal.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MealEntry.COL_ID)));
        meal.setUuid(cursor.getString(cursor.getColumnIndexOrThrow(MealEntry.COL_UUID)));
        meal.setMessId(cursor.getLong(cursor.getColumnIndexOrThrow(MealEntry.COL_MESS_ID)));
        meal.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(MealEntry.COL_USER_ID)));
        meal.setMealDate(cursor.getString(cursor.getColumnIndexOrThrow(MealEntry.COL_MEAL_DATE)));
        meal.setBreakfastCount(cursor.getDouble(cursor.getColumnIndexOrThrow(MealEntry.COL_BREAKFAST_COUNT)));
        meal.setLunchCount(cursor.getDouble(cursor.getColumnIndexOrThrow(MealEntry.COL_LUNCH_COUNT)));
        meal.setDinnerCount(cursor.getDouble(cursor.getColumnIndexOrThrow(MealEntry.COL_DINNER_COUNT)));
        meal.setGuestMealCount(cursor.getDouble(cursor.getColumnIndexOrThrow(MealEntry.COL_GUEST_MEAL_COUNT)));
        meal.setIsLocked(cursor.getInt(cursor.getColumnIndexOrThrow(MealEntry.COL_IS_LOCKED)));
        meal.setSyncStatus(cursor.getInt(cursor.getColumnIndexOrThrow(MealEntry.COL_SYNC_STATUS)));
        meal.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(MealEntry.COL_CREATED_AT)));
        meal.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(MealEntry.COL_UPDATED_AT)));
        return meal;
    }
}
