package com.smartmess.android.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.security.ProviderInstaller;

/**
 * Enterprise Security Provider Installer Helper
 * Dynamically updates the device's Google Play Services security Provider on older Android versions (API 23 and below)
 * to prevent CertPathValidatorException and enable modern TLS Root CAs.
 */
public class SecurityProviderHelper {

    private static final String TAG = "SecurityProvider";
    private static boolean sIsInstalled = false;

    public static void installIfNeeded(Context context) {
        if (sIsInstalled || context == null) return;

        try {
            ProviderInstaller.installIfNeededAsync(context.getApplicationContext(), new ProviderInstaller.ProviderInstallListener() {
                @Override
                public void onProviderInstalled() {
                    sIsInstalled = true;
                    Log.i(TAG, "Google Play Services Security Provider successfully installed (Modern Root CAs active)");
                }

                @Override
                public void onProviderInstallFailed(int errorCode, Intent recoveryIntent) {
                    Log.w(TAG, "Security Provider installation failed (errorCode: " + errorCode + "). In-app bundled ISRG Root X1 TrustManager will handle TLS.");
                }
            });
        } catch (Throwable t) {
            Log.w(TAG, "ProviderInstaller unavailable or skipped: " + t.getMessage());
        }
    }
}
