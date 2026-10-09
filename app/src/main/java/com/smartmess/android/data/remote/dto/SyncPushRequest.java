package com.smartmess.android.data.remote.dto;

import com.smartmess.android.model.Deposit;
import com.smartmess.android.model.Expense;
import com.smartmess.android.model.Meal;
import com.smartmess.android.model.SmsLog;

import java.util.List;

public class SyncPushRequest {
    private String messUuid;
    private List<Deposit> deposits;
    private List<Expense> expenses;
    private List<Meal> meals;
    private List<SmsLog> smsLogs;

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
}
