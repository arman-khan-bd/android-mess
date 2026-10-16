package com.smartmess.android.realtime;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.AppNotificationDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.model.AppNotification;
import com.smartmess.android.model.User;
import com.smartmess.android.notification.PushNotificationManager;
import com.smartmess.android.notification.PushNotificationSyncWorker;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import org.json.JSONObject;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * High-performance, ultra-low-bandwidth Pusher Realtime Client for SmartMess Android.
 * Communicates directly over lightweight WebSockets using the app's existing OkHttp library.
 * Zero polling pressure, instant notification delivery to all mess members within milliseconds.
 */
public class PusherRealtimeManager {

    private static final String TAG = "PusherRealtime";

    public static final String PUSHER_APP_ID = "2202946";
    public static final String PUSHER_KEY = "06ba25700394aec43f0a";
    public static final String PUSHER_CLUSTER = "ap1";

    private static final String PUSHER_WS_URL =
            "wss://ws-" + PUSHER_CLUSTER + ".pusher.com:443/app/" + PUSHER_KEY + "?protocol=7&client=smartmess-android&version=1.0";

    private static volatile PusherRealtimeManager sInstance;

    private final Context context;
    private final SessionManager sessionManager;
    private final AppNotificationDao notificationDao;
    private final UserDao userDao;
    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    private WebSocket webSocket;
    private boolean isConnected = false;
    private boolean isConnecting = false;
    private int reconnectAttempts = 0;

    private final Set<NotificationListener> listeners =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public interface NotificationListener {
        void onNotificationReceived(AppNotification notification);
    }

    private PusherRealtimeManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.sessionManager = new SessionManager(context);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        this.notificationDao = new AppNotificationDao(dbHelper);
        this.userDao = new UserDao(dbHelper);
        this.mainHandler = new Handler(Looper.getMainLooper());

        // 30s keepalive ping interval ensures NAT routers don't kill the TCP socket
        this.httpClient = new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for WebSockets
                .retryOnConnectionFailure(true)
                .build();

