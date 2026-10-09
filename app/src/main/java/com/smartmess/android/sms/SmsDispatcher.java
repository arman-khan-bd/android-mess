package com.smartmess.android.sms;

import android.telephony.SmsManager;
import java.util.ArrayList;

public class SmsDispatcher {

    public static boolean sendSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            return false;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            if (message.length() > 160) {
                ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
