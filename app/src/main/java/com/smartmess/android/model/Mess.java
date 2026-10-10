package com.smartmess.android.model;

import com.google.gson.annotations.SerializedName;

public class Mess {
    @SerializedName("id")
    private long id;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("name")
    private String name;

    @SerializedName(value = "inviteCode", alternate = {"invite_code"})
    private String inviteCode;

    @SerializedName(value = "billingCycle", alternate = {"billing_cycle"})
    private String billingCycle; // 'monthly', 'weekly', 'custom'

    @SerializedName(value = "cycleStartDay", alternate = {"cycle_start_day"})
    private int cycleStartDay;

    @SerializedName(value = "mealCutoffTime", alternate = {"meal_cutoff_time"})
    private String mealCutoffTime; // e.g. "22:00:00"

    @SerializedName(value = "perSmsCost", alternate = {"per_sms_cost"})
    private double perSmsCost;

    @SerializedName(value = "targetMealBudget", alternate = {"target_meal_budget"})
    private double targetMealBudget = 70.0;

    @SerializedName(value = "currentPlanId", alternate = {"current_plan_id"})
    private Long currentPlanId;

    @SerializedName(value = "planExpiresAt", alternate = {"plan_expires_at"})
    private String planExpiresAt;

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    private String createdAt;

    @SerializedName(value = "updatedAt", alternate = {"updated_at"})
    private String updatedAt;

    public Mess() {}

    public Mess(String uuid, String name, String inviteCode, String billingCycle, int cycleStartDay, String mealCutoffTime, double perSmsCost) {
        this.uuid = uuid;
        this.name = name;
        this.inviteCode = inviteCode;
        this.billingCycle = billingCycle;
        this.cycleStartDay = cycleStartDay;
        this.mealCutoffTime = mealCutoffTime;
        this.perSmsCost = perSmsCost;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }

    public int getCycleStartDay() { return cycleStartDay; }
    public void setCycleStartDay(int cycleStartDay) { this.cycleStartDay = cycleStartDay; }

    public String getMealCutoffTime() { return mealCutoffTime; }
    public void setMealCutoffTime(String mealCutoffTime) { this.mealCutoffTime = mealCutoffTime; }

    public double getPerSmsCost() { return perSmsCost; }
    public void setPerSmsCost(double perSmsCost) { this.perSmsCost = perSmsCost; }

    public double getTargetMealBudget() { return targetMealBudget > 0 ? targetMealBudget : 70.0; }
    public void setTargetMealBudget(double targetMealBudget) { this.targetMealBudget = targetMealBudget; }

    public Long getCurrentPlanId() { return currentPlanId; }
    public void setCurrentPlanId(Long currentPlanId) { this.currentPlanId = currentPlanId; }

    public String getPlanExpiresAt() { return planExpiresAt; }
    public void setPlanExpiresAt(String planExpiresAt) { this.planExpiresAt = planExpiresAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
