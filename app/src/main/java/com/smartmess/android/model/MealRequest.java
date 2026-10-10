package com.smartmess.android.model;

public class MealRequest {
    private long id;
    private String uuid;
    private long messId;
    private long userId;
    private String userName; // transient/joined
    private String requestDate; // 'YYYY-MM-DD'
    private double breakfastCount;
    private double lunchCount;
    private double dinnerCount;
    private double guestCount;
    private String note;
    private String status; // 'pending', 'approved', 'rejected'
    private String createdAt;
    private String updatedAt;

    public MealRequest() {
        this.status = "pending";
    }

    public MealRequest(String uuid, long messId, long userId, String requestDate,
                       double breakfastCount, double lunchCount, double dinnerCount,
                       double guestCount, String note) {
        this.uuid = uuid;
        this.messId = messId;
        this.userId = userId;
        this.requestDate = requestDate;
        this.breakfastCount = breakfastCount;
        this.lunchCount = lunchCount;
        this.dinnerCount = dinnerCount;
        this.guestCount = guestCount;
        this.note = note;
        this.status = "pending";
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

    public String getRequestDate() { return requestDate; }
    public void setRequestDate(String requestDate) { this.requestDate = requestDate; }

    public double getBreakfastCount() { return breakfastCount; }
    public void setBreakfastCount(double breakfastCount) { this.breakfastCount = breakfastCount; }

    public double getLunchCount() { return lunchCount; }
    public void setLunchCount(double lunchCount) { this.lunchCount = lunchCount; }

    public double getDinnerCount() { return dinnerCount; }
    public void setDinnerCount(double dinnerCount) { this.dinnerCount = dinnerCount; }

    public double getGuestCount() { return guestCount; }
    public void setGuestCount(double guestCount) { this.guestCount = guestCount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public double getTotalRequestedMeals() {
        return breakfastCount + lunchCount + dinnerCount + guestCount;
    }
}
