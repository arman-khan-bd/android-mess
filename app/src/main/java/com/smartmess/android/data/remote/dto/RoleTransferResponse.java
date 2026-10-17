package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RoleTransferResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("broadcast_message")
    private String broadcastMessage;

    @SerializedName("server_timestamp")
    private String serverTimestamp;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBroadcastMessage() {
        return broadcastMessage;
    }

    public void setBroadcastMessage(String broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
    }

    public String getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(String serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }
}
