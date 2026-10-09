package com.smartmess.android.model;

import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SaasPlan {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name; // Basic Mess, Standard Pro, Annual Hall Enterprise

    @SerializedName("price")
    private double price;

    @SerializedName("duration_in_days")
    private int durationInDays = 30;

    @SerializedName("feature_list")
    private List<String> featureList;

    @SerializedName("is_popular")
    private boolean isPopular;

    private String featuresJson;
    private int status = 1; // 1=active, 0=inactive
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

    public String getName() { return name != null ? name : "SmartMess Plan"; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getDurationInDays() { return durationInDays > 0 ? durationInDays : 30; }
    public void setDurationInDays(int durationInDays) { this.durationInDays = durationInDays; }

    public boolean isPopular() { return isPopular; }
    public void setPopular(boolean popular) { isPopular = popular; }

    public List<String> getFeatureList() {
        if (featureList != null && !featureList.isEmpty()) {
            return featureList;
        }

        List<String> list = new ArrayList<>();
        list.add(maxMembers >= 9999 ? "Unlimited Members" : "Up to " + maxMembers + " Members");
        list.add(smsSim ? "Hardware SIM SMS Reminders" : "Single SMS Reminders");
        if (smsCloud) list.add("Cloud SMS Gateway API");
        if (ocrReceipt) list.add("Smart Receipt OCR Scanner");
        if (pdfBranding) list.add("Watermark-Free Branded PDF & Excel Exports");
        if (adFree) list.add("100% Ad-Free Clean Experience");

        if (list.size() < 3) {
            list.add("Instant SQLite Local Storage");
            list.add("Interactive Balance Sheet");
        }
        return list;
    }

    public void setFeatureList(List<String> featureList) {
        this.featureList = featureList;
    }

    public String getFeaturesJson() {
        if (featuresJson != null && !featuresJson.isEmpty()) {
            return featuresJson;
        }
        // Build JSON representation with feature_list for local caching
        try {
            JSONObject obj = new JSONObject();
            obj.put("max_members", maxMembers);
            obj.put("sms_sim", smsSim);
            obj.put("sms_cloud", smsCloud);
            obj.put("ocr_receipt", ocrReceipt);
            obj.put("pdf_branding", pdfBranding);
            obj.put("ad_free", adFree);
            obj.put("is_popular", isPopular);
            if (featureList != null) {
                JSONArray arr = new JSONArray(featureList);
                obj.put("feature_list", arr);
            }
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

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
            if (obj.has("is_popular")) isPopular = obj.optBoolean("is_popular", false);

            if (obj.has("feature_list")) {
                JSONArray arr = obj.optJSONArray("feature_list");
                if (arr != null) {
                    featureList = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        featureList.add(arr.getString(i));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public String getFormattedDuration() {
        if (durationInDays >= 360) return "per year";
        if (durationInDays >= 28 && durationInDays <= 31) return "per month";
        return "for " + durationInDays + " days";
    }
}
