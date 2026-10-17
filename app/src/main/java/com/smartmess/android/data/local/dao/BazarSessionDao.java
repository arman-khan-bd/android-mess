package com.smartmess.android.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.SQLiteContract.BazarSessionEntry;
import com.smartmess.android.model.BazarItem;
import com.smartmess.android.model.BazarPayer;
import com.smartmess.android.model.BazarSession;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BazarSessionDao {

    private final DatabaseHelper dbHelper;

    public BazarSessionDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public long insertOrUpdate(BazarSession session) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BazarSessionEntry.COL_UUID, session.getUuid());
        values.put(BazarSessionEntry.COL_MESS_ID, session.getMessId());
        values.put(BazarSessionEntry.COL_DATE, session.getDate());
        values.put(BazarSessionEntry.COL_TITLE, session.getTitle());
        values.put(BazarSessionEntry.COL_MEAL_TYPE, session.getMealType());
        values.put(BazarSessionEntry.COL_TOTAL_COST, session.getTotalItemCost());
        values.put(BazarSessionEntry.COL_TOTAL_PAID, session.getTotalPaid());
        values.put(BazarSessionEntry.COL_ITEMS_JSON, serializeItems(session.getItems()));
        values.put(BazarSessionEntry.COL_PAYERS_JSON, serializePayers(session.getPayers()));
        values.put(BazarSessionEntry.COL_VOUCHER_IMAGE_URL, session.getVoucherImageUrl());
        values.put(BazarSessionEntry.COL_CREATED_BY, session.getCreatedBy());
        values.put(BazarSessionEntry.COL_IS_BACKDATED, session.getIsBackdated());
        values.put(BazarSessionEntry.COL_SYNC_STATUS, session.getSyncStatus());
        values.put(BazarSessionEntry.COL_CREATED_AT, session.getCreatedAt());
        values.put(BazarSessionEntry.COL_UPDATED_AT, session.getUpdatedAt());

        Cursor cursor = db.query(BazarSessionEntry.TABLE_NAME, new String[]{BazarSessionEntry.COL_ID},
                BazarSessionEntry.COL_UUID + " = ?", new String[]{session.getUuid()},
                null, null, null);
        long id = -1;
        if (cursor != null && cursor.moveToFirst()) {
            id = cursor.getLong(0);
            cursor.close();
            db.update(BazarSessionEntry.TABLE_NAME, values, BazarSessionEntry.COL_ID + " = ?", new String[]{String.valueOf(id)});
        } else {
            if (cursor != null) cursor.close();
            id = db.insert(BazarSessionEntry.TABLE_NAME, null, values);
        }
        session.setId(id);
        return id;
    }

    public BazarSession getByUuid(String uuid) {
        if (uuid == null) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(BazarSessionEntry.TABLE_NAME, null,
                BazarSessionEntry.COL_UUID + " = ?", new String[]{uuid},
                null, null, null);
        BazarSession session = null;
        if (cursor != null && cursor.moveToFirst()) {
            session = cursorToSession(cursor);
            cursor.close();
        }
        return session;
    }

    public BazarSession getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(BazarSessionEntry.TABLE_NAME, null,
                BazarSessionEntry.COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);
        BazarSession session = null;
        if (cursor != null && cursor.moveToFirst()) {
            session = cursorToSession(cursor);
            cursor.close();
        }
        return session;
    }

    public List<BazarSession> getAll(long messId) {
        List<BazarSession> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(BazarSessionEntry.TABLE_NAME, null,
                BazarSessionEntry.COL_MESS_ID + " = ?", new String[]{String.valueOf(messId)},
                null, null, BazarSessionEntry.COL_DATE + " DESC, " + BazarSessionEntry.COL_ID + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToSession(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<BazarSession> getByDateRange(long messId, String startDate, String endDate) {
        List<BazarSession> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(BazarSessionEntry.TABLE_NAME, null,
                BazarSessionEntry.COL_MESS_ID + " = ? AND " + BazarSessionEntry.COL_DATE + " BETWEEN ? AND ?",
                new String[]{String.valueOf(messId), startDate, endDate},
                null, null, BazarSessionEntry.COL_DATE + " DESC, " + BazarSessionEntry.COL_ID + " DESC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToSession(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public boolean deleteByUuid(String uuid) {
        if (uuid == null) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(BazarSessionEntry.TABLE_NAME, BazarSessionEntry.COL_UUID + " = ?", new String[]{uuid}) > 0;
    }

    public BazarSession cursorToSession(Cursor cursor) {
        BazarSession session = new BazarSession();
        int idIdx = cursor.getColumnIndex(BazarSessionEntry.COL_ID);
        if (idIdx >= 0) session.setId(cursor.getLong(idIdx));

        int uuidIdx = cursor.getColumnIndex(BazarSessionEntry.COL_UUID);
        if (uuidIdx >= 0) session.setUuid(cursor.getString(uuidIdx));

        int messIdIdx = cursor.getColumnIndex(BazarSessionEntry.COL_MESS_ID);
        if (messIdIdx >= 0) session.setMessId(cursor.getLong(messIdIdx));

        int dateIdx = cursor.getColumnIndex(BazarSessionEntry.COL_DATE);
        if (dateIdx >= 0) session.setDate(cursor.getString(dateIdx));

        int titleIdx = cursor.getColumnIndex(BazarSessionEntry.COL_TITLE);
        if (titleIdx >= 0) session.setTitle(cursor.getString(titleIdx));

        int mealTypeIdx = cursor.getColumnIndex(BazarSessionEntry.COL_MEAL_TYPE);
        if (mealTypeIdx >= 0) session.setMealType(cursor.getString(mealTypeIdx));

        int voucherIdx = cursor.getColumnIndex(BazarSessionEntry.COL_VOUCHER_IMAGE_URL);
        if (voucherIdx >= 0) session.setVoucherImageUrl(cursor.getString(voucherIdx));

        int createdByIdx = cursor.getColumnIndex(BazarSessionEntry.COL_CREATED_BY);
        if (createdByIdx >= 0) session.setCreatedBy(cursor.getLong(createdByIdx));

        int isBackdatedIdx = cursor.getColumnIndex(BazarSessionEntry.COL_IS_BACKDATED);
        if (isBackdatedIdx >= 0) session.setIsBackdated(cursor.getInt(isBackdatedIdx));

        int syncStatusIdx = cursor.getColumnIndex(BazarSessionEntry.COL_SYNC_STATUS);
        if (syncStatusIdx >= 0) session.setSyncStatus(cursor.getInt(syncStatusIdx));

        int createdAtIdx = cursor.getColumnIndex(BazarSessionEntry.COL_CREATED_AT);
        if (createdAtIdx >= 0) session.setCreatedAt(cursor.getString(createdAtIdx));

        int updatedAtIdx = cursor.getColumnIndex(BazarSessionEntry.COL_UPDATED_AT);
        if (updatedAtIdx >= 0) session.setUpdatedAt(cursor.getString(updatedAtIdx));

        int itemsJsonIdx = cursor.getColumnIndex(BazarSessionEntry.COL_ITEMS_JSON);
        if (itemsJsonIdx >= 0) {
            String itemsJson = cursor.getString(itemsJsonIdx);
            session.setItems(deserializeItems(itemsJson));
        }

        int payersJsonIdx = cursor.getColumnIndex(BazarSessionEntry.COL_PAYERS_JSON);
        if (payersJsonIdx >= 0) {
            String payersJson = cursor.getString(payersJsonIdx);
            session.setPayers(deserializePayers(payersJson));
        }

        return session;
    }

    public static String serializeItems(List<BazarItem> items) {
        if (items == null) return "[]";
        JSONArray arr = new JSONArray();
        for (BazarItem item : items) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", item.getName());
                obj.put("category", item.getCategory());
                obj.put("quantity", item.getQuantity());
                obj.put("price", item.getPrice());
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        return arr.toString();
    }

    public static List<BazarItem> deserializeItems(String json) {
        List<BazarItem> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                BazarItem item = new BazarItem();
                item.setName(obj.optString("name", ""));
                item.setCategory(obj.optString("category", BazarItem.CAT_RAW_MEAL));
                item.setQuantity(obj.optString("quantity", ""));
                item.setPrice(obj.optDouble("price", 0.0));
                list.add(item);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static String serializePayers(List<BazarPayer> payers) {
        if (payers == null) return "[]";
        JSONArray arr = new JSONArray();
        for (BazarPayer payer : payers) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("userId", payer.getUserId());
                obj.put("userName", payer.getUserName());
                obj.put("amountPaid", payer.getAmountPaid());
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        return arr.toString();
    }

    public static List<BazarPayer> deserializePayers(String json) {
        List<BazarPayer> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                BazarPayer payer = new BazarPayer();
                payer.setUserId(obj.optLong("userId", 0));
                payer.setUserName(obj.optString("userName", ""));
                payer.setAmountPaid(obj.optDouble("amountPaid", 0.0));
                list.add(payer);
            }
        } catch (Exception ignored) {}
        return list;
    }
}
