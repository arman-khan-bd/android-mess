package com.smartmess.android.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.JsonElement;
import com.smartmess.android.model.PlanCapabilities;

public class SessionManager {

    public static final String ACTION_CAPABILITIES_UPDATED = "com.smartmess.android.ACTION_CAPABILITIES_UPDATED";

    private static final String PREF_NAME = "smart_mess_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_UUID = "user_uuid";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_MESS_ID = "mess_id";
    private static final String KEY_MESS_UUID = "mess_uuid";
    private static final String KEY_MESS_NAME = "mess_name";
    private static final String KEY_LAST_SYNC = "last_sync_timestamp";
    private static final String KEY_PLAN_CAPABILITIES = "plan_capabilities_json";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void createSession(long userId, String userUuid, String name, String phone, String role,
                              long messId, String messUuid, String messName, String token) {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USER_UUID, userUuid)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_PHONE, phone)
                .putString(KEY_USER_ROLE, role)
                .putLong(KEY_MESS_ID, messId)
                .putString(KEY_MESS_UUID, messUuid)
                .putString(KEY_MESS_NAME, messName)
                .putString(KEY_AUTH_TOKEN, token)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, 0);
    }

    public String getUserUuid() {
        return prefs.getString(KEY_USER_UUID, "");
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Member");
    }

    public String getUserPhone() {
        return prefs.getString(KEY_USER_PHONE, "");
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "member");
    }

    public long getMessId() {
        return prefs.getLong(KEY_MESS_ID, 0);
    }

    public String getMessUuid() {
        return prefs.getString(KEY_MESS_UUID, "");
    }

    public String getMessName() {
        return prefs.getString(KEY_MESS_NAME, "My Mess");
    }

    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, "");
    }

    public String getLastSyncTimestamp() {
        return prefs.getString(KEY_LAST_SYNC, "1970-01-01 00:00:00");
    }

    public void setLastSyncTimestamp(String timestamp) {
        prefs.edit().putString(KEY_LAST_SYNC, timestamp).apply();
    }

    public boolean isManager() {
        String role = getUserRole();
        return "manager".equalsIgnoreCase(role) || "superadmin".equalsIgnoreCase(role);
    }

    public boolean isAssistant() {
        return "assistant".equalsIgnoreCase(role());
    }

    public boolean canLogExpenses() {
        return isManager() || isAssistant();
    }

    public String role() {
        return getUserRole();
    }

    // =========================================================================
    // Dynamic Free vs Pro Capabilities Store & Auto-Unlock Engine
    // =========================================================================

    public void updatePlanCapabilities(PlanCapabilities caps) {
        if (caps != null) {
            prefs.edit().putString(KEY_PLAN_CAPABILITIES, caps.toJson()).apply();
        }
    }

    public void updatePlanCapabilities(JsonElement element) {
        if (element != null && element.isJsonObject()) {
            PlanCapabilities caps = PlanCapabilities.fromJson(element);
            updatePlanCapabilities(caps);
        }
    }

    public void updatePlanCapabilities(String jsonStr) {
        if (jsonStr != null && !jsonStr.trim().isEmpty()) {
            prefs.edit().putString(KEY_PLAN_CAPABILITIES, jsonStr).apply();
        }
    }

    public PlanCapabilities getPlanCapabilities() {
        String jsonStr = prefs.getString(KEY_PLAN_CAPABILITIES, null);
        return PlanCapabilities.fromJson(jsonStr);
    }

    public boolean isPro() {
        return getPlanCapabilities().isPro();
    }

    public boolean hasCapability(String key) {
        return getPlanCapabilities().hasCapability(key);
    }

    public void broadcastCapabilitiesUpdated(Context context) {
        if (context != null) {
            Intent intent = new Intent(ACTION_CAPABILITIES_UPDATED);
            context.sendBroadcast(intent);
        }
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
