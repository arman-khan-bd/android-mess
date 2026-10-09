package com.smartmess.android.model;

public class User {
    public static final String ROLE_MANAGER = "manager";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_MEMBER = "member";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_ON_LEAVE = "on_leave";
    public static final String STATUS_LEFT = "left";

    private long id;
    private String uuid;
    private long messId;
    private String name;
    private String phone;
    private String password;
    private String role; // 'manager', 'assistant', 'member'
    private String status; // 'active', 'on_leave', 'left'
    private String avatarUrl;
    private String createdAt;
    private String updatedAt;

    public User() {}

    public User(String uuid, long messId, String name, String phone, String role, String status) {
        this.uuid = uuid;
        this.messId = messId;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.status = status;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public long getMessId() { return messId; }
    public void setMessId(long messId) { this.messId = messId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isManager() {
        return "manager".equalsIgnoreCase(role) || "superadmin".equalsIgnoreCase(role);
    }

    public boolean isAssistant() {
        return "assistant".equalsIgnoreCase(role);
    }

    public boolean isMember() {
        return "member".equalsIgnoreCase(role) || (!isManager() && !isAssistant());
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    public boolean isOnLeave() {
        return STATUS_ON_LEAVE.equalsIgnoreCase(status) || "vacation".equalsIgnoreCase(status);
    }

    public boolean hasLeft() {
        return STATUS_LEFT.equalsIgnoreCase(status) || "inactive".equalsIgnoreCase(status);
    }

    public boolean canLogExpenses() {
        return isManager() || isAssistant();
    }
}
