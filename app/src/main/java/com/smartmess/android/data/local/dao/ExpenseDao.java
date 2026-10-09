package com.smartmess.android.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.SQLiteContract.ExpenseEntry;
import com.smartmess.android.data.local.SQLiteContract.UserEntry;
import com.smartmess.android.model.Expense;

import java.util.ArrayList;
import java.util.List;

public class ExpenseDao {
    private final DatabaseHelper dbHelper;

    public ExpenseDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insert(Expense expense) {
        return insertOrUpdate(expense);
    }

    public long insertOrUpdate(Expense expense) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ExpenseEntry.COL_UUID, expense.getUuid());
        values.put(ExpenseEntry.COL_MESS_ID, expense.getMessId());
        values.put(ExpenseEntry.COL_BUYER_USER_ID, expense.getBuyerUserId());
        values.put(ExpenseEntry.COL_EXPENSE_CATEGORY, expense.getExpenseCategory());
        values.put(ExpenseEntry.COL_AMOUNT, expense.getAmount());
        values.put(ExpenseEntry.COL_EXPENSE_DATE, expense.getExpenseDate());
        values.put(ExpenseEntry.COL_TITLE, expense.getTitle());
        values.put(ExpenseEntry.COL_VOUCHER_IMAGE_URL, expense.getVoucherImageUrl());
        values.put(ExpenseEntry.COL_SPLIT_TYPE, expense.getSplitType());
        if (expense.getTargetUserId() != null) {
            values.put(ExpenseEntry.COL_TARGET_USER_ID, expense.getTargetUserId());
        } else {
            values.putNull(ExpenseEntry.COL_TARGET_USER_ID);
        }
        values.put(ExpenseEntry.COL_SYNC_STATUS, expense.getSyncStatus());
        values.put(ExpenseEntry.COL_CREATED_AT, expense.getCreatedAt());
        values.put(ExpenseEntry.COL_UPDATED_AT, expense.getUpdatedAt());

        return db.insertWithOnConflict(ExpenseEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<Expense> getExpensesInDateRange(long messId, String startDate, String endDate) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT e.*, u." + UserEntry.COL_NAME + " as buyer_name, tu." + UserEntry.COL_NAME + " as target_name "
                + "FROM " + ExpenseEntry.TABLE_NAME + " e "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON e." + ExpenseEntry.COL_BUYER_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " tu ON e." + ExpenseEntry.COL_TARGET_USER_ID + " = tu." + UserEntry.COL_ID + " "
                + "WHERE e." + ExpenseEntry.COL_MESS_ID + " = ? AND e." + ExpenseEntry.COL_EXPENSE_DATE + " BETWEEN ? AND ? "
                + "ORDER BY e." + ExpenseEntry.COL_EXPENSE_DATE + " DESC, e." + ExpenseEntry.COL_ID + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), startDate, endDate});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Expense expense = cursorToExpense(cursor);
                int buyerIdx = cursor.getColumnIndex("buyer_name");
                if (buyerIdx >= 0) expense.setBuyerName(cursor.getString(buyerIdx));
                int targetIdx = cursor.getColumnIndex("target_name");
                if (targetIdx >= 0 && !cursor.isNull(targetIdx)) expense.setTargetUserName(cursor.getString(targetIdx));
                list.add(expense);
            }
            cursor.close();
        }
        return list;
    }

    public List<Expense> getRecentExpenses(long messId, int limit) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT e.*, u." + UserEntry.COL_NAME + " as buyer_name, tu." + UserEntry.COL_NAME + " as target_name "
                + "FROM " + ExpenseEntry.TABLE_NAME + " e "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON e." + ExpenseEntry.COL_BUYER_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " tu ON e." + ExpenseEntry.COL_TARGET_USER_ID + " = tu." + UserEntry.COL_ID + " "
                + "WHERE e." + ExpenseEntry.COL_MESS_ID + " = ? "
                + "ORDER BY e." + ExpenseEntry.COL_EXPENSE_DATE + " DESC, e." + ExpenseEntry.COL_ID + " DESC LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(limit)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Expense expense = cursorToExpense(cursor);
                int buyerIdx = cursor.getColumnIndex("buyer_name");
                if (buyerIdx >= 0) expense.setBuyerName(cursor.getString(buyerIdx));
                int targetIdx = cursor.getColumnIndex("target_name");
                if (targetIdx >= 0 && !cursor.isNull(targetIdx)) expense.setTargetUserName(cursor.getString(targetIdx));
                list.add(expense);
            }
            cursor.close();
        }
        return list;
    }

    public List<Expense> getExpensesSince(long messId, String minDate) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT e.*, u." + UserEntry.COL_NAME + " as buyer_name, tu." + UserEntry.COL_NAME + " as target_name "
                + "FROM " + ExpenseEntry.TABLE_NAME + " e "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON e." + ExpenseEntry.COL_BUYER_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " tu ON e." + ExpenseEntry.COL_TARGET_USER_ID + " = tu." + UserEntry.COL_ID + " "
                + "WHERE e." + ExpenseEntry.COL_MESS_ID + " = ? AND e." + ExpenseEntry.COL_EXPENSE_DATE + " >= ? "
                + "ORDER BY e." + ExpenseEntry.COL_EXPENSE_DATE + " DESC, e." + ExpenseEntry.COL_ID + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), minDate});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Expense expense = cursorToExpense(cursor);
                int buyerIdx = cursor.getColumnIndex("buyer_name");
                if (buyerIdx >= 0) expense.setBuyerName(cursor.getString(buyerIdx));
                int targetIdx = cursor.getColumnIndex("target_name");
                if (targetIdx >= 0 && !cursor.isNull(targetIdx)) expense.setTargetUserName(cursor.getString(targetIdx));
                list.add(expense);
            }
            cursor.close();
        }
        return list;
    }

    public List<Expense> getExpensesInDateRange(long messId, String startDate, String endDate) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT e.*, u." + UserEntry.COL_NAME + " as buyer_name, tu." + UserEntry.COL_NAME + " as target_name "
                + "FROM " + ExpenseEntry.TABLE_NAME + " e "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " u ON e." + ExpenseEntry.COL_BUYER_USER_ID + " = u." + UserEntry.COL_ID + " "
                + "LEFT JOIN " + UserEntry.TABLE_NAME + " tu ON e." + ExpenseEntry.COL_TARGET_USER_ID + " = tu." + UserEntry.COL_ID + " "
                + "WHERE e." + ExpenseEntry.COL_MESS_ID + " = ? AND e." + ExpenseEntry.COL_EXPENSE_DATE + " BETWEEN ? AND ? "
                + "ORDER BY e." + ExpenseEntry.COL_EXPENSE_DATE + " DESC, e." + ExpenseEntry.COL_ID + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), startDate, endDate});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Expense expense = cursorToExpense(cursor);
                int buyerIdx = cursor.getColumnIndex("buyer_name");
                if (buyerIdx >= 0) expense.setBuyerName(cursor.getString(buyerIdx));
                int targetIdx = cursor.getColumnIndex("target_name");
                if (targetIdx >= 0 && !cursor.isNull(targetIdx)) expense.setTargetUserName(cursor.getString(targetIdx));
                list.add(expense);
            }
            cursor.close();
        }
        return list;
    }

    public double getTotalByCategory(long messId, String category, String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM(" + ExpenseEntry.COL_AMOUNT + ") FROM " + ExpenseEntry.TABLE_NAME
                + " WHERE " + ExpenseEntry.COL_MESS_ID + " = ? AND "
                + ExpenseEntry.COL_EXPENSE_CATEGORY + " = ? AND "
                + ExpenseEntry.COL_EXPENSE_DATE + " BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), category, startDate, endDate});
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public double getIndividualTargetExpenses(long messId, long targetUserId, String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM(" + ExpenseEntry.COL_AMOUNT + ") FROM " + ExpenseEntry.TABLE_NAME
                + " WHERE " + ExpenseEntry.COL_MESS_ID + " = ? AND "
                + ExpenseEntry.COL_SPLIT_TYPE + " = '" + Expense.SPLIT_INDIVIDUAL + "' AND "
                + ExpenseEntry.COL_TARGET_USER_ID + " = ? AND "
                + ExpenseEntry.COL_EXPENSE_DATE + " BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), String.valueOf(targetUserId), startDate, endDate});
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public double getTotalMessExpenses(long messId, String startDate, String endDate) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT SUM(" + ExpenseEntry.COL_AMOUNT + ") FROM " + ExpenseEntry.TABLE_NAME
                + " WHERE " + ExpenseEntry.COL_MESS_ID + " = ? AND "
                + ExpenseEntry.COL_EXPENSE_DATE + " BETWEEN ? AND ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messId), startDate, endDate});
        double total = 0.0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getDouble(0);
            cursor.close();
        }
        return total;
    }

    public List<Expense> getPendingSync() {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(ExpenseEntry.TABLE_NAME, null,
                ExpenseEntry.COL_SYNC_STATUS + " = 0", null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToExpense(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public void markSynced(List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) return;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ExpenseEntry.COL_SYNC_STATUS, 1);
        for (String uuid : uuids) {
            db.update(ExpenseEntry.TABLE_NAME, values, ExpenseEntry.COL_UUID + " = ?", new String[]{uuid});
        }
    }

    private Expense cursorToExpense(Cursor cursor) {
        Expense expense = new Expense();
        expense.setId(cursor.getLong(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_ID)));
        expense.setUuid(cursor.getString(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_UUID)));
        expense.setMessId(cursor.getLong(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_MESS_ID)));
        expense.setBuyerUserId(cursor.getLong(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_BUYER_USER_ID)));
        expense.setExpenseCategory(cursor.getString(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_EXPENSE_CATEGORY)));
        expense.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_AMOUNT)));
        expense.setExpenseDate(cursor.getString(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_EXPENSE_DATE)));
        expense.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_TITLE)));
        expense.setVoucherImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_VOUCHER_IMAGE_URL)));
        expense.setSplitType(cursor.getString(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_SPLIT_TYPE)));
        int targetCol = cursor.getColumnIndexOrThrow(ExpenseEntry.COL_TARGET_USER_ID);
        if (!cursor.isNull(targetCol)) {
            expense.setTargetUserId(cursor.getLong(targetCol));
        }
        expense.setSyncStatus(cursor.getInt(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_SYNC_STATUS)));
        expense.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_CREATED_AT)));
        expense.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(ExpenseEntry.COL_UPDATED_AT)));
        return expense;
    }
}
