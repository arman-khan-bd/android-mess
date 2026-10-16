package com.smartmess.android.model;

import com.google.gson.annotations.SerializedName;

public class SmsLog {
    @SerializedName("id")
    private long id;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName(value = "messId", alternate = {"mess_id"})
    private long messId;

    @SerializedName(value = "senderUserId", alternate = {"sender_user_id"})
    private long senderUserId;

    @SerializedName(value = "senderName", alternate = {"sender_name"})
    private String senderName;

    @SerializedName(value = "recipientPhone", alternate = {"recipient_phone"})
    private String recipientPhone;

    @SerializedName(value = "targetUserId", alternate = {"target_user_id"})
    private Long targetUserId;

    @SerializedName(value = "targetUserName", alternate = {"target_user_name"})
    private String targetUserName;

    @SerializedName(value = "messageContent", alternate = {"message_content"})
    private String messageContent;

    @SerializedName(value = "costApplied", alternate = {"cost_applied"})
    private double costApplied;

    @SerializedName(value = "dispatchType", alternate = {"dispatch_type"})
    private String dispatchType; // 'device_sim', 'cloud_gateway'

    @SerializedName(value = "deliveryStatus", alternate = {"delivery_status"})
    private String deliveryStatus; // 'sent', 'failed'

    @SerializedName(value = "syncStatus", alternate = {"sync_status"})
    private int syncStatus; // 0=pending, 1=synced

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    private String createdAt;

    @SerializedName(value = "updatedAt", alternate = {"updated_at"})
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
