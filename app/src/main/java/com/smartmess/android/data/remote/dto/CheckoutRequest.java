package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CheckoutRequest {

    @SerializedName("plan_id")
    private long planId;

    @SerializedName("payment_method")
    private String paymentMethod;

    public CheckoutRequest() {}

    public CheckoutRequest(long planId, String paymentMethod) {
        this.planId = planId;
        this.paymentMethod = paymentMethod;
    }

    public long getPlanId() { return planId; }
    public void setPlanId(long planId) { this.planId = planId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
