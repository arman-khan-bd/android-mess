package com.smartmess.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CreateMemberRequest {

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("name")
    private String name;

    @SerializedName("phone")
    private String phone;

    @SerializedName("role")
    private String role;

    @SerializedName("password")
    private String password;

    public CreateMemberRequest() {}

    public CreateMemberRequest(String uuid, String name, String phone, String role, String password) {
        this.uuid = uuid;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.password = password;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
