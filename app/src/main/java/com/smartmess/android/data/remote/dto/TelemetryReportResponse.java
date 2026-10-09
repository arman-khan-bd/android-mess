package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class TelemetryReportResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("report_id")
    private long reportId;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getReportId() { return reportId; }
    public void setReportId(long reportId) { this.reportId = reportId; }
}
