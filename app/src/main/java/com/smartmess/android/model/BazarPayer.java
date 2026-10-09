package com.smartmess.android.model;

import java.io.Serializable;

public class BazarPayer implements Serializable {

    private long userId;
    private String userName;
    private double amountPaid;

    public BazarPayer() {
        this.amountPaid = 0.0;
    }

    public BazarPayer(long userId, String userName, double amountPaid) {
        this.userId = userId;
        this.userName = userName;
        this.amountPaid = amountPaid;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }
}
