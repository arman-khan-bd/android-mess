package com.smartmess.android.model;

public class Mess {
    private long id;
    private String uuid;
    private String name;
    private String inviteCode;
    private String billingCycle; // 'monthly', 'weekly', 'custom'
    private int cycleStartDay;
    private String mealCutoffTime; // e.g. "22:00:00"
    private double perSmsCost;
    private Long currentPlanId;
    private String planExpiresAt;
    private String createdAt;
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

    public Long getCurrentPlanId() { return currentPlanId; }
    public void setCurrentPlanId(Long currentPlanId) { this.currentPlanId = currentPlanId; }

    public String getPlanExpiresAt() { return planExpiresAt; }
    public void setPlanExpiresAt(String planExpiresAt) { this.planExpiresAt = planExpiresAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
