package com.smartmess.android.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.SQLiteContract.MealRequestEntry;
import com.smartmess.android.data.local.SQLiteContract.UserEntry;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.MealRequest;
import com.smartmess.android.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MealRequestDao {

    private final DatabaseHelper dbHelper;

    public MealRequestDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertOrUpdate(MealRequest request) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MealRequestEntry.COL_UUID, request.getUuid() != null ? request.getUuid() : UUID.randomUUID().toString());
        values.put(MealRequestEntry.COL_MESS_ID, request.getMessId());
        values.put(MealRequestEntry.COL_USER_ID, request.getUserId());
        values.put(MealRequestEntry.COL_REQUEST_DATE, request.getRequestDate());
        values.put(MealRequestEntry.COL_BREAKFAST_COUNT, request.getBreakfastCount());
        values.put(MealRequestEntry.COL_LUNCH_COUNT, request.getLunchCount());
        values.put(MealRequestEntry.COL_DINNER_COUNT, request.getDinnerCount());
        values.put(MealRequestEntry.COL_GUEST_COUNT, request.getGuestCount());
        values.put(MealRequestEntry.COL_NOTE, request.getNote());
        values.put(MealRequestEntry.COL_STATUS, request.getStatus() != null ? request.getStatus() : "pending");
        values.put(MealRequestEntry.COL_CREATED_AT, request.getCreatedAt() != null ? request.getCreatedAt() : DateTimeUtils.nowIso());
        values.put(MealRequestEntry.COL_UPDATED_AT, DateTimeUtils.nowIso());

        if (request.getId() > 0) {
            values.put(MealRequestEntry.COL_ID, request.getId());
            db.update(MealRequestEntry.TABLE_NAME, values, MealRequestEntry.COL_ID + " = ?", new String[]{String.valueOf(request.getId())});
            return request.getId();
        } else {
            return db.insert(MealRequestEntry.TABLE_NAME, null, values);
        }
    }

    public List<MealRequest> getAllRequestsForMess(long messId) {
        List<MealRequest> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT r.*, u." + UserEntry.COL_NAME + " as user_name FROM " + MealRequestEntry.TABLE_NAME + " r "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON r." + MealRequestEntry.COL_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "WHERE r." + MealRequestEntry.COL_MESS_ID + " = ? "
                + "ORDER BY r." + MealRequestEntry.COL_REQUEST_DATE + " DESC, r." + MealRequestEntry.COL_ID + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                MealRequest req = cursorToMealRequest(cursor);
                int nameIdx = cursor.getColumnIndex("user_name");
                if (nameIdx >= 0) req.setUserName(cursor.getString(nameIdx));
                list.add(req);
            }
            cursor.close();
        }
        return list;
    }

    public List<MealRequest> getPendingRequestsForMess(long messId) {
        List<MealRequest> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT r.*, u." + UserEntry.COL_NAME + " as user_name FROM " + MealRequestEntry.TABLE_NAME + " r "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON r." + MealRequestEntry.COL_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "WHERE r." + MealRequestEntry.COL_MESS_ID + " = ? AND r." + MealRequestEntry.COL_STATUS + " = 'pending' "
                + "ORDER BY r." + MealRequestEntry.COL_REQUEST_DATE + " ASC, r." + MealRequestEntry.COL_ID + " ASC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                MealRequest req = cursorToMealRequest(cursor);
                int nameIdx = cursor.getColumnIndex("user_name");
                if (nameIdx >= 0) req.setUserName(cursor.getString(nameIdx));
                list.add(req);
            }
            cursor.close();
        }
        return list;
    }

    public List<MealRequest> getRequestsForUser(long messId, long userId) {
        List<MealRequest> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT r.*, u." + UserEntry.COL_NAME + " as user_name FROM " + MealRequestEntry.TABLE_NAME + " r "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON r." + MealRequestEntry.COL_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "WHERE r." + MealRequestEntry.COL_MESS_ID + " = ? AND r." + MealRequestEntry.COL_USER_ID + " = ? "
                + "ORDER BY r." + MealRequestEntry.COL_REQUEST_DATE + " DESC, r." + MealRequestEntry.COL_ID + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(userId)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                MealRequest req = cursorToMealRequest(cursor);
                int nameIdx = cursor.getColumnIndex("user_name");
                if (nameIdx >= 0) req.setUserName(cursor.getString(nameIdx));
                list.add(req);
            }
            cursor.close();
        }
        return list;
    }

    public MealRequest getRequestForUserAndDate(long messId, long userId, String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT r.*, u." + UserEntry.COL_NAME + " as user_name FROM " + MealRequestEntry.TABLE_NAME + " r "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON r." + MealRequestEntry.COL_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "WHERE r." + MealRequestEntry.COL_MESS_ID + " = ? AND r." + MealRequestEntry.COL_USER_ID + " = ? AND r." + MealRequestEntry.COL_REQUEST_DATE + " = ? "
                + "ORDER BY r." + MealRequestEntry.COL_ID + " DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(userId), date});
        MealRequest req = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                req = cursorToMealRequest(cursor);
                int nameIdx = cursor.getColumnIndex("user_name");
                if (nameIdx >= 0) req.setUserName(cursor.getString(nameIdx));
            }
            cursor.close();
        }
        return req;
    }

    public int updateStatus(long requestId, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MealRequestEntry.COL_STATUS, status);
        values.put(MealRequestEntry.COL_UPDATED_AT, DateTimeUtils.nowIso());
        return db.update(MealRequestEntry.TABLE_NAME, values, MealRequestEntry.COL_ID + " = ?", new String[]{String.valueOf(requestId)});
    }

    public boolean approveAndApplyToMeal(MealRequest request, MealDao mealDao) {
        if (request == null || mealDao == null) return false;
        updateStatus(request.getId(), "approved");

        Meal existingMeal = mealDao.getUserMealForDate(request.getMessId(), request.getUserId(), request.getRequestDate());
        String now = DateTimeUtils.nowIso();
        if (existingMeal != null) {
            existingMeal.setBreakfastCount(request.getBreakfastCount());
            existingMeal.setLunchCount(request.getLunchCount());
            existingMeal.setDinnerCount(request.getDinnerCount());
            existingMeal.setGuestMealCount(request.getGuestCount());
            existingMeal.setIsLocked(0);
            existingMeal.setUpdatedAt(now);
            mealDao.insertOrUpdate(existingMeal);
        } else {
            Meal newMeal = new Meal(
                    UUID.randomUUID().toString(),
                    request.getMessId(),
                    request.getUserId(),
                    request.getRequestDate(),
                    request.getBreakfastCount(),
                    request.getLunchCount(),
                    request.getDinnerCount(),
                    request.getGuestCount()
            );
            newMeal.setIsLocked(0);
            newMeal.setCreatedAt(now);
            newMeal.setUpdatedAt(now);
            mealDao.insertOrUpdate(newMeal);
        }
        return true;
    }

    private MealRequest cursorToMealRequest(Cursor cursor) {
        MealRequest req = new MealRequest();
        req.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_ID)));
        req.setUuid(cursor.getString(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_UUID)));
        req.setMessId(cursor.getLong(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_MESS_ID)));
        req.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_USER_ID)));
        req.setRequestDate(cursor.getString(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_REQUEST_DATE)));
        req.setBreakfastCount(cursor.getDouble(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_BREAKFAST_COUNT)));
        req.setLunchCount(cursor.getDouble(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_LUNCH_COUNT)));
        req.setDinnerCount(cursor.getDouble(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_DINNER_COUNT)));
        req.setGuestCount(cursor.getDouble(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_GUEST_COUNT)));
        req.setNote(cursor.getString(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_NOTE)));
        req.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_STATUS)));
        req.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_CREATED_AT)));
        req.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(MealRequestEntry.COL_UPDATED_AT)));
        return req;
    }
}
