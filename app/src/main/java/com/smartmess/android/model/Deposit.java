package com.smartmess.android.model;

import com.google.gson.annotations.SerializedName;

public class Deposit {
    @SerializedName("id")
    private long id;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName(value = "messId", alternate = {"mess_id"})
    private long messId;

    @SerializedName(value = "userId", alternate = {"user_id"})
    private long userId;

    @SerializedName(value = "userName", alternate = {"user_name"})
    private String userName; // transient/joined

    @SerializedName("amount")
    private double amount;

    @SerializedName(value = "depositDate", alternate = {"deposit_date"})
    private String depositDate;

    @SerializedName("note")
    private String note;

    @SerializedName(value = "syncStatus", alternate = {"sync_status"})
    private int syncStatus; // 0=pending, 1=synced

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    private String createdAt;

    @SerializedName(value = "updatedAt", alternate = {"updated_at"})
    private String updatedAt;

    public Deposit() {}

    public Deposit(String uuid, long messId, long userId, double amount, String depositDate, String note) {
        this.uuid = uuid;
        this.messId = messId;
        this.userId = userId;
        this.amount = amount;
        this.depositDate = depositDate;
        this.note = note;
        this.syncStatus = 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public long getMessId() { return messId; }
    public void setMessId(long messId) { this.messId = messId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDepositDate() { return depositDate; }
    public void setDepositDate(String depositDate) { this.depositDate = depositDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public int getSyncStatus() { return syncStatus; }
    public void setSyncStatus(int syncStatus) { this.syncStatus = syncStatus; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
