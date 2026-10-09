package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import com.smartmess.android.model.Deposit;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.SaasPlan;
import com.smartmess.android.model.SmsLog;
import com.smartmess.android.model.User;

import java.util.ArrayList;
import java.util.List;

public class SyncPullResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("server_timestamp")
    private String serverTimestamp;

    @SerializedName("mess")
    private Mess mess;

    @SerializedName("plan")
    private SaasPlan plan;

    @SerializedName("plan_capabilities")
    private com.google.gson.JsonElement planCapabilities;

    @SerializedName(value = "users", alternate = {"members"})
    private List<User> users = new ArrayList<>();

    @SerializedName("deposits")
    private List<Deposit> deposits = new ArrayList<>();

    @SerializedName("expenses")
    private List<Expense> expenses = new ArrayList<>();

    @SerializedName("meals")
    private List<Meal> meals = new ArrayList<>();

    @SerializedName(value = "sms_logs", alternate = {"smsLogs"})
    private List<SmsLog> smsLogs = new ArrayList<>();

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getServerTimestamp() { return serverTimestamp; }
    public void setServerTimestamp(String serverTimestamp) { this.serverTimestamp = serverTimestamp; }

    public Mess getMess() { return mess; }
    public void setMess(Mess mess) { this.mess = mess; }

    public SaasPlan getPlan() { return plan; }
    public void setPlan(SaasPlan plan) { this.plan = plan; }

    public com.google.gson.JsonElement getPlanCapabilities() { return planCapabilities; }
    public void setPlanCapabilities(com.google.gson.JsonElement planCapabilities) { this.planCapabilities = planCapabilities; }

    public List<User> getUsers() { return users != null ? users : new ArrayList<>(); }
    public void setUsers(List<User> users) { this.users = users; }

    public List<Deposit> getDeposits() { return deposits != null ? deposits : new ArrayList<>(); }
    public void setDeposits(List<Deposit> deposits) { this.deposits = deposits; }

    public List<Expense> getExpenses() { return expenses != null ? expenses : new ArrayList<>(); }
    public void setExpenses(List<Expense> expenses) { this.expenses = expenses; }

    public List<Meal> getMeals() { return meals != null ? meals : new ArrayList<>(); }
    public void setMeals(List<Meal> meals) { this.meals = meals; }

    public List<SmsLog> getSmsLogs() { return smsLogs != null ? smsLogs : new ArrayList<>(); }
    public void setSmsLogs(List<SmsLog> smsLogs) { this.smsLogs = smsLogs; }
}
