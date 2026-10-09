package com.smartmess.android.model;

public class AppNotification {

    private long id;
    private String uuid;
    private long messId;
    private long userId; // 0 for broadcast / all members
    private String title;
    private String message;
    private String type; // 'expense', 'role', 'budget_alert', 'vacation', 'deposit', 'system'
    private boolean isRead;
    private String createdAt;

    public AppNotification() {}

    public AppNotification(String uuid, long messId, long userId, String title, String message, String type, String createdAt) {
        this.uuid = uuid;
        this.messId = messId;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public long getMessId() { return messId; }
    public void setMessId(long messId) { this.messId = messId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type != null ? type : "system"; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
