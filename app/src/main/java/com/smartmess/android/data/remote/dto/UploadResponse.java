package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class UploadResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("voucher_image_url")
    private String voucherImageUrl;

    @SerializedName("url")
    private String url;

    @SerializedName("avatar_url")
    private String avatarUrl;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getVoucherImageUrl() { return voucherImageUrl; }
    public void setVoucherImageUrl(String voucherImageUrl) { this.voucherImageUrl = voucherImageUrl; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getEffectiveUrl() {
        if (voucherImageUrl != null && !voucherImageUrl.trim().isEmpty()) {
            return voucherImageUrl.trim();
        }
        if (url != null && !url.trim().isEmpty()) {
            return url.trim();
        }
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            return avatarUrl.trim();
        }
        return null;
    }
}
