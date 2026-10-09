package com.smartmess.android.model;

import org.json.JSONObject;

public class SaasPlan {
    private long id;
    private String name; // Basic, Standard, Enterprise
    private double price;
    private int durationInDays;
    private String featuresJson;
    private int status; // 1=active, 0=inactive
    private String createdAt;
    private String updatedAt;

    // Parsed feature cache
    private int maxMembers = 15;
    private boolean smsSim = true;
    private boolean smsCloud = false;
    private boolean ocrReceipt = false;
    private boolean pdfBranding = false;
    private boolean adFree = false;

    public SaasPlan() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getDurationInDays() { return durationInDays; }
    public void setDurationInDays(int durationInDays) { this.durationInDays = durationInDays; }

    public String getFeaturesJson() { return featuresJson; }
    public void setFeaturesJson(String featuresJson) {
        this.featuresJson = featuresJson;
        parseFeatures();
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public int getMaxMembers() { return maxMembers; }
    public boolean isSmsSim() { return smsSim; }
    public boolean isSmsCloud() { return smsCloud; }
    public boolean isOcrReceipt() { return ocrReceipt; }
    public boolean isPdfBranding() { return pdfBranding; }
    public boolean isAdFree() { return adFree; }

    private void parseFeatures() {
        if (featuresJson == null || featuresJson.trim().isEmpty()) return;
        try {
            JSONObject obj = new JSONObject(featuresJson);
            if (obj.has("max_members")) maxMembers = obj.optInt("max_members", 15);
            if (obj.has("sms_sim")) smsSim = obj.optBoolean("sms_sim", true);
            if (obj.has("sms_cloud")) smsCloud = obj.optBoolean("sms_cloud", false);
            if (obj.has("ocr_receipt")) ocrReceipt = obj.optBoolean("ocr_receipt", false);
            if (obj.has("pdf_branding")) pdfBranding = obj.optBoolean("pdf_branding", false);
            if (obj.has("ad_free")) adFree = obj.optBoolean("ad_free", false);
        } catch (Exception ignored) {}
    }
}