        registerNetworkMonitor();
    }

    public static PusherRealtimeManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (PusherRealtimeManager.class) {
                if (sInstance == null) {
                    sInstance = new PusherRealtimeManager(context);
                }
            }
        }
        return sInstance;
    }

    public static void init(Context context) {
        getInstance(context).connect();
    }

    public void addListener(NotificationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(NotificationListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Connect or reconnect to Pusher WebSocket server.
     */
    public synchronized void connect() {
        if (isConnected || isConnecting) {
            return;
        }

        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in, skipping Pusher connection.");
            return;
        }

        isConnecting = true;
        Log.i(TAG, "Connecting to Pusher WebSocket: " + PUSHER_WS_URL);

        Request request = new Request.Builder()
                .url(PUSHER_WS_URL)
                .build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {
                Log.i(TAG, "Pusher WebSocket connection opened.");
                isConnected = true;
                isConnecting = false;
                reconnectAttempts = 0;
            }

            @Override
            public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
                handleIncomingMessage(ws, text);
            }

            @Override
            public void onClosing(@NonNull WebSocket ws, int code, @NonNull String reason) {
                Log.w(TAG, "Pusher WebSocket closing: " + code + " / " + reason);
                isConnected = false;
                isConnecting = false;
            }

            @Override
            public void onClosed(@NonNull WebSocket ws, int code, @NonNull String reason) {
                Log.w(TAG, "Pusher WebSocket closed: " + code + " / " + reason);
                isConnected = false;
                isConnecting = false;
                scheduleReconnect();
            }

            @Override
            public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, @Nullable Response response) {
                Log.e(TAG, "Pusher WebSocket failure: " + t.getMessage(), t);
                isConnected = false;
                isConnecting = false;
                scheduleReconnect();
            }
        });
    }

    /**
     * Disconnects current socket and cleans up resources.
     */
    public synchronized void disconnect() {
        if (webSocket != null) {
            try {
                webSocket.close(1000, "App closed");
            } catch (Throwable ignored) {}
            webSocket = null;
        }
        isConnected = false;
        isConnecting = false;
    }

    /**
     * Called when session details (messId, userId) change or user switches mess.
     */
    public void updateSubscription() {
        if (isConnected && webSocket != null) {
            subscribeChannels(webSocket);
        } else {
            connect();
        }
    }

    private void handleIncomingMessage(WebSocket ws, String text) {
        try {
            JSONObject json = new JSONObject(text);
            String event = json.optString("event", "");

            if ("pusher:connection_established".equals(event)) {
                Log.i(TAG, "Pusher handshake succeeded. Subscribing to mess channels...");
                subscribeChannels(ws);
            } else if ("pusher:ping".equals(event)) {
                // Heartbeat response
                ws.send("{\"event\":\"pusher:pong\",\"data\":{}}");
            } else if ("pusher_internal:subscription_succeeded".equals(event)) {
                String channel = json.optString("channel", "");
                Log.i(TAG, "Subscribed successfully to channel: " + channel);
            } else if ("AppNotificationEvent".equals(event)) {
                processAppNotification(json);
            } else if ("MemberJoinedEvent".equals(event)) {
                processMemberJoined(json);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error handling Pusher message: " + t.getMessage(), t);
        }
    }

    private void subscribeChannels(WebSocket ws) {
        if (ws == null) return;
        long messId = sessionManager.getMessId();
        long userId = sessionManager.getUserId();

        if (messId > 0) {
            String messChannel = "mess." + messId;
            String subMess = "{\"event\":\"pusher:subscribe\",\"data\":{\"channel\":\"" + messChannel + "\"}}";
            ws.send(subMess);
            Log.d(TAG, "Sent subscribe for: " + messChannel);
        }

        if (userId > 0) {
            String userChannel = "user." + userId;
            String subUser = "{\"event\":\"pusher:subscribe\",\"data\":{\"channel\":\"" + userChannel + "\"}}";
            ws.send(subUser);
            Log.d(TAG, "Sent subscribe for: " + userChannel);
        }
    }

    private void processAppNotification(JSONObject rootJson) {
        try {
            JSONObject dataObj;
            Object rawData = rootJson.get("data");
            if (rawData instanceof String) {
                dataObj = new JSONObject((String) rawData);
            } else if (rawData instanceof JSONObject) {
                dataObj = (JSONObject) rawData;
            } else {
                return;
            }

            long targetUserId = dataObj.optLong("target_user_id", 0);
            long currentUserId = sessionManager.getUserId();

            // Direct message filtering
            if (targetUserId > 0 && targetUserId != currentUserId) {
                Log.d(TAG, "Ignoring notification targeted to another user: " + targetUserId);
                return;
            }

            String uuid = dataObj.optString("uuid", UUID.randomUUID().toString());
            if (notificationDao.existsByUuid(uuid)) {
                Log.d(TAG, "Notification with uuid already exists in local DB: " + uuid);
                return;
            }

            long messId = dataObj.optLong("mess_id", sessionManager.getMessId());
            String title = dataObj.optString("title", "মেস নোটিফিকেশন");
            String message = dataObj.optString("message", "");
            String type = dataObj.optString("type", "notice");
            String channel = dataObj.optString("channel", "push");
            String createdAt = dataObj.optString("created_at", DateTimeUtils.nowIso());

            AppNotification notif = new AppNotification();
            notif.setUuid(uuid);
            notif.setMessId(messId);
            notif.setUserId(targetUserId);
            notif.setTitle(title);
            notif.setMessage(message);
            notif.setType(type);
            notif.setChannel(channel);
            notif.setRead(false);
            notif.setCreatedAt(createdAt);

            long rowId = notificationDao.insert(notif);
            notif.setId(rowId);

            // Display heads-up system notification with vibration and sound
            PushNotificationManager.showSystemNotification(context, rowId, title, message, type, channel);

            // Notify active in-app screens
            mainHandler.post(() -> {
                for (NotificationListener listener : listeners) {
                    try {
                        listener.onNotificationReceived(notif);
                    } catch (Throwable t) {
                        Log.e(TAG, "Error in NotificationListener: " + t.getMessage());
                    }
                }
            });

        } catch (Throwable t) {
            Log.e(TAG, "Error processing AppNotificationEvent: " + t.getMessage(), t);
        }
    }

    private void processMemberJoined(JSONObject rootJson) {
        try {
            JSONObject dataObj;
            Object rawData = rootJson.get("data");
            if (rawData instanceof String) {
                dataObj = new JSONObject((String) rawData);
            } else if (rawData instanceof JSONObject) {
                dataObj = (JSONObject) rawData;
            } else {
                return;
            }

            long userId = dataObj.optLong("user_id", 0);
            String name = dataObj.optString("user_name", "সদস্য");
            String phone = dataObj.optString("phone", "");
            String email = dataObj.optString("email", "");
            String role = dataObj.optString("user_role", "member");
            String avatarUrl = dataObj.optString("avatar_url", null);
            long messId = dataObj.optLong("mess_id", sessionManager.getMessId());

            if (userId > 0) {
                User existing = userDao.getById(userId);
                if (existing == null) {
                    User newUser = new User();
                    newUser.setId(userId);
                    newUser.setName(name);
                    newUser.setPhone(phone);
                    newUser.setEmail(email);
                    newUser.setRole(role);
                    newUser.setMessId(messId);
                    newUser.setAvatarUrl(avatarUrl);
                    newUser.setStatus("active");
                    userDao.insert(newUser);
                }
            }

            // Also trigger background pull so all tables remain in sync
            PushNotificationSyncWorker.runImmediateSync(context);

        } catch (Throwable t) {
            Log.e(TAG, "Error processing MemberJoinedEvent: " + t.getMessage(), t);
        }
    }

    private void scheduleReconnect() {
        if (!sessionManager.isLoggedIn()) return;

        reconnectAttempts++;
        long delaySeconds = Math.min((long) Math.pow(2, reconnectAttempts), 30L);
        Log.i(TAG, "Scheduling Pusher reconnect in " + delaySeconds + " seconds (attempt " + reconnectAttempts + ")");

        mainHandler.postDelayed(() -> {
            if (!isConnected && !isConnecting) {
                connect();
            }
        }, delaySeconds * 1000L);
    }

    private void registerNetworkMonitor() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        Log.i(TAG, "Network became available. Checking Pusher connection...");
                        if (!isConnected && !isConnecting) {
                            connect();
                        }
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        Log.w(TAG, "Network lost.");
                        isConnected = false;
                    }
                });
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not register NetworkCallback: " + t.getMessage());
        }
    }
}
