package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class NotificationSendResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private NotificationData data;

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public NotificationData getData() { return data; }

    public static class NotificationData {
        @SerializedName("id")
        private long id;

        @SerializedName("uuid")
        private String uuid;

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
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getType() { return type; }
        public String getChannel() { return channel; }
        public String getCreatedAt() { return createdAt; }
    }
}
