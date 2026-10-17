package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RoleTransferRequest {

    @SerializedName("new_manager_user_id")
    private long newManagerUserId;

    @SerializedName("reason")
    private String reason;

    public RoleTransferRequest(long newManagerUserId, String reason) {
        this.newManagerUserId = newManagerUserId;
        this.reason = reason;
    }

    public long getNewManagerUserId() {
        return newManagerUserId;
    }

    public void setNewManagerUserId(long newManagerUserId) {
        this.newManagerUserId = newManagerUserId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
