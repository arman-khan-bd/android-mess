package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import com.smartmess.android.model.Deposit;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.SmsLog;

import java.util.ArrayList;
import java.util.List;

public class SyncPayload {

    @SerializedName("mess_uuid")
    private String messUuid;

    @SerializedName("deposits")
    private List<Deposit> deposits = new ArrayList<>();

    @SerializedName("expenses")
    private List<Expense> expenses = new ArrayList<>();

    @SerializedName("meals")
    private List<Meal> meals = new ArrayList<>();

    @SerializedName("sms_logs")
    private List<SmsLog> smsLogs = new ArrayList<>();

    public SyncPayload() {}

    public SyncPayload(String messUuid, List<Deposit> deposits, List<Expense> expenses, List<Meal> meals, List<SmsLog> smsLogs) {
        this.messUuid = messUuid;
        this.deposits = deposits != null ? deposits : new ArrayList<>();
        this.expenses = expenses != null ? expenses : new ArrayList<>();
        this.meals = meals != null ? meals : new ArrayList<>();
        this.smsLogs = smsLogs != null ? smsLogs : new ArrayList<>();
    }

    public String getMessUuid() { return messUuid; }
    public void setMessUuid(String messUuid) { this.messUuid = messUuid; }

    public List<Deposit> getDeposits() { return deposits; }
    public void setDeposits(List<Deposit> deposits) { this.deposits = deposits; }

    public List<Expense> getExpenses() { return expenses; }
    public void setExpenses(List<Expense> expenses) { this.expenses = expenses; }

    public List<Meal> getMeals() { return meals; }
    public void setMeals(List<Meal> meals) { this.meals = meals; }

    public List<SmsLog> getSmsLogs() { return smsLogs; }
    public void setSmsLogs(List<SmsLog> smsLogs) { this.smsLogs = smsLogs; }

    public int totalRecordCount() {
        return deposits.size() + expenses.size() + meals.size() + smsLogs.size();
    }
}
