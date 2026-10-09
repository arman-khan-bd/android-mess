package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class TicketMessageDto {

    @SerializedName("id")
    private long id;

    @SerializedName("sender_type")
    private String senderType;

    @SerializedName("sender_id")
    private long senderId;

    @SerializedName("sender_name")
    private String senderName;

    @SerializedName("is_me")
    private boolean isMe;

    @SerializedName("message_body")
    private String messageBody;

    @SerializedName("attachment_url")
    private String attachmentUrl;

    @SerializedName("created_at")
    private String createdAt;

    public long getId() { return id; }
    public String getSenderType() { return senderType; }
    public long getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public boolean isMe() { return isMe; }
    public String getMessageBody() { return messageBody; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public String getCreatedAt() { return createdAt; }

    public void setMessageBody(String messageBody) { this.messageBody = messageBody; }
    public void setSenderType(String senderType) { this.senderType = senderType; }
    public void setMe(boolean me) { isMe = me; }
}
