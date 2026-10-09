package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import com.smartmess.android.model.SaasPlan;

import java.util.List;

public class PlansResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("plans")
    private List<SaasPlan> plans;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<SaasPlan> getPlans() {
        return plans;
    }

    public void setPlans(List<SaasPlan> plans) {
        this.plans = plans;
    }
}
