package com.smartmess.android.data.remote.dto;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.SaasPlan;

public class PlanStatusResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("plan_tier")
    private String planTier;

    @SerializedName("plan_capabilities")
    private JsonElement planCapabilities;

    @SerializedName("mess")
    private Mess mess;

    @SerializedName("plan")
    private SaasPlan plan;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getPlanTier() { return planTier; }
    public void setPlanTier(String planTier) { this.planTier = planTier; }

    public JsonElement getPlanCapabilities() { return planCapabilities; }
    public void setPlanCapabilities(JsonElement planCapabilities) { this.planCapabilities = planCapabilities; }

    public Mess getMess() { return mess; }
    public void setMess(Mess mess) { this.mess = mess; }

    public SaasPlan getPlan() { return plan; }
    public void setPlan(SaasPlan plan) { this.plan = plan; }
}
