package com.smartmess.android.notification;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.AppNotificationDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.model.AppNotification;
import com.smartmess.android.model.User;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.Map;
import java.util.UUID;

/**
 * Core Firebase Cloud Messaging service for SmartMess Android.
 * Handles incoming push notifications, background/foreground message processing,
 * offline SQLite persistence, and heads-up system tray alerts.
 */
public class SmartMessFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "SmartMessFCM";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.i(TAG, "New FCM Token received: " + token);

        try {
            SessionManager sessionManager = new SessionManager(this);
            if (sessionManager.isLoggedIn()) {
                FirebaseMessagingHelper.updateSubscriptions(
                        sessionManager.getMessId(),
                        sessionManager.getUserId()
                );
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error in onNewToken: " + t.getMessage(), t);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.i(TAG, "FCM message received from: " + remoteMessage.getFrom());

        try {
            Map<String, String> data = remoteMessage.getData();
            RemoteMessage.Notification notification = remoteMessage.getNotification();

            String title = data.get("title");
            if (title == null && notification != null) {
                title = notification.getTitle();
            }
            if (title == null || title.trim().isEmpty()) {
                title = "মেস নোটিফিকেশন";
            }

            String message = data.get("message");
            if (message == null && notification != null) {
                message = notification.getBody();
            }
            if (message == null) {
                message = "";
            }

            SessionManager sessionManager = new SessionManager(this);
            long currentUserId = sessionManager.getUserId();

            String targetUserIdStr = data.get("target_user_id");
            if (targetUserIdStr != null && !targetUserIdStr.isEmpty()) {
                try {
                    long targetUserId = Long.parseLong(targetUserIdStr);
                    if (targetUserId > 0 && targetUserId != currentUserId) {
                        Log.d(TAG, "Notification target user mismatch, skipping: " + targetUserId);
                        return;
                    }
                } catch (NumberFormatException ignored) {}
            }

            String uuid = data.get("uuid");
            if (uuid == null || uuid.trim().isEmpty()) {
                uuid = UUID.randomUUID().toString();
            }

            DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
            AppNotificationDao notificationDao = new AppNotificationDao(dbHelper);

            if (notificationDao.existsByUuid(uuid)) {
                Log.d(TAG, "Notification with uuid already stored locally: " + uuid);
                return;
            }

            long messId = sessionManager.getMessId();
            String messIdStr = data.get("mess_id");
            if (messIdStr != null && !messIdStr.isEmpty()) {
                try {
                    messId = Long.parseLong(messIdStr);
                } catch (NumberFormatException ignored) {}
            }

            String type = data.get("type");
            if (type == null || type.trim().isEmpty()) {
                type = "notice";
            }

            String channel = data.get("channel");
            if (channel == null || channel.trim().isEmpty()) {
                channel = "push";
            }

            String createdAt = data.get("created_at");
            if (createdAt == null || createdAt.trim().isEmpty()) {
                createdAt = DateTimeUtils.nowIso();
            }

            AppNotification notif = new AppNotification();
            notif.setUuid(uuid);
            notif.setMessId(messId);
            notif.setUserId(currentUserId);
            notif.setTitle(title);
            notif.setMessage(message);
            notif.setType(type);
            notif.setChannel(channel);
            notif.setRead(false);
            notif.setCreatedAt(createdAt);

            long rowId = notificationDao.insert(notif);
            notif.setId(rowId);

            // 1. Show Heads-Up Android Tray Notification with sound & vibration
            PushNotificationManager.showSystemNotification(this, rowId, title, message, type, channel);

            // 2. Handle Member Joined Event data
            if ("role".equalsIgnoreCase(type) || "member_joined".equalsIgnoreCase(data.get("event"))) {
                handleMemberJoinedPayload(data, dbHelper);
            }

            // 3. Notify all actively visible UI screens (ActivityNotificationCenter, FragmentNotifications, MainActivity)
            FirebaseMessagingHelper.notifyListeners(notif);

        } catch (Throwable t) {
            Log.e(TAG, "Error handling FCM message: " + t.getMessage(), t);
        }
    }

    private void handleMemberJoinedPayload(Map<String, String> data, DatabaseHelper dbHelper) {
        try {
            String userIdStr = data.get("user_id");
            if (userIdStr != null && !userIdStr.isEmpty()) {
                long userId = Long.parseLong(userIdStr);
                UserDao userDao = new UserDao(dbHelper);
                User existing = userDao.getById(userId);
                if (existing == null) {
                    User newUser = new User();
                    newUser.setId(userId);
                    newUser.setName(data.get("user_name"));
                    newUser.setPhone(data.get("phone"));
                    newUser.setEmail(data.get("email"));
                    newUser.setRole(data.get("user_role") != null ? data.get("user_role") : "member");
                    newUser.setAvatarUrl(data.get("avatar_url"));
                    newUser.setStatus("active");
                    userDao.insert(newUser);
                }
            }

            // Trigger background sync to keep SQLite full parity
            PushNotificationSyncWorker.runImmediateSync(this);
        } catch (Throwable t) {
            Log.e(TAG, "Error processing member joined payload: " + t.getMessage());
        }
    }
}
