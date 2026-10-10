package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CheckoutRequest {

    @SerializedName("plan_id")
    private long planId;

    @SerializedName("payment_method")
    private String paymentMethod;

    @SerializedName("mess_id")
    private Long messId;

    @SerializedName("mess_uuid")
    private String messUuid;

    public CheckoutRequest() {}

    public CheckoutRequest(long planId, String paymentMethod) {
        this.planId = planId;
        this.paymentMethod = paymentMethod;
    }

    public CheckoutRequest(long planId, String paymentMethod, Long messId, String messUuid) {
        this.planId = planId;
        this.paymentMethod = paymentMethod;
        this.messId = messId;
        this.messUuid = messUuid;
    }

    public long getPlanId() { return planId; }
    public void setPlanId(long planId) { this.planId = planId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Long getMessId() { return messId; }
    public void setMessId(Long messId) { this.messId = messId; }

    public String getMessUuid() { return messUuid; }
    public void setMessUuid(String messUuid) { this.messUuid = messUuid; }
}
