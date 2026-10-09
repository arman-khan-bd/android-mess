package com.smartmess.android.data.remote.dto;

public class RegisterRequest {
    private String name;
    private String phone;
    private String password;
    private String messName;
    private String inviteCode; // optional: join existing mess

    public RegisterRequest(String name, String phone, String password, String messName, String inviteCode) {
        this.name = name;
        this.phone = phone;
        this.password = password;
        this.messName = messName;
        this.inviteCode = inviteCode;
    }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public String getMessName() { return messName; }
    public String getInviteCode() { return inviteCode; }
}
