package com.smartmess.android.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.SQLiteContract.UserEntry;
import com.smartmess.android.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private final DatabaseHelper dbHelper;

    public UserDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertOrUpdate(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserEntry.COL_UUID, user.getUuid());
        values.put(UserEntry.COL_MESS_ID, user.getMessId());
        values.put(UserEntry.COL_NAME, user.getName());
        values.put(UserEntry.COL_PHONE, user.getPhone());
        values.put(UserEntry.COL_PASSWORD, user.getPassword());
        values.put(UserEntry.COL_ROLE, user.getRole());
        values.put(UserEntry.COL_STATUS, user.getStatus());
        values.put(UserEntry.COL_CREATED_AT, user.getCreatedAt());
        values.put(UserEntry.COL_UPDATED_AT, user.getUpdatedAt());

        return db.insertWithOnConflict(UserEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public User getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(UserEntry.TABLE_NAME, null,
                UserEntry.COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);
        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        return user;
    }

    public User getByPhone(String phone) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(UserEntry.TABLE_NAME, null,
                UserEntry.COL_PHONE + " = ?", new String[]{phone},
                null, null, null);
        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        return user;
    }

    public List<User> getActiveMembersByMess(long messId) {
        List<User> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(UserEntry.TABLE_NAME, null,
                UserEntry.COL_MESS_ID + " = ? AND " + UserEntry.COL_STATUS + " = ?",
                new String[]{String.valueOf(messId), "active"},
                null, null, UserEntry.COL_NAME + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToUser(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public int countActiveMembers(long messId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + UserEntry.TABLE_NAME + " WHERE " + UserEntry.COL_MESS_ID + " = ? AND " + UserEntry.COL_STATUS + " = 'active'",
                new String[]{String.valueOf(messId)}
        );
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(UserEntry.COL_ID)));
        user.setUuid(cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COL_UUID)));
        user.setMessId(cursor.getLong(cursor.getColumnIndexOrThrow(UserEntry.COL_MESS_ID)));
        user.setName(cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COL_NAME)));
        user.setPhone(cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COL_PHONE)));
        user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COL_PASSWORD)));
        user.setRole(cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COL_ROLE)));
        user.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COL_STATUS)));
        user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COL_CREATED_AT)));
        user.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COL_UPDATED_AT)));
        return user;
    }
}
