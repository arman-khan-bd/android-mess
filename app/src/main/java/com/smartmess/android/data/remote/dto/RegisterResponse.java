package com.smartmess.android.data.remote.dto;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.User;

public class RegisterResponse {
    private boolean success;
    private String message;
    private String token;
    private User user;
    private Mess mess;

    @SerializedName("plan_capabilities")
    private JsonElement planCapabilities;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Mess getMess() { return mess; }
    public void setMess(Mess mess) { this.mess = mess; }

    public JsonElement getPlanCapabilities() { return planCapabilities; }
    public void setPlanCapabilities(JsonElement planCapabilities) { this.planCapabilities = planCapabilities; }
}
