package com.smartmess.android.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.SQLiteContract.SaasPlanEntry;
import com.smartmess.android.model.SaasPlan;

import java.util.ArrayList;
import java.util.List;

public class SaasPlanDao {
    private final DatabaseHelper dbHelper;

    public SaasPlanDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertOrUpdate(SaasPlan plan) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SaasPlanEntry.COL_NAME, plan.getName());
        values.put(SaasPlanEntry.COL_PRICE, plan.getPrice());
        values.put(SaasPlanEntry.COL_DURATION_IN_DAYS, plan.getDurationInDays());
        values.put(SaasPlanEntry.COL_FEATURES, plan.getFeaturesJson());
        values.put(SaasPlanEntry.COL_STATUS, plan.getStatus());
        values.put(SaasPlanEntry.COL_CREATED_AT, plan.getCreatedAt());
        values.put(SaasPlanEntry.COL_UPDATED_AT, plan.getUpdatedAt());

        if (plan.getId() > 0) {
            values.put(SaasPlanEntry.COL_ID, plan.getId());
            return db.insertWithOnConflict(SaasPlanEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } else {
            return db.insert(SaasPlanEntry.TABLE_NAME, null, values);
        }
    }

    public SaasPlan getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(SaasPlanEntry.TABLE_NAME, null,
                SaasPlanEntry.COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);
        SaasPlan plan = null;
        if (cursor != null && cursor.moveToFirst()) {
            plan = cursorToPlan(cursor);
            cursor.close();
        }
        return plan;
    }

    public List<SaasPlan> getAllActivePlans() {
        List<SaasPlan> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(SaasPlanEntry.TABLE_NAME, null,
                SaasPlanEntry.COL_STATUS + " = 1", null, null, null,
                SaasPlanEntry.COL_PRICE + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToPlan(cursor));
            }
            cursor.close();
        }
        return list;
    }

    private SaasPlan cursorToPlan(Cursor cursor) {
        SaasPlan plan = new SaasPlan();
        plan.setId(cursor.getLong(cursor.getColumnIndexOrThrow(SaasPlanEntry.COL_ID)));
        plan.setName(cursor.getString(cursor.getColumnIndexOrThrow(SaasPlanEntry.COL_NAME)));
        plan.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(SaasPlanEntry.COL_PRICE)));
        plan.setDurationInDays(cursor.getInt(cursor.getColumnIndexOrThrow(SaasPlanEntry.COL_DURATION_IN_DAYS)));
        plan.setFeaturesJson(cursor.getString(cursor.getColumnIndexOrThrow(SaasPlanEntry.COL_FEATURES)));
        plan.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow(SaasPlanEntry.COL_STATUS)));
        plan.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(SaasPlanEntry.COL_CREATED_AT)));
        plan.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(SaasPlanEntry.COL_UPDATED_AT)));
        return plan;
    }
}
