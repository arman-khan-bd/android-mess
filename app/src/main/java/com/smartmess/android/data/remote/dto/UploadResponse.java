package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class UploadResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("voucher_image_url")
    private String voucherImageUrl;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getVoucherImageUrl() { return voucherImageUrl; }
    public void setVoucherImageUrl(String voucherImageUrl) { this.voucherImageUrl = voucherImageUrl; }
}
