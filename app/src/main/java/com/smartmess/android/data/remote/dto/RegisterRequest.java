package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("password")
    private String password;

    @SerializedName("mess_name")
    private String messName;

    @SerializedName("location")
    private String location;

    @SerializedName("billing_cycle")
    private String billingCycle;

    @SerializedName("invite_code")
    private String inviteCode;

    // Default constructor for Gson
    public RegisterRequest() {}

    // Path A: Create Mess Constructor
    public static RegisterRequest createMess(String name, String email, String phone, String password,
                                             String messName, String location, String billingCycle) {
        RegisterRequest req = new RegisterRequest();
        req.name = name;
        req.email = email;
        req.phone = phone;
        req.password = password;
        req.messName = messName;
        req.location = location;
        req.billingCycle = (billingCycle != null && !billingCycle.isEmpty()) ? billingCycle : "monthly";
        req.inviteCode = null;
        return req;
    }

    // Path B: Join Mess Constructor
    public static RegisterRequest joinMess(String name, String email, String phone, String password,
                                           String inviteCode) {
        RegisterRequest req = new RegisterRequest();
        req.name = name;
        req.email = email;
        req.phone = phone;
        req.password = password;
        req.inviteCode = inviteCode;
        req.messName = null;
        req.location = null;
        return req;
    }

    // Backward-compatibility constructor
    public RegisterRequest(String name, String phone, String password, String messName, String inviteCode) {
        this.name = name;
        this.phone = phone;
        this.password = password;
        this.messName = messName;
        this.inviteCode = inviteCode;
        this.billingCycle = "monthly";
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public String getMessName() { return messName; }
    public String getLocation() { return location; }
    public String getBillingCycle() { return billingCycle; }
    public String getInviteCode() { return inviteCode; }
}
