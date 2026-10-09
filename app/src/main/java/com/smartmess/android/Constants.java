package com.smartmess.android;

/**
 * =========================================================================
 * SmartMess Commercial Configuration Constants
 * Codecanyon Buyers: Modify this single file to rebrand the entire application
 * =========================================================================
 */
public class Constants {

    // 1. Backend Server REST API Configuration
    // Replace with your production domain where Laravel 11 backend is deployed
    public static final String SERVER_BASE_URL = "https://mess.e-bd.shop/api/v1/";

    // 2. Localization & Currency Branding
    public static final String APP_NAME = "SmartMess";
    public static final String CURRENCY_SYMBOL = "৳"; // e.g., ৳, $, ₹, €, £
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DISPLAY_DATE_FORMAT = "EEE, dd MMM yyyy";

    // 3. Default Business Logic & Cutoff Rule
    public static final String DEFAULT_CUTOFF_TIME = "22:00:00"; // 10 PM daily lock
    public static final double DEFAULT_PER_SMS_COST = 0.50; // BDT 0.50 per SIM SMS

    // 4. Google AdMob Monetization (Optional - Disabled for Premium Tier)
    // Replace with your live AdMob IDs from Google AdMob Console
    public static final boolean ENABLE_ADMOB = false; // Set to true to show ads
    public static final String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"; // Test ID
    public static final String ADMOB_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"; // Test Banner

    // 5. Image & Voucher Compression Parameters
    public static final int VOUCHER_MAX_WIDTH = 1080; // Downscale receipts to 1080px
    public static final int VOUCHER_COMPRESSION_QUALITY = 75; // 75% lossy WebP quality

    // 6. Support & Feedback
    public static final String SUPPORT_EMAIL = "support@e-bd.shop";
    public static final String PRIVACY_POLICY_URL = "https://mess.e-bd.shop/privacy";
    public static final String TERMS_OF_SERVICE_URL = "https://mess.e-bd.shop/terms";
}
