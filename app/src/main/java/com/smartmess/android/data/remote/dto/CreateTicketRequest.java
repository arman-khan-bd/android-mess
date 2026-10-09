package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CreateTicketRequest {

    @SerializedName("subject")
    private String subject;

    @SerializedName("category")
    private String category; // billing_plan, sync_issue, feature_request, other

    @SerializedName("priority")
    private String priority; // normal, urgent

    @SerializedName("message")
    private String message;

    public CreateTicketRequest(String subject, String category, String priority, String message) {
        this.subject = subject;
        this.category = category;
        this.priority = priority;
        this.message = message;
    }

    public String getSubject() { return subject; }
    public String getCategory() { return category; }
    public String getPriority() { return priority; }
    public String getMessage() { return message; }
}
