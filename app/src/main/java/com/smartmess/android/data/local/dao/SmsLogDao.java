package com.smartmess.android.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.SQLiteContract.SmsLogEntry;
import com.smartmess.android.data.local.SQLiteContract.UserEntry;
import com.smartmess.android.model.SmsLog;

import java.util.ArrayList;
import java.util.List;

public class SmsLogDao {
    private final DatabaseHelper dbHelper;

    public SmsLogDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insert(SmsLog log) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SmsLogEntry.COL_UUID, log.getUuid());
        values.put(SmsLogEntry.COL_MESS_ID, log.getMessId());
        values.put(SmsLogEntry.COL_SENDER_USER_ID, log.getSenderUserId());
        values.put(SmsLogEntry.COL_RECIPIENT_PHONE, log.getRecipientPhone());
        if (log.getTargetUserId() != null) {
            values.put(SmsLogEntry.COL_TARGET_USER_ID, log.getTargetUserId());
        } else {
            values.putNull(SmsLogEntry.COL_TARGET_USER_ID);
        }
        values.put(SmsLogEntry.COL_MESSAGE_CONTENT, log.getMessageContent());
        values.put(SmsLogEntry.COL_COST_APPLIED, log.getCostApplied());
        values.put(SmsLogEntry.COL_DISPATCH_TYPE, log.getDispatchType());
        values.put(SmsLogEntry.COL_DELIVERY_STATUS, log.getDeliveryStatus());
        values.put(SmsLogEntry.COL_SYNC_STATUS, log.getSyncStatus());
        values.put(SmsLogEntry.COL_CREATED_AT, log.getCreatedAt());
        values.put(SmsLogEntry.COL_UPDATED_AT, log.getUpdatedAt());

        return db.insert(SmsLogEntry.TABLE_NAME, null, values);
    }

    public List<SmsLog> getLogsByMess(long messId, int limit) {
        List<SmsLog> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT s.*, u." + UserEntry.COL_NAME + " as sender_name, tu." + UserEntry.COL_NAME + " as target_name "
                + "FROM " + SmsLogEntry.TABLE_NAME + " s "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON s." + SmsLogEntry.COL_SENDER_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " tu ON s." + SmsLogEntry.COL_TARGET_USER_ID + " = tu." + UserEntry.COL_ID + " "
                + "WHERE s." + SmsLogEntry.COL_MESS_ID + " = ? "
                + "ORDER BY s." + SmsLogEntry.COL_ID + " DESC LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(limit)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                SmsLog log = cursorToSmsLog(cursor);
                int senderIdx = cursor.getColumnIndex("sender_name");
                if (senderIdx >= 0) log.setSenderName(cursor.getString(senderIdx));
                int targetIdx = cursor.getColumnIndex("target_name");
                if (targetIdx >= 0 && !cursor.isNull(targetIdx)) log.setTargetUserName(cursor.getString(targetIdx));
                list.add(log);
            }
            cursor.close();
        }
        return list;
    }

    public List<SmsLog> getPendingSync() {
        List<SmsLog> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(SmsLogEntry.TABLE_NAME, null,
                SmsLogEntry.COL_SYNC_STATUS + " = 0", null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToSmsLog(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public void markSynced(List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SmsLogEntry.COL_SYNC_STATUS, 1);
        for (String uuid : uuids) {
            db.update(SmsLogEntry.TABLE_NAME, values, SmsLogEntry.COL_UUID + " = ?", new String[]{uuid});
        }
    }

    private SmsLog cursorToSmsLog(Cursor cursor) {
        SmsLog log = new SmsLog();
        log.setId(cursor.getLong(cursor.getColumnIndexOrThrow(SmsLogEntry.COL_ID)));
        log.setUuid(cursor.getString(cursor.getColumnIndexOrThrow(SmsLogEntry.COL_UUID)));
        log.setMessId(cursor.getLong(cursor.getColumnIndexOrThrow(SmsLogEntry.COL_MESS_ID)));
        log.setSenderUserId(cursor.getLong(cursor.getColumnIndexOrThrow(SmsLogEntry.COL_SENDER_USER_ID)));
        log.setRecipientPhone(cursor.getString(cursor.getColumnIndexOrThrow(SmsLogEntry.COL_RECIPIENT_PHONE)));
        int targetCol = cursor.getColumnIndexOrThrow(SmsLogEntry.COL_TARGET_USER_ID);
        if (!cursor.isNull(targetCol)) {
            log.setTargetUserId(cursor.getLong(targetCol));
        }
        log.setMessageContent(cursor.getString(cursor.getColumnIndexOrThrow(SmsLogEntry.COL_MESSAGE_CONTENT)));
        log.setCostApplied(cursor.getDouble(cursor.getColumnIndexOrThrow(SmsLogEntry.COL_COST_APPLIED)));
        log.setDispatchType(cursor.getString(cursor.getColumnIndexOrThrow(SmsLogEntry.COL_DISPATCH_TYPE)));
        log.setDeliveryStatus(cursor.getString(cursor.getColumnIndexOrThrow(SmsLogEntry.COL_DELIVERY_STATUS)));
        log.setSyncStatus(cursor.getInt(cursor.getColumnIndexOrThrow(SmsLogEntry.COL_SYNC_STATUS)));
        log.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(SmsLogEntry.COL_CREATED_AT)));
        log.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(SmsLogEntry.COL_UPDATED_AT)));
        return log;
    }
}
