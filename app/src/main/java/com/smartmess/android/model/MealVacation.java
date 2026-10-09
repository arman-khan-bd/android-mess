package com.smartmess.android.model;

public class MealVacation {

    private long id;
    private String uuid;
    private long messId;
    private long userId;
    private String startDate; // yyyy-MM-dd
    private String endDate;   // yyyy-MM-dd
    private String reason;
    private String status = "active"; // active, cancelled, completed
    private String createdAt;
    private String updatedAt;

    // Optional transient display field
    private String userName;

    public MealVacation() {}

    public MealVacation(String uuid, long messId, long userId, String startDate, String endDate, String reason) {
        this.uuid = uuid;
        this.messId = messId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = "active";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public long getMessId() { return messId; }
    public void setMessId(long messId) { this.messId = messId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getReason() { return reason != null ? reason : "Scheduled Vacation"; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status != null ? status : "active"; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return "active".equalsIgnoreCase(status); }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
