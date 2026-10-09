package com.smartmess.android.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.SQLiteContract.MealVacationEntry;
import com.smartmess.android.data.local.SQLiteContract.UserEntry;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.MealVacation;
import com.smartmess.android.utils.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MealVacationDao {

    private final DatabaseHelper dbHelper;

    public MealVacationDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertOrUpdate(MealVacation vacation) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MealVacationEntry.COL_UUID, vacation.getUuid() != null ? vacation.getUuid() : UUID.randomUUID().toString());
        values.put(MealVacationEntry.COL_MESS_ID, vacation.getMessId());
        values.put(MealVacationEntry.COL_USER_ID, vacation.getUserId());
        values.put(MealVacationEntry.COL_START_DATE, vacation.getStartDate());
        values.put(MealVacationEntry.COL_END_DATE, vacation.getEndDate());
        values.put(MealVacationEntry.COL_REASON, vacation.getReason());
        values.put(MealVacationEntry.COL_STATUS, vacation.getStatus());
        values.put(MealVacationEntry.COL_CREATED_AT, vacation.getCreatedAt() != null ? vacation.getCreatedAt() : DateTimeUtils.nowIso());
        values.put(MealVacationEntry.COL_UPDATED_AT, DateTimeUtils.nowIso());

        if (vacation.getId() > 0) {
            values.put(MealVacationEntry.COL_ID, vacation.getId());
            return db.insertWithOnConflict(MealVacationEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } else {
            return db.insert(MealVacationEntry.TABLE_NAME, null, values);
        }
    }

    public boolean isUserOnVacation(long messId, long userId, String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + MealVacationEntry.TABLE_NAME
                + " WHERE " + MealVacationEntry.COL_MESS_ID + " = ? AND "
                + MealVacationEntry.COL_USER_ID + " = ? AND "
                + MealVacationEntry.COL_STATUS + " = 'active' AND "
                + "? >= " + MealVacationEntry.COL_START_DATE + " AND ? <= " + MealVacationEntry.COL_END_DATE;
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(userId), date, date});
        boolean onVacation = false;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                onVacation = cursor.getInt(0) > 0;
            }
            cursor.close();
        }
        return onVacation;
    }

    public List<MealVacation> getActiveVacationsForUser(long messId, long userId) {
        List<MealVacation> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = MealVacationEntry.COL_MESS_ID + " = ? AND " + MealVacationEntry.COL_USER_ID + " = ? AND " + MealVacationEntry.COL_STATUS + " = 'active'";
        Cursor cursor = db.query(MealVacationEntry.TABLE_NAME, null, selection,
                new String[]{String.valueOf(messId), String.valueOf(userId)}, null, null,
                MealVacationEntry.COL_START_DATE + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToVacation(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<MealVacation> getAllVacationsForMess(long messId) {
        List<MealVacation> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT v.*, u." + UserEntry.COL_NAME + " AS user_name FROM " + MealVacationEntry.TABLE_NAME + " v "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON v." + MealVacationEntry.COL_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "WHERE v." + MealVacationEntry.COL_MESS_ID + " = ? "
                + "ORDER BY v." + MealVacationEntry.COL_START_DATE + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                MealVacation v = cursorToVacation(cursor);
                int nameCol = cursor.getColumnIndex("user_name");
                if (nameCol != -1 && !cursor.isNull(nameCol)) {
                    v.setUserName(cursor.getString(nameCol));
                }
                list.add(v);
            }
            cursor.close();
        }
        return list;
    }

    public int cancelVacation(long vacationId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MealVacationEntry.COL_STATUS, "cancelled");
        values.put(MealVacationEntry.COL_UPDATED_AT, DateTimeUtils.nowIso());
        return db.update(MealVacationEntry.TABLE_NAME, values, MealVacationEntry.COL_ID + " = ?", new String[]{String.valueOf(vacationId)});
    }

    /**
     * Iterates all days from startDate to endDate and locks all meals to 0.0 with is_locked = 1.
     * Prevents automated meal debit and reminders during the absence window.
     */
    public int autoLockMealsForVacation(long messId, long userId, String startDateStr, String endDateStr, MealDao mealDao) {
        int count = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date start = sdf.parse(startDateStr);
            Date end = sdf.parse(endDateStr);

            if (start == null || end == null || start.after(end)) return 0;

            Calendar cal = Calendar.getInstance();
            cal.setTime(start);

            while (!cal.getTime().after(end)) {
                String dateStr = sdf.format(cal.getTime());
                Meal existing = mealDao.getUserMealForDate(messId, userId, dateStr);

                if (existing != null) {
                    existing.setBreakfastCount(0.0);
                    existing.setLunchCount(0.0);
                    existing.setDinnerCount(0.0);
                    existing.setGuestMealCount(0.0);
                    existing.setIsLocked(1);
                    existing.setUpdatedAt(DateTimeUtils.nowIso());
                    mealDao.insertOrUpdate(existing);
                } else {
                    Meal newMeal = new Meal(
                            UUID.randomUUID().toString(),
                            messId,
                            userId,
                            dateStr,
                            0.0,
                            0.0,
                            0.0,
                            0.0
                    );
                    newMeal.setIsLocked(1);
                    newMeal.setCreatedAt(DateTimeUtils.nowIso());
                    newMeal.setUpdatedAt(DateTimeUtils.nowIso());
                    mealDao.insertOrUpdate(newMeal);
                }

                count++;
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        } catch (Exception e) {
            android.util.Log.e("MealVacationDao", "Error auto-locking meals for vacation: " + e.getMessage(), e);
        }
        return count;
    }

    private MealVacation cursorToVacation(Cursor cursor) {
        MealVacation v = new MealVacation();
        v.setId(cursor.getLong(cursor.getColumnIndexOrThrow(MealVacationEntry.COL_ID)));
        v.setUuid(cursor.getString(cursor.getColumnIndexOrThrow(MealVacationEntry.COL_UUID)));
        v.setMessId(cursor.getLong(cursor.getColumnIndexOrThrow(MealVacationEntry.COL_MESS_ID)));
        v.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(MealVacationEntry.COL_USER_ID)));
        v.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow(MealVacationEntry.COL_START_DATE)));
        v.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(MealVacationEntry.COL_END_DATE)));
        v.setReason(cursor.getString(cursor.getColumnIndexOrThrow(MealVacationEntry.COL_REASON)));
        v.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(MealVacationEntry.COL_STATUS)));
        v.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(MealVacationEntry.COL_CREATED_AT)));
        v.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(MealVacationEntry.COL_UPDATED_AT)));
        return v;
    }
}
