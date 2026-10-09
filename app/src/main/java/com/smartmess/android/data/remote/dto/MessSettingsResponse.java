package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import com.smartmess.android.model.Mess;

public class MessSettingsResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("mess")
    private Mess mess;

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

    public Mess getMess() {
        return mess;
    }

    public void setMess(Mess mess) {
        this.mess = mess;
    }
}
