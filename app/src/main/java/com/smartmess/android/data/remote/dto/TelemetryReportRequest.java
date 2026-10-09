package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class TelemetryReportRequest {

    @SerializedName("report_type")
    private String reportType; // crash_auto, manual_bug, feature_suggestion, limitation

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("stack_trace")
    private String stackTrace;

    @SerializedName("device_metadata")
    private Map<String, String> deviceMetadata;

    @SerializedName("severity")
    private String severity; // low, medium, high, critical

    @SerializedName("mess_id")
    private Long messId;

    @SerializedName("user_id")
    private Long userId;

    public TelemetryReportRequest() {}

    public TelemetryReportRequest(String reportType, String title, String description, String stackTrace, Map<String, String> deviceMetadata, String severity) {
        this.reportType = reportType;
        this.title = title;
        this.description = description;
        this.stackTrace = stackTrace;
        this.deviceMetadata = deviceMetadata;
        this.severity = severity;
    }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public Map<String, String> getDeviceMetadata() { return deviceMetadata; }
    public void setDeviceMetadata(Map<String, String> deviceMetadata) { this.deviceMetadata = deviceMetadata; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Long getMessId() { return messId; }
    public void setMessId(Long messId) { this.messId = messId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
