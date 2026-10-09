package com.smartmess.android;

/**
 * =========================================================================
 * SmartMess Commercial Buyer Configuration (AppConfig.java)
 * =========================================================================
 * Modify this file to point the Android app to your cPanel production server,
 * rebrand app strings, and set up your Google AdMob advertising unit IDs.
 */
public final class AppConfig {

    private AppConfig() {
        // Prevent instantiation
    }

    // =========================================================================
    // 1. Production Backend REST API Base URL
    // =========================================================================
    // IMPORTANT: Must include the protocol (https://) and trailing slash (/)
    public static final String BASE_URL = "https://mess.e-bd.shop/api/v1/";

    // =========================================================================
    // 2. Application Identity & Localization
    // =========================================================================
    public static final String APP_NAME = "SmartMess";
    public static final String APPLICATION_ID = "com.smartmess.android";
    public static final String APP_VERSION = "2.0.0";
    public static final String CURRENCY_SYMBOL = "৳"; // e.g. $, ৳, ₹, €, £

    // =========================================================================
    // 3. Accounting & Notification Rules
    // =========================================================================
    public static final String DEFAULT_CUTOFF_TIME = "22:00:00"; // 10:00 PM cutoff
    public static final double DEFAULT_PER_SMS_COST = 0.50; // SIM SMS rate
    public static final int AUTO_SYNC_INTERVAL_MINUTES = 15;

    // =========================================================================
    // 4. Google AdMob Monetization (Commercial / Free Tier)
    // =========================================================================
    // Set to true to display Banner and Interstitial ads
    public static final boolean ENABLE_ADMOB = false;

    // Google AdMob App ID (Replace with your AdMob App ID from Google AdMob Console)
    public static final String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"; // Google Test App ID

    // Banner Ad Unit ID (Displayed at the bottom of Dashboard & Meal Sheet)
    public static final String ADMOB_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"; // Google Test Banner

    // Interstitial Ad Unit ID (Triggered after expense voucher uploads or daily audits)
    public static final String ADMOB_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // Google Test Interstitial

    // =========================================================================
    // 5. Media & Voucher Compression
    // =========================================================================
    public static final int MAX_VOUCHER_DIMENSION_PX = 1080;
    public static final int WEBP_COMPRESSION_QUALITY = 75; // 75% quality for ultra-low bandwidth

    // =========================================================================
    // 6. Support & Legal Policies
    // =========================================================================
    public static final String SUPPORT_EMAIL = "support@e-bd.shop";
    public static final String WEBSITE_URL = "https://mess.e-bd.shop";
    public static final String PRIVACY_POLICY_URL = "https://mess.e-bd.shop/privacy";
    public static final String TERMS_OF_SERVICE_URL = "https://mess.e-bd.shop/terms";
}
