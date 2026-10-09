package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TicketListResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("tickets")
    private List<SupportTicketDto> tickets;

    public boolean isSuccess() { return success; }
    public List<SupportTicketDto> getTickets() { return tickets; }
}
