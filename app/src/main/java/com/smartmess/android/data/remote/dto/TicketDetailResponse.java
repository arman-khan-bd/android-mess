package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TicketDetailResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("ticket")
    private SupportTicketDto ticket;

    @SerializedName("messages")
    private List<TicketMessageDto> messages;

    public boolean isSuccess() { return success; }
    public SupportTicketDto getTicket() { return ticket; }
    public List<TicketMessageDto> getMessages() { return messages; }
}
