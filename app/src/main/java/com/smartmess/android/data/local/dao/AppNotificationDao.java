package com.smartmess.android.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.SQLiteContract.NotificationEntry;
import com.smartmess.android.model.AppNotification;
import com.smartmess.android.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AppNotificationDao {

    private final DatabaseHelper dbHelper;

    public AppNotificationDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insert(AppNotification notification) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NotificationEntry.COL_UUID, notification.getUuid() != null ? notification.getUuid() : UUID.randomUUID().toString());
        values.put(NotificationEntry.COL_MESS_ID, notification.getMessId());
        values.put(NotificationEntry.COL_USER_ID, notification.getUserId());
        values.put(NotificationEntry.COL_TITLE, notification.getTitle());
        values.put(NotificationEntry.COL_MESSAGE, notification.getMessage());
        values.put(NotificationEntry.COL_TYPE, notification.getType());
        values.put(NotificationEntry.COL_IS_READ, notification.isRead() ? 1 : 0);
        values.put(NotificationEntry.COL_CREATED_AT, notification.getCreatedAt() != null ? notification.getCreatedAt() : DateTimeUtils.nowIso());

        return db.insert(NotificationEntry.TABLE_NAME, null, values);
    }

    public List<AppNotification> getNotificationsForMess(long messId, int limit) {
        List<AppNotification> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = NotificationEntry.COL_MESS_ID + " = ?";
        String limitStr = limit > 0 ? String.valueOf(limit) : "50";

        Cursor cursor = db.query(NotificationEntry.TABLE_NAME, null, selection,
                new String[]{String.valueOf(messId)}, null, null,
                NotificationEntry.COL_CREATED_AT + " DESC", limitStr);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToNotification(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public int getUnreadCount(long messId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + NotificationEntry.TABLE_NAME
                + " WHERE " + NotificationEntry.COL_MESS_ID + " = ? AND " + NotificationEntry.COL_IS_READ + " = 0";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId)});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public int markAllAsRead(long messId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NotificationEntry.COL_IS_READ, 1);
        return db.update(NotificationEntry.TABLE_NAME, values, NotificationEntry.COL_MESS_ID + " = ?", new String[]{String.valueOf(messId)});
    }

    public int clearAll(long messId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(NotificationEntry.TABLE_NAME, NotificationEntry.COL_MESS_ID + " = ?", new String[]{String.valueOf(messId)});
    }

    private AppNotification cursorToNotification(Cursor cursor) {
        AppNotification n = new AppNotification();
        n.setId(cursor.getLong(cursor.getColumnIndexOrThrow(NotificationEntry.COL_ID)));
        n.setUuid(cursor.getString(cursor.getColumnIndexOrThrow(NotificationEntry.COL_UUID)));
        n.setMessId(cursor.getLong(cursor.getColumnIndexOrThrow(NotificationEntry.COL_MESS_ID)));
        n.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(NotificationEntry.COL_USER_ID)));
        n.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(NotificationEntry.COL_TITLE)));
        n.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(NotificationEntry.COL_MESSAGE)));
        n.setType(cursor.getString(cursor.getColumnIndexOrThrow(NotificationEntry.COL_TYPE)));
        n.setRead(cursor.getInt(cursor.getColumnIndexOrThrow(NotificationEntry.COL_IS_READ)) == 1);
        n.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(NotificationEntry.COL_CREATED_AT)));
        return n;
    }
}
