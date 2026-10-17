package com.smartmess.android.data.local;

public final class SQLiteContract {
    private SQLiteContract() {}

    public static final String DATABASE_NAME = "smart_mess.db";
    public static final int DATABASE_VERSION = 1;

    public static final class MessEntry {
        public static final String TABLE_NAME = "messes";
        public static final String COL_ID = "id";
        public static final String COL_UUID = "uuid";
        public static final String COL_NAME = "name";
        public static final String COL_INVITE_CODE = "invite_code";
        public static final String COL_BILLING_CYCLE = "billing_cycle";
        public static final String COL_CYCLE_START_DAY = "cycle_start_day";
        public static final String COL_MEAL_CUTOFF_TIME = "meal_cutoff_time";
        public static final String COL_PER_SMS_COST = "per_sms_cost";
        public static final String COL_TARGET_MEAL_BUDGET = "target_meal_budget";
        public static final String COL_CURRENT_PLAN_ID = "current_plan_id";
        public static final String COL_PLAN_EXPIRES_AT = "plan_expires_at";
        public static final String COL_CREATED_AT = "created_at";
        public static final String COL_UPDATED_AT = "updated_at";
    }

    public static final class UserEntry {
        public static final String TABLE_NAME = "users";
        public static final String COL_ID = "id";
        public static final String COL_UUID = "uuid";
        public static final String COL_MESS_ID = "mess_id";
        public static final String COL_NAME = "name";
        public static final String COL_PHONE = "phone";
        public static final String COL_PASSWORD = "password";
        public static final String COL_ROLE = "role";
        public static final String COL_STATUS = "status";
        public static final String COL_AVATAR_URL = "avatar_url";
        public static final String COL_EMAIL = "email";
        public static final String COL_FACEBOOK_URL = "facebook_url";
        public static final String COL_CREATED_AT = "created_at";
        public static final String COL_UPDATED_AT = "updated_at";
    }

    public static final class DepositEntry {
        public static final String TABLE_NAME = "deposits";
        public static final String COL_ID = "id";
        public static final String COL_UUID = "uuid";
        public static final String COL_MESS_ID = "mess_id";
        public static final String COL_USER_ID = "user_id";
        public static final String COL_AMOUNT = "amount";
        public static final String COL_DEPOSIT_DATE = "deposit_date";
        public static final String COL_NOTE = "note";
        public static final String COL_SYNC_STATUS = "sync_status";
        public static final String COL_CREATED_AT = "created_at";
        public static final String COL_UPDATED_AT = "updated_at";
    }

    public static final class ExpenseEntry {
        public static final String TABLE_NAME = "expenses";
        public static final String COL_ID = "id";
        public static final String COL_UUID = "uuid";
        public static final String COL_MESS_ID = "mess_id";
        public static final String COL_BUYER_USER_ID = "buyer_user_id";
        public static final String COL_EXPENSE_CATEGORY = "expense_category";
        public static final String COL_AMOUNT = "amount";
        public static final String COL_EXPENSE_DATE = "expense_date";
        public static final String COL_TITLE = "title";
        public static final String COL_VOUCHER_IMAGE_URL = "voucher_image_url";
        public static final String COL_SPLIT_TYPE = "split_type";
        public static final String COL_TARGET_USER_ID = "target_user_id";
        public static final String COL_SYNC_STATUS = "sync_status";
        public static final String COL_BAZAR_SESSION_UUID = "bazar_session_uuid";
        public static final String COL_CREATED_AT = "created_at";
        public static final String COL_UPDATED_AT = "updated_at";
    }

    public static final class MealEntry {
        public static final String TABLE_NAME = "meals";
        public static final String COL_ID = "id";
        public static final String COL_UUID = "uuid";
        public static final String COL_MESS_ID = "mess_id";
        public static final String COL_USER_ID = "user_id";
        public static final String COL_MEAL_DATE = "meal_date";
        public static final String COL_BREAKFAST_COUNT = "breakfast_count";
        public static final String COL_LUNCH_COUNT = "lunch_count";
        public static final String COL_DINNER_COUNT = "dinner_count";
        public static final String COL_GUEST_MEAL_COUNT = "guest_meal_count";
        public static final String COL_IS_LOCKED = "is_locked";
        public static final String COL_IS_FLEXIBLE = "is_flexible";
        public static final String COL_MEAL_TYPE = "meal_type";
        public static final String COL_RAW_BAZAR_NAME = "raw_bazar_name";
        public static final String COL_RAW_BAZAR_AMOUNT = "raw_bazar_amount";
        public static final String COL_SHARED_PRICE = "shared_price";
        public static final String COL_BAZAR_ID = "bazar_id";
        public static final String COL_BAZAR_SESSION_UUID = "bazar_session_uuid";
        public static final String COL_SYNC_STATUS = "sync_status";
        public static final String COL_CREATED_AT = "created_at";
        public static final String COL_UPDATED_AT = "updated_at";
    }

