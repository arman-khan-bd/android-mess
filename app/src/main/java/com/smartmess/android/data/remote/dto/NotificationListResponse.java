package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NotificationListResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private List<NotificationItem> data;

    @SerializedName("unread_count")
    private int unreadCount;

    public String getStatus() { return status; }
    public List<NotificationItem> getData() { return data; }
    public int getUnreadCount() { return unreadCount; }

    public static class NotificationItem {
        @SerializedName("id")
        private long id;

        @SerializedName("uuid")
        private String uuid;

        @SerializedName("mess_id")
        private long messId;

        @SerializedName("target_user_id")
        private Long targetUserId;

        @SerializedName("title")
        private String title;

        @SerializedName("message")
        private String message;

        @SerializedName("type")
        private String type;

        @SerializedName("channel")
        private String channel;

        @SerializedName("created_at")
        private String createdAt;

        public long getId() { return id; }
        public String getUuid() { return uuid; }
        public long getMessId() { return messId; }
        public Long getTargetUserId() { return targetUserId; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getType() { return type; }
        public String getChannel() { return channel; }
        public String getCreatedAt() { return createdAt; }
    }
}
