package com.smartmess.android.data.remote.dto;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class CheckoutResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("plan_tier")
    private String planTier;

    @SerializedName("plan_capabilities")
    private JsonElement planCapabilities;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPlanTier() { return planTier; }
    public void setPlanTier(String planTier) { this.planTier = planTier; }

    public JsonElement getPlanCapabilities() { return planCapabilities; }
    public void setPlanCapabilities(JsonElement planCapabilities) { this.planCapabilities = planCapabilities; }
}
