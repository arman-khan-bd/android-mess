package com.smartmess.android.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.SQLiteContract.DepositEntry;
import com.smartmess.android.data.local.SQLiteContract.UserEntry;
import com.smartmess.android.model.Deposit;

import java.util.ArrayList;
import java.util.List;

public class DepositDao {
    private final DatabaseHelper dbHelper;

    public DepositDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertOrUpdate(Deposit deposit) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DepositEntry.COL_UUID, deposit.getUuid());
        values.put(DepositEntry.COL_MESS_ID, deposit.getMessId());
        values.put(DepositEntry.COL_USER_ID, deposit.getUserId());
        values.put(DepositEntry.COL_AMOUNT, deposit.getAmount());
        values.put(DepositEntry.COL_DEPOSIT_DATE, deposit.getDepositDate());
        values.put(DepositEntry.COL_NOTE, deposit.getNote());
        values.put(DepositEntry.COL_SYNC_STATUS, deposit.getSyncStatus());
        values.put(DepositEntry.COL_CREATED_AT, deposit.getCreatedAt());
        values.put(DepositEntry.COL_UPDATED_AT, deposit.getUpdatedAt());

        return db.insertWithOnConflict(DepositEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<Deposit> getDepositsInDateRange(long messId, String startDate, String endDate) {
        List<Deposit> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT d.*, u." + UserEntry.COL_NAME + " as user_name FROM " + DepositEntry.TABLE_NAME + " d "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON d." + DepositEntry.COL_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "WHERE d." + DepositEntry.COL_MESS_ID + " = ? AND d." + DepositEntry.COL_DEPOSIT_DATE + " BETWEEN ? AND ? "
                + "ORDER BY d." + DepositEntry.COL_DEPOSIT_DATE + " DESC, d." + DepositEntry.COL_ID + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), startDate, endDate});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Deposit deposit = cursorToDeposit(cursor);
                int nameIdx = cursor.getColumnIndex("user_name");
                if (nameIdx >= 0) deposit.setUserName(cursor.getString(nameIdx));
                list.add(deposit);
            }
            cursor.close();
        }
        return list;
    }

    public List<Deposit> getRecentDeposits(long messId, int limit) {
        List<Deposit> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT d.*, u." + UserEntry.COL_NAME + " as user_name FROM " + DepositEntry.TABLE_NAME + " d "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON d." + DepositEntry.COL_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "WHERE d." + DepositEntry.COL_MESS_ID + " = ? "
                + "ORDER BY d." + DepositEntry.COL_DEPOSIT_DATE + " DESC, d." + DepositEntry.COL_ID + " DESC LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(limit)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Deposit deposit = cursorToDeposit(cursor);
                int nameIdx = cursor.getColumnIndex("user_name");
                if (nameIdx >= 0) deposit.setUserName(cursor.getString(nameIdx));
                list.add(deposit);
            }
            cursor.close();
        }
        return list;
    }

    public List<Deposit> getUserDeposits(long messId, long userId) {
        List<Deposit> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT d.*, u." + UserEntry.COL_NAME + " as user_name FROM " + DepositEntry.TABLE_NAME + " d "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON d." + DepositEntry.COL_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "WHERE d." + DepositEntry.COL_MESS_ID + " = ? AND d." + DepositEntry.COL_USER_ID + " = ? "
                + "ORDER BY d." + DepositEntry.COL_DEPOSIT_DATE + " DESC, d." + DepositEntry.COL_ID + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(userId)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Deposit deposit = cursorToDeposit(cursor);
                int nameIdx = cursor.getColumnIndex("user_name");
                if (nameIdx >= 0) deposit.setUserName(cursor.getString(nameIdx));
                list.add(deposit);
            }
            cursor.close();
        }
        return list;
    }

    public double getTotalUserDeposits(long messId, long userId, String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM(" + DepositEntry.COL_AMOUNT + ") FROM " + DepositEntry.TABLE_NAME
                + " WHERE " + DepositEntry.COL_MESS_ID + " = ? AND "
                + DepositEntry.COL_USER_ID + " = ? AND "
                + DepositEntry.COL_DEPOSIT_DATE + " BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(userId), startDate, endDate});
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public double getTotalMessDeposits(long messId, String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM(" + DepositEntry.COL_AMOUNT + ") FROM " + DepositEntry.TABLE_NAME
                + " WHERE " + DepositEntry.COL_MESS_ID + " = ? AND "
                + DepositEntry.COL_DEPOSIT_DATE + " BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), startDate, endDate});
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public List<Deposit> getPendingSync() {
        List<Deposit> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DepositEntry.TABLE_NAME, null,
                DepositEntry.COL_SYNC_STATUS + " = 0", null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToDeposit(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public void markSynced(List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DepositEntry.COL_SYNC_STATUS, 1);
        for (String uuid : uuids) {
            db.update(DepositEntry.TABLE_NAME, values, DepositEntry.COL_UUID + " = ?", new String[]{uuid});
        }
    }

    private Deposit cursorToDeposit(Cursor cursor) {
        Deposit deposit = new Deposit();
        deposit.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DepositEntry.COL_ID)));
        deposit.setUuid(cursor.getString(cursor.getColumnIndexOrThrow(DepositEntry.COL_UUID)));
        deposit.setMessId(cursor.getLong(cursor.getColumnIndexOrThrow(DepositEntry.COL_MESS_ID)));
        deposit.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(DepositEntry.COL_USER_ID)));
        deposit.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DepositEntry.COL_AMOUNT)));
        deposit.setDepositDate(cursor.getString(cursor.getColumnIndexOrThrow(DepositEntry.COL_DEPOSIT_DATE)));
        deposit.setNote(cursor.getString(cursor.getColumnIndexOrThrow(DepositEntry.COL_NOTE)));
        deposit.setSyncStatus(cursor.getInt(cursor.getColumnIndexOrThrow(DepositEntry.COL_SYNC_STATUS)));
        deposit.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DepositEntry.COL_CREATED_AT)));
        deposit.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DepositEntry.COL_UPDATED_AT)));
        return deposit;
    }
}
