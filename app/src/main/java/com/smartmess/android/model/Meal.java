package com.smartmess.android.model;

public class Meal {
    private long id;
    private String uuid;
    private long messId;
    private long userId;
    private String userName; // transient/joined
    private String mealDate; // 'YYYY-MM-DD'
    private double breakfastCount;
    private double lunchCount;
    private double dinnerCount;
    private double guestMealCount;
    private int isLocked;
    private int syncStatus; // 0=pending, 1=synced
    private String createdAt;
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

    public double getTotalMeals() {
        return breakfastCount + lunchCount + dinnerCount + guestMealCount;
    }
}
