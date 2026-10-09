package com.smartmess.android.model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Free vs. Pro Dynamic Capability Model
 * Parses compiled capability payload from API:
 * /auth/login, /auth/register, /sync/pull, and /plans/status
 */
public class PlanCapabilities implements Serializable {

    @SerializedName("plan_tier")
    private String planTier = "free";

    @SerializedName("is_active")
    private boolean isActive = true;

    @SerializedName("expires_at")
    private String expiresAt;

    @SerializedName("capabilities")
    private Map<String, Object> capabilities = new HashMap<>();

    public PlanCapabilities() {
        initDefaultFree();
    }

    public static PlanCapabilities createDefaultFree() {
        PlanCapabilities p = new PlanCapabilities();
        p.initDefaultFree();
        return p;
    }

    public static PlanCapabilities createFree() {
        return createDefaultFree();
    }

    public static PlanCapabilities createPro(String expiresAt) {
        PlanCapabilities p = new PlanCapabilities();
        p.setPlanTier("pro");
        p.setActive(true);
        p.setExpiresAt(expiresAt);
        p.capabilities.put("cloud_sync", true);
        p.capabilities.put("unlimited_members", true);
        p.capabilities.put("max_members", 9999);
        p.capabilities.put("pdf_export", true);
        p.capabilities.put("bulk_sms", true);
        p.capabilities.put("ocr_scanner", true);
        p.capabilities.put("full_history", true);
        p.capabilities.put("priority_support", true);
        return p;
    }

    private void initDefaultFree() {
        this.planTier = "free";
        this.isActive = true;
        if (capabilities == null) capabilities = new HashMap<>();
        capabilities.put("cloud_sync", false);
        capabilities.put("unlimited_members", false);
        capabilities.put("max_members", 6);
        capabilities.put("pdf_export", false);
        capabilities.put("bulk_sms", false);
        capabilities.put("ocr_scanner", false);
        capabilities.put("full_history", false);
        capabilities.put("priority_support", false);
    }

    public boolean isPro() {
        return "pro".equalsIgnoreCase(planTier) || "enterprise".equalsIgnoreCase(planTier);
    }

    public boolean isCloudSyncEnabled() {
        return getBooleanCap("cloud_sync", isPro());
    }

    public boolean isUnlimitedMembers() {
        return getBooleanCap("unlimited_members", isPro());
    }

    public int getMaxMembers() {
        if (isPro()) return 9999;
        if (capabilities != null && capabilities.containsKey("max_members")) {
            Object val = capabilities.get("max_members");
            if (val instanceof Number) {
                return ((Number) val).intValue();
            } else if (val instanceof String) {
                try {
                    return Integer.parseInt((String) val);
                } catch (Exception ignored) {}
            }
        }
        return 6;
    }

    public boolean isPdfExportEnabled() {
        return getBooleanCap("pdf_export", isPro());
    }

    public boolean isBulkSmsEnabled() {
        return getBooleanCap("bulk_sms", isPro());
    }

    public boolean isOcrScannerEnabled() {
        return getBooleanCap("ocr_scanner", isPro());
    }

    public boolean isFullHistoryEnabled() {
        return getBooleanCap("full_history", isPro());
    }

    public boolean isPrioritySupportEnabled() {
        return getBooleanCap("priority_support", isPro());
    }

    public boolean hasCapability(String key) {
        if (key == null) return false;
        if (isPro()) return true;
        return getBooleanCap(key, false);
    }

    private boolean getBooleanCap(String key, boolean defaultVal) {
        if (capabilities != null && capabilities.containsKey(key)) {
            Object val = capabilities.get(key);
            if (val instanceof Boolean) return (Boolean) val;
            if (val instanceof String) return "true".equalsIgnoreCase((String) val);
            if (val instanceof Number) return ((Number) val).intValue() == 1;
        }
        return defaultVal;
    }

    public static PlanCapabilities fromJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return createDefaultFree();
        }
        try {
            Gson gson = new Gson();
            PlanCapabilities p = gson.fromJson(jsonStr, PlanCapabilities.class);
            if (p != null) {
                if (p.capabilities == null) p.capabilities = new HashMap<>();
                return p;
            }
        } catch (Exception ignored) {}
        return createDefaultFree();
    }

    public static PlanCapabilities fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return createDefaultFree();
        }
        return fromJson(element.toString());
    }

    public String toJson() {
        try {
            return new Gson().toJson(this);
        } catch (Exception e) {
            return "";
        }
    }

    // Getters and Setters
    public String getPlanTier() { return planTier; }
    public void setPlanTier(String planTier) { this.planTier = planTier; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public Map<String, Object> getCapabilities() { return capabilities; }
    public void setCapabilities(Map<String, Object> capabilities) { this.capabilities = capabilities; }
}
