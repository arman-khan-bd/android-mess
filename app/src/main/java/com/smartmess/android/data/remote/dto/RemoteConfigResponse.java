package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RemoteConfigResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("maintenance_mode")
    private boolean maintenanceMode;

    @SerializedName("maintenance_message")
    private String maintenanceMessage;

    @SerializedName("force_update")
    private boolean forceUpdate;

    @SerializedName("has_update")
    private boolean hasUpdate;

    @SerializedName("min_version_code")
    private int minVersionCode;

    @SerializedName("latest_version_code")
    private int latestVersionCode;

    @SerializedName("update_url")
    private String updateUrl;

    @SerializedName("broadcast_announcement")
    private String broadcastAnnouncement;

    public boolean isSuccess() { return success; }
    public boolean isMaintenanceMode() { return maintenanceMode; }
    public String getMaintenanceMessage() { return maintenanceMessage; }
    public boolean isForceUpdate() { return forceUpdate; }
    public boolean hasUpdate() { return hasUpdate; }
    public int getMinVersionCode() { return minVersionCode; }
    public int getLatestVersionCode() { return latestVersionCode; }
    public String getUpdateUrl() { return updateUrl; }
    public String getBroadcastAnnouncement() { return broadcastAnnouncement; }
}
