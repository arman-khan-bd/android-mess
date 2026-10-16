package com.smartmess.android.model;

import com.google.gson.annotations.SerializedName;

public class Expense {
    // Categories
    public static final String CAT_RAW_MEAL = "raw_meal";
    public static final String CAT_SHARED_FOOD = "shared_food";
    public static final String CAT_UTILITY_ASSET = "utility_asset";
    public static final String CAT_SMS_CHARGE = "sms_charge";

    // Split Types
    public static final String SPLIT_MEAL_DEPENDENT = "meal_dependent";
    public static final String SPLIT_ALL_EQUAL = "split_all_equal";
    public static final String SPLIT_INDIVIDUAL = "individual_member";

    @SerializedName("id")
    private long id;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName(value = "messId", alternate = {"mess_id"})
    private long messId;

    @SerializedName(value = "buyerUserId", alternate = {"buyer_user_id"})
    private long buyerUserId;

    @SerializedName(value = "buyerName", alternate = {"buyer_name"})
    private String buyerName; // transient/joined

    @SerializedName(value = "expenseCategory", alternate = {"expense_category"})
    private String expenseCategory;

    @SerializedName("amount")
    private double amount;

    @SerializedName(value = "expenseDate", alternate = {"expense_date"})
    private String expenseDate;

    @SerializedName("title")
    private String title;

    @SerializedName(value = "voucherImageUrl", alternate = {"voucher_image_url"})
    private String voucherImageUrl;

    @SerializedName(value = "splitType", alternate = {"split_type"})
    private String splitType;

    @SerializedName(value = "targetUserId", alternate = {"target_user_id"})
    private Long targetUserId;

    @SerializedName(value = "targetUserName", alternate = {"target_user_name"})
    private String targetUserName; // transient/joined

    @SerializedName(value = "syncStatus", alternate = {"sync_status"})
    private int syncStatus; // 0=pending, 1=synced

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    private String createdAt;

    @SerializedName(value = "updatedAt", alternate = {"updated_at"})
    private String updatedAt;

    public Expense() {}

    public Expense(String uuid, long messId, long buyerUserId, String category, double amount, String date, String title, String splitType) {
        this.uuid = uuid;
        this.messId = messId;
        this.buyerUserId = buyerUserId;
        this.expenseCategory = category;
        this.amount = amount;
        this.expenseDate = date;
        this.title = title;
        this.splitType = splitType;
        this.syncStatus = 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public long getMessId() { return messId; }
    public void setMessId(long messId) { this.messId = messId; }

    public long getBuyerUserId() { return buyerUserId; }
    public void setBuyerUserId(long buyerUserId) { this.buyerUserId = buyerUserId; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getExpenseCategory() { return expenseCategory; }
    public void setExpenseCategory(String expenseCategory) { this.expenseCategory = expenseCategory; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getExpenseDate() { return expenseDate; }
    public void setExpenseDate(String expenseDate) { this.expenseDate = expenseDate; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVoucherImageUrl() { return voucherImageUrl; }
    public void setVoucherImageUrl(String voucherImageUrl) { this.voucherImageUrl = voucherImageUrl; }

    public String getSplitType() { return splitType; }
    public void setSplitType(String splitType) { this.splitType = splitType; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public String getTargetUserName() { return targetUserName; }
    public void setTargetUserName(String targetUserName) { this.targetUserName = targetUserName; }

    public int getSyncStatus() { return syncStatus; }
    public void setSyncStatus(int syncStatus) { this.syncStatus = syncStatus; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isRawMeal() {
        return CAT_RAW_MEAL.equalsIgnoreCase(expenseCategory);
    }

    public boolean isSharedFood() {
        return CAT_SHARED_FOOD.equalsIgnoreCase(expenseCategory);
    }

    public boolean isUtilityAsset() {
        return CAT_UTILITY_ASSET.equalsIgnoreCase(expenseCategory);
    }

    public boolean isSmsCharge() {
        return CAT_SMS_CHARGE.equalsIgnoreCase(expenseCategory);
    }
}
