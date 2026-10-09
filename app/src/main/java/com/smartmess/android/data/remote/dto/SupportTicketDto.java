package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class SupportTicketDto {

    @SerializedName("id")
    private long id;

    @SerializedName("ticket_number")
    private String ticketNumber;

    @SerializedName("subject")
    private String subject;

    @SerializedName("category")
    private String category;

    @SerializedName("priority")
    private String priority;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public long getId() { return id; }
    public String getTicketNumber() { return ticketNumber; }
    public String getSubject() { return subject; }
    public String getCategory() { return category; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public void setId(long id) { this.id = id; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setCategory(String category) { this.category = category; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setStatus(String status) { this.status = status; }
}
