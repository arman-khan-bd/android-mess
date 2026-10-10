package com.smartmess.android.ui.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.AppNotificationDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.NotificationSendRequest;
import com.smartmess.android.data.remote.dto.NotificationSendResponse;
import com.smartmess.android.model.AppNotification;
import com.smartmess.android.model.User;
import com.smartmess.android.sms.SmsCostingManager;
import com.smartmess.android.utils.BatteryOptimizationHelper;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.NotificationCenterHelper;
import com.smartmess.android.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityNotificationManager extends AppCompatActivity {

    private ImageButton btnBackNotifManager;
    private TextView badgeManagerRole;

    // Battery optimization status card
    private LinearLayout cardBatteryOptimization;
    private ImageView ivBatteryStatusIcon;
    private TextView tvBatteryStatusTitle;
    private TextView tvBatteryStatusDesc;
    private MaterialButton btnFixBatteryOpt;

    // Target audience
    private RadioGroup rgTargetAudience;
    private RadioButton rbTargetBroadcast;
    private RadioButton rbTargetSingle;
    private LinearLayout layoutSingleMemberSelector;
    private Spinner spinnerTargetMember;

    // Delivery channel
    private RadioGroup rgDeliveryChannel;
    private RadioButton rbChannelPush;
    private RadioButton rbChannelSms;
    private RadioButton rbChannelBoth;

    // Category chips
    private ChipGroup chipGroupTemplates;

    // Inputs
    private TextInputEditText etNotifTitle;
    private TextInputEditText etNotifMessage;
    private TextView tvCharCountIndicator;

    // Preview
    private TextView tvPreviewChannelBadge;
    private TextView tvPreviewTitle;
    private TextView tvPreviewMessage;
    private TextView tvCostEstimate;

    private MaterialButton btnDispatchNotification;

    private SessionManager sessionManager;
    private UserDao userDao;
    private AppNotificationDao notificationDao;
    private SmsCostingManager smsCostingManager;
    private ApiService apiService;

    private List<User> activeMembers = new ArrayList<>();
    private String selectedType = NotificationCenterHelper.TYPE_NOTICE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_manager);

        sessionManager = new SessionManager(this);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        userDao = new UserDao(dbHelper);
        notificationDao = new AppNotificationDao(dbHelper);
        smsCostingManager = new SmsCostingManager(this);
        apiService = ApiClient.getApiService(this);

        try {
            initViews();
            loadMembers();
            updateBatteryOptCard();
            setupListeners();
            updateCostEstimate();
        } catch (Throwable t) {
            android.util.Log.e("ActivityNotifManager", "Init error: " + t.getMessage(), t);
        }
    }

    private void initViews() {
        btnBackNotifManager = findViewById(R.id.btnBackNotifManager);
        badgeManagerRole = findViewById(R.id.badgeManagerRole);

        cardBatteryOptimization = findViewById(R.id.cardBatteryOptimization);
        ivBatteryStatusIcon = findViewById(R.id.ivBatteryStatusIcon);
        tvBatteryStatusTitle = findViewById(R.id.tvBatteryStatusTitle);
        tvBatteryStatusDesc = findViewById(R.id.tvBatteryStatusDesc);
        btnFixBatteryOpt = findViewById(R.id.btnFixBatteryOpt);

        rgTargetAudience = findViewById(R.id.rgTargetAudience);
        rbTargetBroadcast = findViewById(R.id.rbTargetBroadcast);
        rbTargetSingle = findViewById(R.id.rbTargetSingle);
        layoutSingleMemberSelector = findViewById(R.id.layoutSingleMemberSelector);
        spinnerTargetMember = findViewById(R.id.spinnerTargetMember);

        rgDeliveryChannel = findViewById(R.id.rgDeliveryChannel);
        rbChannelPush = findViewById(R.id.rbChannelPush);
        rbChannelSms = findViewById(R.id.rbChannelSms);
        rbChannelBoth = findViewById(R.id.rbChannelBoth);

        chipGroupTemplates = findViewById(R.id.chipGroupTemplates);

        etNotifTitle = findViewById(R.id.etNotifTitle);
        etNotifMessage = findViewById(R.id.etNotifMessage);
        tvCharCountIndicator = findViewById(R.id.tvCharCountIndicator);

        tvPreviewChannelBadge = findViewById(R.id.tvPreviewChannelBadge);
        tvPreviewTitle = findViewById(R.id.tvPreviewTitle);
        tvPreviewMessage = findViewById(R.id.tvPreviewMessage);
        tvCostEstimate = findViewById(R.id.tvCostEstimate);

        btnDispatchNotification = findViewById(R.id.btnDispatchNotification);

        if (btnBackNotifManager != null) {
            btnBackNotifManager.setOnClickListener(v -> finish());
        }

        if (badgeManagerRole != null) {
            if (sessionManager.isManager()) {
                badgeManagerRole.setText("ম্যানেজার");
            } else if (sessionManager.isAssistant()) {
                badgeManagerRole.setText("সহকারী");
            } else {
                badgeManagerRole.setText("সদস্য");
            }
        }
    }

    private void updateBatteryOptCard() {
        try {
            boolean isIgnored = BatteryOptimizationHelper.isBatteryOptimizationIgnored(this);
            if (ivBatteryStatusIcon == null || tvBatteryStatusTitle == null || tvBatteryStatusDesc == null || btnFixBatteryOpt == null) return;
            if (isIgnored) {
                ivBatteryStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.credit_green));
                tvBatteryStatusTitle.setText("ব্যাকগ্রাউন্ড পুশ ডেলিভারি: সক্রিয়");
                tvBatteryStatusDesc.setText("অ্যাপ বন্ধ থাকলেও নোটিফিকেশন সাথে সাথে পৌঁছাবে।");
                btnFixBatteryOpt.setText("সক্রিয়");
                btnFixBatteryOpt.setEnabled(false);
            } else {
                ivBatteryStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.warning_amber));
                tvBatteryStatusTitle.setText("ব্যাটারি অপটিমাইজেশন সক্রিয় আছে");
                tvBatteryStatusDesc.setText("অ্যাপ বন্ধ থাকলে নোটিফিকেশন দেরিতে আসতে পারে। ট্যাপ করে অনুমতি দিন।");
                btnFixBatteryOpt.setText("অনুমতি দিন");
                btnFixBatteryOpt.setEnabled(true);
                btnFixBatteryOpt.setOnClickListener(v -> {
                    BatteryOptimizationHelper.showBatteryOptimizationDialog(this, this::updateBatteryOptCard);
                });
            }
        } catch (Throwable t) {
            if (cardBatteryOptimization != null) {
                cardBatteryOptimization.setVisibility(View.GONE);
            }
        }
    }

    private void loadMembers() {
        long messId = sessionManager.getMessId();
        activeMembers = userDao.getActiveMembersByMess(messId);
        if (activeMembers == null || activeMembers.isEmpty()) {
            activeMembers = userDao.getAllMembers(messId);
        }
        if (activeMembers == null) {
            activeMembers = new ArrayList<>();
        }

        List<String> memberLabels = new ArrayList<>();
        for (User u : activeMembers) {
            if (u != null) {
                String name = u.getName() != null ? u.getName() : "সদস্য";
                String phone = u.getPhone() != null ? u.getPhone() : "ফোন নেই";
                memberLabels.add(name + " (" + phone + ")");
            }
        }

        if (memberLabels.isEmpty()) {
            memberLabels.add("কোনো সদস্য পাওয়া যায়নি");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, memberLabels);
        if (spinnerTargetMember != null) {
            spinnerTargetMember.setAdapter(adapter);
        }
    }

    private void setupListeners() {
        rgTargetAudience.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isSingle = (checkedId == R.id.rbTargetSingle);
            layoutSingleMemberSelector.setVisibility(isSingle ? View.VISIBLE : View.GONE);
            updateCostEstimate();
        });

        rgDeliveryChannel.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbChannelBoth) {
                tvPreviewChannelBadge.setText("🚀 PUSH + SMS");
            } else if (checkedId == R.id.rbChannelSms) {
                tvPreviewChannelBadge.setText("💬 SIM SMS");
            } else {
                tvPreviewChannelBadge.setText("🔔 PUSH");
            }
            updateCostEstimate();
        });

        chipGroupTemplates.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipTemplateDue) {
                selectedType = NotificationCenterHelper.TYPE_BUDGET;
                etNotifTitle.setText("Due Payment Reminder");
                etNotifMessage.setText("Dear Member, please clear your outstanding mess dues at your earliest convenience to maintain uninterrupted meal services.");
            } else if (checkedId == R.id.chipTemplateMeal) {
                selectedType = "meal";
                etNotifTitle.setText("Tomorrow's Meal Count Confirmation");
                etNotifMessage.setText("Reminder: Please review and lock your breakfast, lunch, and dinner preferences for tomorrow before 10:00 PM tonight.");
            } else if (checkedId == R.id.chipTemplateBazar) {
                selectedType = "bazar";
                etNotifTitle.setText("Bazar Duty Assignment");
                etNotifMessage.setText("Notice: You have been assigned for tomorrow morning's mess bazaar. Please coordinate with the manager.");
            } else if (checkedId == R.id.chipTemplateAlert) {
                selectedType = "emergency";
                etNotifTitle.setText("Urgent Mess Notice");
                etNotifMessage.setText("Attention all members: Please check the mess bulletin immediately regarding an important update.");
            } else {
                selectedType = NotificationCenterHelper.TYPE_NOTICE;
                etNotifTitle.setText("Mess Notice & Announcement");
                etNotifMessage.setText("Important notice from Mess Management: Please take note of the upcoming schedule and arrangements.");
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String title = etNotifTitle.getText() != null ? etNotifTitle.getText().toString() : "";
                String message = etNotifMessage.getText() != null ? etNotifMessage.getText().toString() : "";

                tvPreviewTitle.setText(title.isEmpty() ? "Notification Title" : title);
                tvPreviewMessage.setText(message.isEmpty() ? "Notice details..." : message);

                int chars = message.length();
                int smsParts = chars <= 160 ? 1 : (int) Math.ceil((double) chars / 153.0);
                tvCharCountIndicator.setText(chars + " characters • " + smsParts + " SMS part" + (smsParts > 1 ? "s" : ""));
                updateCostEstimate();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etNotifTitle.addTextChangedListener(watcher);
        etNotifMessage.addTextChangedListener(watcher);

        btnDispatchNotification.setOnClickListener(v -> dispatchNotification());
    }

    private void updateCostEstimate() {
        boolean isSms = (rgDeliveryChannel.getCheckedRadioButtonId() == R.id.rbChannelSms ||
                         rgDeliveryChannel.getCheckedRadioButtonId() == R.id.rbChannelBoth);
        if (!isSms) {
            tvCostEstimate.setText("Estimated Cost: ৳0.00 (Online Push Notification is 100% Free)");
            tvCostEstimate.setTextColor(ContextCompat.getColor(this, R.color.credit_green));
        } else {
            boolean isSingle = (rgTargetAudience.getCheckedRadioButtonId() == R.id.rbTargetSingle);
            int count = isSingle ? 1 : Math.max(1, activeMembers.size());
            double totalCost = count * 0.50;
            tvCostEstimate.setText(String.format(java.util.Locale.US, "Estimated SMS Cost: ৳%.2f (%d member%s @ ৳0.50/SMS debited to ledger)",
                    totalCost, count, count > 1 ? "s" : ""));
            tvCostEstimate.setTextColor(ContextCompat.getColor(this, R.color.warning_amber));
        }
    }

    private void dispatchNotification() {
        try {
            String title = etNotifTitle.getText() != null ? etNotifTitle.getText().toString().trim() : "";
            String message = etNotifMessage.getText() != null ? etNotifMessage.getText().toString().trim() : "";

            if (title.isEmpty()) {
                etNotifTitle.setError("Title is required");
                etNotifTitle.requestFocus();
                return;
            }

            if (message.isEmpty()) {
                etNotifMessage.setError("Message is required");
                etNotifMessage.requestFocus();
                return;
            }

            boolean isSingle = (rgTargetAudience.getCheckedRadioButtonId() == R.id.rbTargetSingle);
            User targetUser = null;
            if (isSingle) {
                if (activeMembers == null || activeMembers.isEmpty()) {
                    Toast.makeText(this, "মেসে পাঠানোর মতো কোনো সক্রিয় সদস্য নেই।", Toast.LENGTH_SHORT).show();
                    return;
                }
                int selectedIndex = spinnerTargetMember.getSelectedItemPosition();
                if (selectedIndex >= 0 && selectedIndex < activeMembers.size()) {
                    targetUser = activeMembers.get(selectedIndex);
                } else {
                    Toast.makeText(this, "দয়া করে একজন প্রাপক সদস্য নির্বাচন করুন।", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            String channel = "push";
            if (rgDeliveryChannel.getCheckedRadioButtonId() == R.id.rbChannelSms) {
                channel = "sms";
            } else if (rgDeliveryChannel.getCheckedRadioButtonId() == R.id.rbChannelBoth) {
                channel = "both";
            }

            boolean isSmsRequested = "sms".equalsIgnoreCase(channel) || "both".equalsIgnoreCase(channel);
            if (isSmsRequested && !com.smartmess.android.utils.PermissionHelper.hasSmsPermission(this)) {
                com.smartmess.android.utils.PermissionHelper.requestSmsPermission(this);
                Toast.makeText(this, "এসএমএস প্রেরণের জন্য অনুগ্রহ করে অনুমতি প্রদান করুন।", Toast.LENGTH_LONG).show();
                return;
            }

            btnDispatchNotification.setEnabled(false);
            btnDispatchNotification.setText("Dispatching...");

            final User finalTargetUser = targetUser;
            final String finalChannel = channel;
            final String finalTitle = title;
            final String finalMessage = message;

            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                int smsSent = 0;
                // 1. Dispatch SMS if channel is sms or both
                if (isSmsRequested) {
                    try {
                        User sender = userDao.getById(sessionManager.getUserId());
                        if (sender == null) {
                            sender = new User();
                            sender.setId(sessionManager.getUserId());
                            sender.setMessId(sessionManager.getMessId());
                            sender.setName(sessionManager.getUserName());
                            sender.setPhone(sessionManager.getUserPhone());
                        }

                        if (finalTargetUser != null) {
                            boolean sent = smsCostingManager.sendSingleDueReminder(sender, finalTargetUser, finalMessage);
                            if (sent) smsSent = 1;
                        } else {
                            smsSent = smsCostingManager.broadcastNoticeToAllMembers(sender, finalMessage);
                        }
                    } catch (Throwable smsEx) {
                        android.util.Log.e("NotifManager", "SMS dispatch error: " + smsEx.getMessage(), smsEx);
                    }
                }

                final int finalSmsSent = smsSent;

                // Always save locally in SQLite
                saveLocalNotification(finalTitle, finalMessage, selectedType, finalChannel, finalTargetUser);

                // 2. Dispatch Online Push Notification if channel is push or both
                if ("push".equalsIgnoreCase(finalChannel) || "both".equalsIgnoreCase(finalChannel)) {
                    Long targetUserId = (finalTargetUser != null) ? finalTargetUser.getId() : null;
                    NotificationSendRequest request = new NotificationSendRequest(
                            finalTitle, finalMessage, selectedType, finalChannel, targetUserId
                    );

                    if (apiService == null) {
                        apiService = ApiClient.getApiService(ActivityNotificationManager.this);
                    }

                    apiService.sendNotification(request).enqueue(new Callback<NotificationSendResponse>() {
                        @Override
                        public void onResponse(Call<NotificationSendResponse> call, Response<NotificationSendResponse> response) {
                            int targetCount = finalTargetUser != null ? 1 : (activeMembers != null ? activeMembers.size() : 1);
                            boolean isSuccess = response.isSuccessful() && response.body() != null;
                            showSuccessDialog(finalChannel, targetCount, finalSmsSent, isSuccess);
                        }

                        @Override
                        public void onFailure(Call<NotificationSendResponse> call, Throwable t) {
                            int targetCount = finalTargetUser != null ? 1 : (activeMembers != null ? activeMembers.size() : 1);
                            showSuccessDialog(finalChannel, targetCount, finalSmsSent, false);
                        }
                    });
                } else {
                    // Only SMS was requested
                    int targetCount = finalTargetUser != null ? 1 : (activeMembers != null ? activeMembers.size() : 1);
                    showSuccessDialog(finalChannel, targetCount, finalSmsSent, true);
                }
            });
        } catch (Throwable t) {
            android.util.Log.e("NotifManager", "Fatal crash prevented in dispatchNotification: " + t.getMessage(), t);
            btnDispatchNotification.setEnabled(true);
            btnDispatchNotification.setText("🚀 Dispatch Notification");
            Toast.makeText(this, "নোটিফিকেশন পাঠানো ব্যর্থ হয়েছে: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveLocalNotification(String title, String message, String type, String channel, User targetUser) {
        try {
            long messId = sessionManager.getMessId();
            long targetId = targetUser != null ? targetUser.getId() : 0;
            AppNotification notif = new AppNotification(
                    UUID.randomUUID().toString(),
                    messId,
                    targetId,
                    title,
                    message,
                    type,
                    channel,
                    DateTimeUtils.nowIso()
            );
            notificationDao.insert(notif);
        } catch (Throwable t) {
            android.util.Log.e("NotifManager", "Error saving local notification: " + t.getMessage(), t);
        }
    }

    private void showSuccessDialog(String channel, int totalTargets, int smsSent, boolean isCloudSuccess) {
        runOnUiThread(() -> {
            if (isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && isDestroyed())) {
                return;
            }

            btnDispatchNotification.setEnabled(true);
            btnDispatchNotification.setText("🚀 Dispatch Notification");

            String channelSummary = "both".equalsIgnoreCase(channel) ? "Online Push & SIM SMS" :
                    ("sms".equalsIgnoreCase(channel) ? "SIM SMS" : "Online Push Notification");

            String msg = "Your notification has been dispatched successfully!\n\n" +
                    "• Channel: " + channelSummary + "\n" +
                    "• Target Recipients: " + totalTargets + " member(s)\n";

            if ("sms".equalsIgnoreCase(channel) || "both".equalsIgnoreCase(channel)) {
                msg += "• SMS Dispatched: " + smsSent + " delivered & ledger updated.\n";
            }
            if ("push".equalsIgnoreCase(channel) || "both".equalsIgnoreCase(channel)) {
                if (isCloudSuccess) {
                    msg += "• Push Notification: Broadcast to active member devices.\n";
                } else {
                    msg += "• Push Notification: Saved locally. (Cloud sync pending or server offline).\n";
                }
            }

            try {
                new AlertDialog.Builder(this)
                        .setTitle("✅ নোটিফিকেশন পাঠানো সম্পন্ন")
                        .setMessage(msg)
                        .setPositiveButton("সম্পন্ন", (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            } catch (Throwable t) {
                Toast.makeText(this, "সফলভাবে পাঠানো হয়েছে!", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void showSuccessDialog(String channel, int totalTargets, int smsSent) {
        showSuccessDialog(channel, totalTargets, smsSent, true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == com.smartmess.android.utils.PermissionHelper.REQ_SMS) {
            if (com.smartmess.android.utils.PermissionHelper.hasSmsPermission(this)) {
                Toast.makeText(this, "এসএমএস অনুমতি প্রাপ্ত হয়েছে! পুনরায় Dispatch চাপুন।", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "এসএমএস অনুমতি প্রত্যাখ্যাত হয়েছে। শুধুমাত্র অনলাইন পুশ পাঠানো যাবে।", Toast.LENGTH_LONG).show();
            }
        }
    }
}
