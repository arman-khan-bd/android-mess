package com.smartmess.android.notification;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.smartmess.android.data.remote.ApiConfig;
import com.smartmess.android.model.AppNotification;
import com.smartmess.android.utils.SessionManager;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class for managing Firebase Cloud Messaging subscriptions and in-app listeners.
 */
public class FirebaseMessagingHelper {

    private static final String TAG = "FirebaseHelper";

    public interface NotificationListener {
        void onNotificationReceived(AppNotification notification);
    }

    private static final Set<NotificationListener> listeners =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void init(Context context) {
        if (context == null) return;
        SessionManager sessionManager = new SessionManager(context);

        if (sessionManager.isLoggedIn()) {
            long messId = sessionManager.getMessId();
            long userId = sessionManager.getUserId();
            updateSubscriptions(messId, userId);
        }

        // Fetch and register current FCM registration token
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String token = task.getResult();
                    Log.i(TAG, "FCM Device Token: " + token);
                    registerTokenWithBackend(context, token);
                }
            });
        } catch (Throwable t) {
            Log.w(TAG, "Error fetching FCM token: " + t.getMessage());
        }
    }

    public static void registerTokenWithBackend(Context context, String token) {
        if (context == null || token == null || token.trim().isEmpty()) return;
        new Thread(() -> {
            try {
                SessionManager sm = new SessionManager(context);
                String authToken = sm.getAuthToken();
                if (authToken == null || authToken.trim().isEmpty() || "offline_session_token".equals(authToken)) {
                    return;
                }

                String baseUrl = ApiConfig.getBaseUrl();
                URL url = new URL(baseUrl.replaceAll("/+$", "") + "/notifications/register-token");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                conn.setDoOutput(true);
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                JSONObject payload = new JSONObject();
                payload.put("token", token);
                payload.put("platform", "android");
                payload.put("device_brand", Build.MANUFACTURER);
                payload.put("device_model", Build.MODEL);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes("UTF-8"));
                }
                int code = conn.getResponseCode();
                Log.d(TAG, "registerTokenWithBackend status code: " + code);
                conn.disconnect();
            } catch (Throwable t) {
                Log.d(TAG, "registerTokenWithBackend failed: " + t.getMessage());
            }
        }).start();
    }

    public static void updateSubscriptions(long messId, long userId) {
        try {
            if (messId > 0) {
                String messTopic = "mess_" + messId;
                FirebaseMessaging.getInstance().subscribeToTopic(messTopic)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.i(TAG, "Subscribed successfully to topic: " + messTopic);
                            }
                        });
            }

            if (userId > 0) {
                String userTopic = "user_" + userId;
                FirebaseMessaging.getInstance().subscribeToTopic(userTopic)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.i(TAG, "Subscribed successfully to topic: " + userTopic);
                            }
                        });
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error subscribing to FCM topics: " + t.getMessage());
        }
    }

    public static void clearSubscriptions(long messId, long userId) {
        try {
            if (messId > 0) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("mess_" + messId);
            }
            if (userId > 0) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("user_" + userId);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error unsubscribing from FCM topics: " + t.getMessage());
        }
    }

    public static void addListener(NotificationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public static void removeListener(NotificationListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public static void notifyListeners(AppNotification notification) {
        if (notification == null) return;
        mainHandler.post(() -> {
            for (NotificationListener listener : listeners) {
                try {
                    listener.onNotificationReceived(notification);
                } catch (Throwable t) {
                    Log.e(TAG, "Error in NotificationListener: " + t.getMessage());
                }
            }
        });
    }
}
