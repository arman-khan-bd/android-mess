package com.smartmess.android.model;

public class SmsLog {
    private long id;
    private String uuid;
    private long messId;
    private long senderUserId;
    private String senderName;
    private String recipientPhone;
    private Long targetUserId;
    private String targetUserName;
    private String messageContent;
    private double costApplied;
    private String dispatchType; // 'device_sim', 'cloud_gateway'
    private String deliveryStatus; // 'sent', 'failed'
    private int syncStatus; // 0=pending, 1=synced
    private String createdAt;
    private String updatedAt;

    public SmsLog() {}

    public SmsLog(String uuid, long messId, long senderUserId, String recipientPhone, Long targetUserId, String messageContent, double costApplied, String dispatchType, String deliveryStatus) {
        this.uuid = uuid;
        this.messId = messId;
        this.senderUserId = senderUserId;
        this.recipientPhone = recipientPhone;
        this.targetUserId = targetUserId;
        this.messageContent = messageContent;
        this.costApplied = costApplied;
        this.dispatchType = dispatchType;
        this.deliveryStatus = deliveryStatus;
        this.syncStatus = 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public long getMessId() { return messId; }
    public void setMessId(long messId) { this.messId = messId; }

    public long getSenderUserId() { return senderUserId; }
    public void setSenderUserId(long senderUserId) { this.senderUserId = senderUserId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public String getTargetUserName() { return targetUserName; }
    public void setTargetUserName(String targetUserName) { this.targetUserName = targetUserName; }

    public String getTargetName() { return targetUserName; }
    public void setTargetName(String targetName) { this.targetUserName = targetName; }

    public String getMessageContent() { return messageContent; }
    public void setMessageContent(String messageContent) { this.messageContent = messageContent; }

    public double getCostApplied() { return costApplied; }
    public void setCostApplied(double costApplied) { this.costApplied = costApplied; }

    public String getDispatchType() { return dispatchType; }
    public void setDispatchType(String dispatchType) { this.dispatchType = dispatchType; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public int getSyncStatus() { return syncStatus; }
    public void setSyncStatus(int syncStatus) { this.syncStatus = syncStatus; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
