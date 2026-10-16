package com.smartmess.android.sms;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class SmsDispatcher {

    private static final String TAG = "SmsDispatcher";

    public static class SimCardInfo {
        public final int subscriptionId;
        public final int slotIndex; // 0 for SIM 1, 1 for SIM 2
        public final String displayName;
        public final String carrierName;

        public SimCardInfo(int subscriptionId, int slotIndex, String displayName, String carrierName) {
            this.subscriptionId = subscriptionId;
            this.slotIndex = slotIndex;
            this.displayName = displayName;
            this.carrierName = carrierName;
        }

        public String getDisplayLabel() {
            String name = (displayName != null && !displayName.trim().isEmpty()) ? displayName : carrierName;
            if (name == null || name.trim().isEmpty()) {
                name = "Carrier " + (slotIndex + 1);
            }
            return "সিম " + (slotIndex + 1) + " (" + name + ")";
        }

        @Override
        public String toString() {
            return getDisplayLabel();
        }
    }

    /**
     * Queries active SIM cards available on device. Requires READ_PHONE_STATE permission on API 22+.
     */
    public static List<SimCardInfo> getActiveSimCards(Context context) {
        List<SimCardInfo> list = new ArrayList<>();
        if (context == null) return list;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    SubscriptionManager subManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                    if (subManager != null) {
                        List<SubscriptionInfo> subList = subManager.getActiveSubscriptionInfoList();
                        if (subList != null) {
                            for (SubscriptionInfo info : subList) {
                                int subId = info.getSubscriptionId();
                                int slot = info.getSimSlotIndex();
                                String dName = info.getDisplayName() != null ? info.getDisplayName().toString() : "";
                                String cName = info.getCarrierName() != null ? info.getCarrierName().toString() : "";
                                list.add(new SimCardInfo(subId, slot, dName, cName));
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error querying active SIM cards: " + t.getMessage());
        }
        return list;
    }

    public static boolean sendSms(String phoneNumber, String message) {
        return sendSms(null, -1, phoneNumber, message);
    }

    public static boolean sendSms(Context context, int subscriptionId, String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            return false;
        }

        try {
            SmsManager smsManager = null;
            if (subscriptionId >= 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
                    try {
                        SmsManager sysManager = context.getSystemService(SmsManager.class);
                        if (sysManager != null) {
                            smsManager = sysManager.createForSubscriptionId(subscriptionId);
                        }
                    } catch (Throwable ignored) {}
                }
                if (smsManager == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    try {
                        smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId);
                    } catch (Throwable ignored) {}
                }
            }

            if (smsManager == null) {
                smsManager = SmsManager.getDefault();
            }

            if (smsManager == null) {
                Log.e(TAG, "SmsManager unavailable on device");
                return false;
            }

            if (message.length() > 160) {
                ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            }
            return true;
        } catch (Throwable e) {
            Log.e(TAG, "Failed sending SMS to " + phoneNumber + ": " + e.getMessage(), e);
            return false;
        }
    }
}
