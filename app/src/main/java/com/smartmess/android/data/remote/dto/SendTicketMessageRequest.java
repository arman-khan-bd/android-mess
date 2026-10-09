package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class SendTicketMessageRequest {

    @SerializedName("message")
    private String message;

    public SendTicketMessageRequest(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
