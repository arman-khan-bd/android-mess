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
                FirebaseMessagingHelper.registerTokenWithBackend(this, token);
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

            // 1. Show Heads-Up Android Tray Notification with sound & vibration (skip tray popup only if self is sender)
            boolean isSelfSender = false;
            String senderUserIdStr = data.get("sender_user_id");
            if (senderUserIdStr != null && !senderUserIdStr.isEmpty()) {
                try {
                    long senderId = Long.parseLong(senderUserIdStr);
                    if (senderId > 0 && senderId == currentUserId) {
                        isSelfSender = true;
                    }
                } catch (NumberFormatException ignored) {}
            }

            if (!isSelfSender) {
                PushNotificationManager.showSystemNotification(this, rowId, title, message, type, channel);
            }

            // 2. Handle Member Joined or Manager Transferred Event data
            if ("role".equalsIgnoreCase(type) || "member_joined".equalsIgnoreCase(data.get("event")) || "manager_transferred".equalsIgnoreCase(data.get("event"))) {
                handleRoleOrMemberEvent(data, dbHelper, sessionManager);
            }

            // 3. Trigger immediate sync when bazar, expense, deposit, meal, or role is updated
            if ("bazar".equalsIgnoreCase(type) || "expense".equalsIgnoreCase(type) || "meal".equalsIgnoreCase(type)
                    || "deposit".equalsIgnoreCase(type) || "role".equalsIgnoreCase(type)) {
                PushNotificationSyncWorker.runImmediateSync(this);
                com.smartmess.android.data.sync.SyncManager.triggerSync(this);
            }

            // 4. Notify all actively visible UI screens (ActivityNotificationCenter, FragmentNotifications, MainActivity)
            FirebaseMessagingHelper.notifyListeners(notif);

        } catch (Throwable t) {
            Log.e(TAG, "Error handling FCM message: " + t.getMessage(), t);
        }
    }

    private void handleRoleOrMemberEvent(Map<String, String> data, DatabaseHelper dbHelper, SessionManager sessionManager) {
        try {
            UserDao userDao = new UserDao(dbHelper);
            long myUserId = sessionManager.getUserId();

            // Manager transfer event
            String newManagerIdStr = data.get("new_manager_id");
            String oldManagerIdStr = data.get("old_manager_id");
            if (newManagerIdStr != null && !newManagerIdStr.isEmpty()) {
                long newManagerId = Long.parseLong(newManagerIdStr);
                userDao.updateRole(newManagerId, User.ROLE_MANAGER);
                if (oldManagerIdStr != null && !oldManagerIdStr.isEmpty()) {
                    long oldManagerId = Long.parseLong(oldManagerIdStr);
                    userDao.updateRole(oldManagerId, User.ROLE_MEMBER);
                }

                // If this device belongs to the new manager, update session
                if (myUserId == newManagerId) {
                    sessionManager.setUserRole(User.ROLE_MANAGER);
                } else if (oldManagerIdStr != null && myUserId == Long.parseLong(oldManagerIdStr)) {
                    sessionManager.setUserRole(User.ROLE_MEMBER);
                }
            }

            // Member joined event
            String userIdStr = data.get("user_id");
            if (userIdStr != null && !userIdStr.isEmpty()) {
                long userId = Long.parseLong(userIdStr);
                User existing = userDao.getById(userId);
                if (existing == null) {
                    User newUser = new User();
                    newUser.setId(userId);
                    newUser.setName(data.get("user_name"));
                    newUser.setPhone(data.get("phone"));
                    newUser.setEmail(data.get("email"));
                    newUser.setRole(data.get("user_role") != null ? data.get("user_role") : User.ROLE_MEMBER);
                    newUser.setAvatarUrl(data.get("avatar_url"));
                    newUser.setStatus(User.STATUS_ACTIVE);
                    userDao.insertOrUpdate(newUser);
                }
            }

            // Trigger background sync to keep SQLite full parity
            com.smartmess.android.data.sync.SyncManager.triggerSync(this);
        } catch (Throwable t) {
            Log.e(TAG, "Error processing role/member event: " + t.getMessage());
        }
    }
}
