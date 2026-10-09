package com.smartmess.android.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.smartmess.android.data.local.SQLiteContract.*;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper sInstance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseHelper(Context context) {
        super(context, SQLiteContract.DATABASE_NAME, null, SQLiteContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. saas_plans
        db.execSQL("CREATE TABLE " + SaasPlanEntry.TABLE_NAME + " ("
                + SaasPlanEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SaasPlanEntry.COL_NAME + " TEXT NOT NULL, "
                + SaasPlanEntry.COL_PRICE + " REAL DEFAULT 0.0, "
                + SaasPlanEntry.COL_DURATION_IN_DAYS + " INTEGER DEFAULT 30, "
                + SaasPlanEntry.COL_FEATURES + " TEXT, "
                + SaasPlanEntry.COL_STATUS + " INTEGER DEFAULT 1, "
                + SaasPlanEntry.COL_CREATED_AT + " TEXT, "
                + SaasPlanEntry.COL_UPDATED_AT + " TEXT);");

        // 2. messes
        db.execSQL("CREATE TABLE " + MessEntry.TABLE_NAME + " ("
                + MessEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MessEntry.COL_UUID + " TEXT UNIQUE NOT NULL, "
                + MessEntry.COL_NAME + " TEXT NOT NULL, "
                + MessEntry.COL_INVITE_CODE + " TEXT UNIQUE NOT NULL, "
                + MessEntry.COL_BILLING_CYCLE + " TEXT DEFAULT 'monthly', "
                + MessEntry.COL_CYCLE_START_DAY + " INTEGER DEFAULT 1, "
                + MessEntry.COL_MEAL_CUTOFF_TIME + " TEXT DEFAULT '22:00:00', "
                + MessEntry.COL_PER_SMS_COST + " REAL DEFAULT 0.50, "
                + MessEntry.COL_TARGET_MEAL_BUDGET + " REAL DEFAULT 70.0, "
                + MessEntry.COL_CURRENT_PLAN_ID + " INTEGER, "
                + MessEntry.COL_PLAN_EXPIRES_AT + " TEXT, "
                + MessEntry.COL_CREATED_AT + " TEXT, "
                + MessEntry.COL_UPDATED_AT + " TEXT);");

        // 3. users
        db.execSQL("CREATE TABLE " + UserEntry.TABLE_NAME + " ("
                + UserEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + UserEntry.COL_UUID + " TEXT UNIQUE NOT NULL, "
                + UserEntry.COL_MESS_ID + " INTEGER NOT NULL, "
                + UserEntry.COL_NAME + " TEXT NOT NULL, "
                + UserEntry.COL_PHONE + " TEXT UNIQUE NOT NULL, "
                + UserEntry.COL_PASSWORD + " TEXT, "
                + UserEntry.COL_ROLE + " TEXT DEFAULT 'member', "
                + UserEntry.COL_STATUS + " TEXT DEFAULT 'active', "
                + UserEntry.COL_AVATAR_URL + " TEXT, "
                + UserEntry.COL_CREATED_AT + " TEXT, "
                + UserEntry.COL_UPDATED_AT + " TEXT);");

        // 4. deposits
        db.execSQL("CREATE TABLE " + DepositEntry.TABLE_NAME + " ("
                + DepositEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DepositEntry.COL_UUID + " TEXT UNIQUE NOT NULL, "
                + DepositEntry.COL_MESS_ID + " INTEGER NOT NULL, "
                + DepositEntry.COL_USER_ID + " INTEGER NOT NULL, "
                + DepositEntry.COL_AMOUNT + " REAL NOT NULL, "
                + DepositEntry.COL_DEPOSIT_DATE + " TEXT NOT NULL, "
                + DepositEntry.COL_NOTE + " TEXT, "
                + DepositEntry.COL_SYNC_STATUS + " INTEGER DEFAULT 0, "
                + DepositEntry.COL_CREATED_AT + " TEXT, "
                + DepositEntry.COL_UPDATED_AT + " TEXT);");

        // 5. expenses
        db.execSQL("CREATE TABLE " + ExpenseEntry.TABLE_NAME + " ("
                + ExpenseEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ExpenseEntry.COL_UUID + " TEXT UNIQUE NOT NULL, "
                + ExpenseEntry.COL_MESS_ID + " INTEGER NOT NULL, "
                + ExpenseEntry.COL_BUYER_USER_ID + " INTEGER NOT NULL, "
                + ExpenseEntry.COL_EXPENSE_CATEGORY + " TEXT NOT NULL, "
                + ExpenseEntry.COL_AMOUNT + " REAL NOT NULL, "
                + ExpenseEntry.COL_EXPENSE_DATE + " TEXT NOT NULL, "
                + ExpenseEntry.COL_TITLE + " TEXT NOT NULL, "
                + ExpenseEntry.COL_VOUCHER_IMAGE_URL + " TEXT, "
                + ExpenseEntry.COL_SPLIT_TYPE + " TEXT DEFAULT 'meal_dependent', "
                + ExpenseEntry.COL_TARGET_USER_ID + " INTEGER, "
                + ExpenseEntry.COL_SYNC_STATUS + " INTEGER DEFAULT 0, "
                + ExpenseEntry.COL_CREATED_AT + " TEXT, "
                + ExpenseEntry.COL_UPDATED_AT + " TEXT);");

        // 6. meals
        db.execSQL("CREATE TABLE " + MealEntry.TABLE_NAME + " ("
                + MealEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MealEntry.COL_UUID + " TEXT UNIQUE NOT NULL, "
                + MealEntry.COL_MESS_ID + " INTEGER NOT NULL, "
                + MealEntry.COL_USER_ID + " INTEGER NOT NULL, "
                + MealEntry.COL_MEAL_DATE + " TEXT NOT NULL, "
                + MealEntry.COL_BREAKFAST_COUNT + " REAL DEFAULT 0.0, "
                + MealEntry.COL_LUNCH_COUNT + " REAL DEFAULT 0.0, "
                + MealEntry.COL_DINNER_COUNT + " REAL DEFAULT 0.0, "
                + MealEntry.COL_GUEST_MEAL_COUNT + " REAL DEFAULT 0.0, "
                + MealEntry.COL_IS_LOCKED + " INTEGER DEFAULT 0, "
                + MealEntry.COL_SYNC_STATUS + " INTEGER DEFAULT 0, "
                + MealEntry.COL_CREATED_AT + " TEXT, "
                + MealEntry.COL_UPDATED_AT + " TEXT);");

        // 7. sms_logs
        db.execSQL("CREATE TABLE " + SmsLogEntry.TABLE_NAME + " ("
                + SmsLogEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + SmsLogEntry.COL_UUID + " TEXT UNIQUE NOT NULL, "
                + SmsLogEntry.COL_MESS_ID + " INTEGER NOT NULL, "
                + SmsLogEntry.COL_SENDER_USER_ID + " INTEGER NOT NULL, "
                + SmsLogEntry.COL_RECIPIENT_PHONE + " TEXT NOT NULL, "
                + SmsLogEntry.COL_TARGET_USER_ID + " INTEGER, "
                + SmsLogEntry.COL_MESSAGE_CONTENT + " TEXT NOT NULL, "
                + SmsLogEntry.COL_COST_APPLIED + " REAL DEFAULT 0.0, "
                + SmsLogEntry.COL_DISPATCH_TYPE + " TEXT DEFAULT 'device_sim', "
                + SmsLogEntry.COL_DELIVERY_STATUS + " TEXT DEFAULT 'sent', "
                + SmsLogEntry.COL_SYNC_STATUS + " INTEGER DEFAULT 0, "
                + SmsLogEntry.COL_CREATED_AT + " TEXT, "
                + SmsLogEntry.COL_UPDATED_AT + " TEXT);");

        // 8. meal_vacations
        db.execSQL("CREATE TABLE " + MealVacationEntry.TABLE_NAME + " ("
                + MealVacationEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MealVacationEntry.COL_UUID + " TEXT UNIQUE NOT NULL, "
                + MealVacationEntry.COL_MESS_ID + " INTEGER NOT NULL, "
                + MealVacationEntry.COL_USER_ID + " INTEGER NOT NULL, "
                + MealVacationEntry.COL_START_DATE + " TEXT NOT NULL, "
                + MealVacationEntry.COL_END_DATE + " TEXT NOT NULL, "
                + MealVacationEntry.COL_REASON + " TEXT, "
                + MealVacationEntry.COL_STATUS + " TEXT DEFAULT 'active', "
                + MealVacationEntry.COL_CREATED_AT + " TEXT, "
                + MealVacationEntry.COL_UPDATED_AT + " TEXT);");

        // 9. app_notifications
        db.execSQL("CREATE TABLE " + NotificationEntry.TABLE_NAME + " ("
                + NotificationEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + NotificationEntry.COL_UUID + " TEXT UNIQUE NOT NULL, "
                + NotificationEntry.COL_MESS_ID + " INTEGER NOT NULL, "
                + NotificationEntry.COL_USER_ID + " INTEGER DEFAULT 0, "
                + NotificationEntry.COL_TITLE + " TEXT NOT NULL, "
                + NotificationEntry.COL_MESSAGE + " TEXT NOT NULL, "
                + NotificationEntry.COL_TYPE + " TEXT DEFAULT 'system', "
                + NotificationEntry.COL_IS_READ + " INTEGER DEFAULT 0, "
                + NotificationEntry.COL_CREATED_AT + " TEXT);");

        // Indexes for high performance queries
        db.execSQL("CREATE INDEX idx_meals_mess_date ON " + MealEntry.TABLE_NAME + " (" + MealEntry.COL_MESS_ID + ", " + MealEntry.COL_MEAL_DATE + ");");
        db.execSQL("CREATE INDEX idx_expenses_mess_date ON " + ExpenseEntry.TABLE_NAME + " (" + ExpenseEntry.COL_MESS_ID + ", " + ExpenseEntry.COL_EXPENSE_DATE + ");");
        db.execSQL("CREATE INDEX idx_deposits_mess_date ON " + DepositEntry.TABLE_NAME + " (" + DepositEntry.COL_MESS_ID + ", " + DepositEntry.COL_DEPOSIT_DATE + ");");
        db.execSQL("CREATE INDEX idx_users_mess ON " + UserEntry.TABLE_NAME + " (" + UserEntry.COL_MESS_ID + ");");
        db.execSQL("CREATE INDEX idx_vacations_user ON " + MealVacationEntry.TABLE_NAME + " (" + MealVacationEntry.COL_MESS_ID + ", " + MealVacationEntry.COL_USER_ID + ");");
        db.execSQL("CREATE INDEX idx_notifications_mess ON " + NotificationEntry.TABLE_NAME + " (" + NotificationEntry.COL_MESS_ID + ");");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        try {
            db.execSQL("ALTER TABLE " + MessEntry.TABLE_NAME + " ADD COLUMN " + MessEntry.COL_TARGET_MEAL_BUDGET + " REAL DEFAULT 70.0;");
        } catch (Exception ignored) {}
        try {
            db.execSQL("ALTER TABLE " + UserEntry.TABLE_NAME + " ADD COLUMN " + UserEntry.COL_AVATAR_URL + " TEXT;");
        } catch (Exception ignored) {}
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + MealVacationEntry.TABLE_NAME + " ("
                    + MealVacationEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + MealVacationEntry.COL_UUID + " TEXT UNIQUE NOT NULL, "
                    + MealVacationEntry.COL_MESS_ID + " INTEGER NOT NULL, "
                    + MealVacationEntry.COL_USER_ID + " INTEGER NOT NULL, "
                    + MealVacationEntry.COL_START_DATE + " TEXT NOT NULL, "
                    + MealVacationEntry.COL_END_DATE + " TEXT NOT NULL, "
                    + MealVacationEntry.COL_REASON + " TEXT, "
                    + MealVacationEntry.COL_STATUS + " TEXT DEFAULT 'active', "
                    + MealVacationEntry.COL_CREATED_AT + " TEXT, "
                    + MealVacationEntry.COL_UPDATED_AT + " TEXT);");
        } catch (Exception ignored) {}
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + NotificationEntry.TABLE_NAME + " ("
                    + NotificationEntry.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + NotificationEntry.COL_UUID + " TEXT UNIQUE NOT NULL, "
                    + NotificationEntry.COL_MESS_ID + " INTEGER NOT NULL, "
                    + NotificationEntry.COL_USER_ID + " INTEGER DEFAULT 0, "
                    + NotificationEntry.COL_TITLE + " TEXT NOT NULL, "
                    + NotificationEntry.COL_MESSAGE + " TEXT NOT NULL, "
                    + NotificationEntry.COL_TYPE + " TEXT DEFAULT 'system', "
                    + NotificationEntry.COL_IS_READ + " INTEGER DEFAULT 0, "
                    + NotificationEntry.COL_CREATED_AT + " TEXT);");
        } catch (Exception ignored) {}
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For development / upgrade drops
        db.execSQL("DROP TABLE IF EXISTS " + SaasPlanEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + MessEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + UserEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DepositEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ExpenseEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + MealEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SmsLogEntry.TABLE_NAME);
        onCreate(db);
    }
}
