package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class SyncPushResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("synced_uuids")
    private List<String> syncedUuids = new ArrayList<>();

    @SerializedName("synced_deposits")
    private List<String> syncedDepositUuids = new ArrayList<>();

    @SerializedName("synced_expenses")
    private List<String> syncedExpenseUuids = new ArrayList<>();

    @SerializedName("synced_meals")
    private List<String> syncedMealUuids = new ArrayList<>();

    @SerializedName("synced_sms_logs")
    private List<String> syncedSmsLogUuids = new ArrayList<>();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getSyncedUuids() {
        if (syncedUuids != null && !syncedUuids.isEmpty()) {
            return syncedUuids;
        }
        List<String> combined = new ArrayList<>();
        if (syncedDepositUuids != null) combined.addAll(syncedDepositUuids);
        if (syncedExpenseUuids != null) combined.addAll(syncedExpenseUuids);
        if (syncedMealUuids != null) combined.addAll(syncedMealUuids);
        if (syncedSmsLogUuids != null) combined.addAll(syncedSmsLogUuids);
        return combined;
    }

    public List<String> getSyncedDepositUuids() { return syncedDepositUuids != null ? syncedDepositUuids : new ArrayList<>(); }
    public List<String> getSyncedExpenseUuids() { return syncedExpenseUuids != null ? syncedExpenseUuids : new ArrayList<>(); }
    public List<String> getSyncedMealUuids() { return syncedMealUuids != null ? syncedMealUuids : new ArrayList<>(); }
    public List<String> getSyncedSmsLogUuids() { return syncedSmsLogUuids != null ? syncedSmsLogUuids : new ArrayList<>(); }
}