    public static final class SmsLogEntry {
        public static final String TABLE_NAME = "sms_logs";
        public static final String COL_ID = "id";
        public static final String COL_UUID = "uuid";
        public static final String COL_MESS_ID = "mess_id";
        public static final String COL_SENDER_USER_ID = "sender_user_id";
        public static final String COL_RECIPIENT_PHONE = "recipient_phone";
        public static final String COL_TARGET_USER_ID = "target_user_id";
        public static final String COL_MESSAGE_CONTENT = "message_content";
        public static final String COL_COST_APPLIED = "cost_applied";
        public static final String COL_DISPATCH_TYPE = "dispatch_type";
        public static final String COL_DELIVERY_STATUS = "delivery_status";
        public static final String COL_SYNC_STATUS = "sync_status";
        public static final String COL_CREATED_AT = "created_at";
        public static final String COL_UPDATED_AT = "updated_at";
    }

    public static final class SaasPlanEntry {
        public static final String TABLE_NAME = "saas_plans";
        public static final String COL_ID = "id";
        public static final String COL_NAME = "name";
        public static final String COL_PRICE = "price";
        public static final String COL_DURATION_IN_DAYS = "duration_in_days";
        public static final String COL_FEATURES = "features";
        public static final String COL_STATUS = "status";
        public static final String COL_CREATED_AT = "created_at";
        public static final String COL_UPDATED_AT = "updated_at";
    }

    public static final class MealVacationEntry {
        public static final String TABLE_NAME = "meal_vacations";
        public static final String COL_ID = "id";
        public static final String COL_UUID = "uuid";
        public static final String COL_MESS_ID = "mess_id";
        public static final String COL_USER_ID = "user_id";
        public static final String COL_START_DATE = "start_date";
        public static final String COL_END_DATE = "end_date";
        public static final String COL_REASON = "reason";
        public static final String COL_STATUS = "status"; // 'active', 'cancelled', 'completed'
        public static final String COL_CREATED_AT = "created_at";
        public static final String COL_UPDATED_AT = "updated_at";
    }

    public static final class NotificationEntry {
        public static final String TABLE_NAME = "app_notifications";
        public static final String COL_ID = "id";
        public static final String COL_UUID = "uuid";
        public static final String COL_MESS_ID = "mess_id";
        public static final String COL_USER_ID = "user_id";
        public static final String COL_TITLE = "title";
        public static final String COL_MESSAGE = "message";
        public static final String COL_TYPE = "type"; // 'expense', 'role', 'budget_alert', 'vacation', 'deposit', 'system'
        public static final String COL_IS_READ = "is_read"; // 0 or 1
        public static final String COL_CREATED_AT = "created_at";
    }

    public static final class MealRequestEntry {
        public static final String TABLE_NAME = "meal_requests";
        public static final String COL_ID = "id";
        public static final String COL_UUID = "uuid";
        public static final String COL_MESS_ID = "mess_id";
        public static final String COL_USER_ID = "user_id";
        public static final String COL_REQUEST_DATE = "request_date";
        public static final String COL_BREAKFAST_COUNT = "breakfast_count";
        public static final String COL_LUNCH_COUNT = "lunch_count";
        public static final String COL_DINNER_COUNT = "dinner_count";
        public static final String COL_GUEST_COUNT = "guest_count";
        public static final String COL_NOTE = "note";
        public static final String COL_STATUS = "status"; // 'pending', 'approved', 'rejected'
        public static final String COL_CREATED_AT = "created_at";
        public static final String COL_UPDATED_AT = "updated_at";
    }

    public static final class BazarSessionEntry {
        public static final String TABLE_NAME = "bazar_sessions";
        public static final String COL_ID = "id";
        public static final String COL_UUID = "uuid";
        public static final String COL_MESS_ID = "mess_id";
        public static final String COL_DATE = "bazar_date";
        public static final String COL_TITLE = "title";
        public static final String COL_MEAL_TYPE = "meal_type";
        public static final String COL_TOTAL_COST = "total_cost";
        public static final String COL_TOTAL_PAID = "total_paid";
        public static final String COL_ITEMS_JSON = "items_json";
        public static final String COL_PAYERS_JSON = "payers_json";
        public static final String COL_VOUCHER_IMAGE_URL = "voucher_image_url";
        public static final String COL_CREATED_BY = "created_by";
        public static final String COL_IS_BACKDATED = "is_backdated";
        public static final String COL_SYNC_STATUS = "sync_status";
        public static final String COL_CREATED_AT = "created_at";
        public static final String COL_UPDATED_AT = "updated_at";
    }

    public static final class ItemizedConsumptionEntry {
        public static final String TABLE_NAME = "itemized_consumption_logs";
        public static final String COL_ID = "id";
        public static final String COL_UUID = "uuid";
        public static final String COL_MESS_ID = "mess_id";
        public static final String COL_BAZAR_SESSION_UUID = "bazar_session_uuid";
        public static final String COL_BAZAR_ID = "bazar_id";
        public static final String COL_MEAL_ID = "meal_id";
        public static final String COL_USER_ID = "user_id";
        public static final String COL_USER_NAME = "user_name";
        public static final String COL_ITEM_NAME = "item_name";
        public static final String COL_ITEM_CATEGORY = "item_category";
        public static final String COL_ITEM_PRICE = "item_price";
        public static final String COL_MEAL_DATE = "meal_date";
        public static final String COL_MEAL_TYPE = "meal_type";
        public static final String COL_ACTIVE_DINERS_COUNT = "active_diners_count";
        public static final String COL_COST_SHARE = "cost_share";
        public static final String COL_IS_CONSUMED = "is_consumed";
        public static final String COL_CREATED_AT = "created_at";
    }
}
