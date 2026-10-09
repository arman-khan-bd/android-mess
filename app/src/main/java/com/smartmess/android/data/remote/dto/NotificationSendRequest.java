package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class NotificationSendRequest {

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("type")
    private String type; // 'notice', 'reminder', 'budget', 'meal', 'bazar', 'emergency', 'general'

    @SerializedName("channel")
    private String channel; // 'push', 'sms', 'both'

    @SerializedName("target_user_id")
    private Long targetUserId; // null for broadcast to all

    public NotificationSendRequest() {}

    public NotificationSendRequest(String title, String message, String type, String channel, Long targetUserId) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.channel = channel;
        this.targetUserId = targetUserId;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
}
