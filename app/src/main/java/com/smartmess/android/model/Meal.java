package com.smartmess.android.model;

import com.google.gson.annotations.SerializedName;

public class Meal {
    @SerializedName("id")
    private long id;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName(value = "messId", alternate = {"mess_id"})
    private long messId;

    @SerializedName(value = "userId", alternate = {"user_id"})
    private long userId;

    @SerializedName(value = "userName", alternate = {"user_name"})
    private String userName; // transient/joined

    @SerializedName(value = "mealDate", alternate = {"meal_date"})
    private String mealDate; // 'YYYY-MM-DD'

    @SerializedName(value = "breakfastCount", alternate = {"breakfast_count"})
    private double breakfastCount;

    @SerializedName(value = "lunchCount", alternate = {"lunch_count"})
    private double lunchCount;

    @SerializedName(value = "dinnerCount", alternate = {"dinner_count"})
    private double dinnerCount;

    @SerializedName(value = "guestMealCount", alternate = {"guest_meal_count"})
    private double guestMealCount;

    @SerializedName(value = "isLocked", alternate = {"is_locked"})
    private int isLocked;

    @SerializedName(value = "isFlexible", alternate = {"is_flexible"})
    private int isFlexible; // 0=standard, 1=flexible

    @SerializedName(value = "mealType", alternate = {"meal_type"})
    private String mealType; // 'breakfast', 'lunch', 'dinner', 'special', 'mixed'

    @SerializedName(value = "rawBazarName", alternate = {"raw_bazar_name"})
    private String rawBazarName; // e.g. 'গরুর মাংস (Beef)', 'সবজি (Sobji)', 'বেগুন (Begun)'

    @SerializedName(value = "rawBazarAmount", alternate = {"raw_bazar_amount"})
    private double rawBazarAmount;

    @SerializedName(value = "sharedPrice", alternate = {"shared_price"})
    private double sharedPrice; // Individual shared meal price calculated for this user

    @SerializedName(value = "bazarId", alternate = {"bazar_id"})
    private long bazarId;

    @SerializedName(value = "syncStatus", alternate = {"sync_status"})
    private int syncStatus; // 0=pending, 1=synced

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    private String createdAt;

    @SerializedName(value = "updatedAt", alternate = {"updated_at"})
    private String updatedAt;

    public Meal() {}

    public Meal(String uuid, long messId, long userId, String mealDate, double breakfast, double lunch, double dinner, double guestMeal) {
        this.uuid = uuid;
        this.messId = messId;
        this.userId = userId;
        this.mealDate = mealDate;
        this.breakfastCount = breakfast;
        this.lunchCount = lunch;
        this.dinnerCount = dinner;
        this.guestMealCount = guestMeal;
        this.isLocked = 0;
        this.syncStatus = 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public long getMessId() { return messId; }
    public void setMessId(long messId) { this.messId = messId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getMealDate() { return mealDate; }
    public void setMealDate(String mealDate) { this.mealDate = mealDate; }

    public double getBreakfastCount() { return breakfastCount; }
    public void setBreakfastCount(double breakfastCount) { this.breakfastCount = breakfastCount; }

    public double getLunchCount() { return lunchCount; }
    public void setLunchCount(double lunchCount) { this.lunchCount = lunchCount; }

    public double getDinnerCount() { return dinnerCount; }
    public void setDinnerCount(double dinnerCount) { this.dinnerCount = dinnerCount; }

    public double getGuestMealCount() { return guestMealCount; }
    public void setGuestMealCount(double guestMealCount) { this.guestMealCount = guestMealCount; }

    public int getIsLocked() { return isLocked; }
    public void setIsLocked(int isLocked) { this.isLocked = isLocked; }

    public int getSyncStatus() { return syncStatus; }
    public void setSyncStatus(int syncStatus) { this.syncStatus = syncStatus; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public int getIsFlexible() { return isFlexible; }
    public void setIsFlexible(int isFlexible) { this.isFlexible = isFlexible; }
    public boolean isFlexible() { return isFlexible == 1; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public String getRawBazarName() { return rawBazarName; }
    public void setRawBazarName(String rawBazarName) { this.rawBazarName = rawBazarName; }

    public double getRawBazarAmount() { return rawBazarAmount; }
    public void setRawBazarAmount(double rawBazarAmount) { this.rawBazarAmount = rawBazarAmount; }

    public double getSharedPrice() { return sharedPrice; }
    public void setSharedPrice(double sharedPrice) { this.sharedPrice = sharedPrice; }

    public long getBazarId() { return bazarId; }
    public void setBazarId(long bazarId) { this.bazarId = bazarId; }

    public double getTotalMeals() {
        return breakfastCount + lunchCount + dinnerCount + guestMealCount;
    }
}
