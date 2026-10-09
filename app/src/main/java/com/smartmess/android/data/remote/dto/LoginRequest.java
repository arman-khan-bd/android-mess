package com.smartmess.android.data.remote.dto;

public class LoginRequest {
    private String phone;
    private String password;

    public LoginRequest(String phone, String password) {
        this.phone = phone;
        this.password = password;
    }

    public String getPhone() { return phone; }
    public String getPassword() { return password; }
}
