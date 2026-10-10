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
        if (user == null) return 0;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        if (user.getId() > 0) {
            values.put(UserEntry.COL_ID, user.getId());
        } else {
            User existing = null;
            if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                existing = getByPhone(user.getPhone().trim());
            }
            if (existing == null && user.getUuid() != null && !user.getUuid().trim().isEmpty()) {
                existing = getByUuid(user.getUuid().trim());
            }
            if (existing != null && existing.getId() > 0) {
                values.put(UserEntry.COL_ID, existing.getId());
                user.setId(existing.getId());
            }
        }

        String uuid = (user.getUuid() != null && !user.getUuid().trim().isEmpty())
                ? user.getUuid().trim() : java.util.UUID.randomUUID().toString();
        values.put(UserEntry.COL_UUID, uuid);
        values.put(UserEntry.COL_MESS_ID, user.getMessId() > 0 ? user.getMessId() : 1);
        values.put(UserEntry.COL_NAME, (user.getName() != null && !user.getName().trim().isEmpty())
                ? user.getName().trim() : "Member");
        values.put(UserEntry.COL_PHONE, user.getPhone() != null ? user.getPhone().trim() : "");
        values.put(UserEntry.COL_PASSWORD, user.getPassword());
        values.put(UserEntry.COL_ROLE, user.getRole() != null ? user.getRole() : "member");
        values.put(UserEntry.COL_STATUS, user.getStatus() != null ? user.getStatus() : "active");
        values.put(UserEntry.COL_AVATAR_URL, user.getAvatarUrl());
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

    public User getByUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(UserEntry.TABLE_NAME, null,
                UserEntry.COL_UUID + " = ?", new String[]{uuid},
                null, null, null);
        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        return user;
    }

    public List<User> getAllMembers(long messId) {
        return getAllMembersByMess(messId);
    }

    public List<User> getAllMembersByMess(long messId) {
        List<User> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(UserEntry.TABLE_NAME, null,
                UserEntry.COL_MESS_ID + " = ?",
                new String[]{String.valueOf(messId)},
                null, null, UserEntry.COL_NAME + " ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToUser(cursor));
            }
            cursor.close();
        }
        return list;
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

    public void updateRole(long userId, String newRole) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserEntry.COL_ROLE, newRole);
        values.put(UserEntry.COL_UPDATED_AT, com.smartmess.android.utils.DateTimeUtils.getCurrentDateTime());
        db.update(UserEntry.TABLE_NAME, values, UserEntry.COL_ID + " = ?", new String[]{String.valueOf(userId)});
    }

    public void updateStatus(long userId, String newStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserEntry.COL_STATUS, newStatus);
        values.put(UserEntry.COL_UPDATED_AT, com.smartmess.android.utils.DateTimeUtils.getCurrentDateTime());
        db.update(UserEntry.TABLE_NAME, values, UserEntry.COL_ID + " = ?", new String[]{String.valueOf(userId)});
    }

    public void updateRoleAndStatus(long userId, String newRole, String newStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (newRole != null) values.put(UserEntry.COL_ROLE, newRole);
        if (newStatus != null) values.put(UserEntry.COL_STATUS, newStatus);
        values.put(UserEntry.COL_UPDATED_AT, com.smartmess.android.utils.DateTimeUtils.getCurrentDateTime());
        db.update(UserEntry.TABLE_NAME, values, UserEntry.COL_ID + " = ?", new String[]{String.valueOf(userId)});
    }

    public void updateAvatar(long userId, String avatarUrl) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserEntry.COL_AVATAR_URL, avatarUrl);
        values.put(UserEntry.COL_UPDATED_AT, com.smartmess.android.utils.DateTimeUtils.getCurrentDateTime());
        db.update(UserEntry.TABLE_NAME, values, UserEntry.COL_ID + " = ?", new String[]{String.valueOf(userId)});
    }

    public void updateUserProfile(long userId, String name, String phone, String avatarUrl) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (name != null) values.put(UserEntry.COL_NAME, name);
        if (phone != null) values.put(UserEntry.COL_PHONE, phone);
        if (avatarUrl != null) values.put(UserEntry.COL_AVATAR_URL, avatarUrl);
        values.put(UserEntry.COL_UPDATED_AT, com.smartmess.android.utils.DateTimeUtils.getCurrentDateTime());
        db.update(UserEntry.TABLE_NAME, values, UserEntry.COL_ID + " = ?", new String[]{String.valueOf(userId)});
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
        int avatarCol = cursor.getColumnIndex(UserEntry.COL_AVATAR_URL);
        if (avatarCol >= 0 && !cursor.isNull(avatarCol)) {
            user.setAvatarUrl(cursor.getString(avatarCol));
        }
        user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COL_CREATED_AT)));
        user.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COL_UPDATED_AT)));
        return user;
    }
}
