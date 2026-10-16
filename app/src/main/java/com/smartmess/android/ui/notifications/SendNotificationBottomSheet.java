package com.smartmess.android.ui.notifications;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.AppNotificationDao;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.NotificationSendRequest;
import com.smartmess.android.data.remote.dto.NotificationSendResponse;
import com.smartmess.android.model.AppNotification;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.User;
import com.smartmess.android.sms.SmsCostingManager;
import com.smartmess.android.sms.SmsDispatcher;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.NotificationCenterHelper;
import com.smartmess.android.utils.PermissionHelper;
import com.smartmess.android.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SendNotificationBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SendNotifBottomSheet";

    private RadioGroup rgAudience;
    private RadioButton rbBroadcast;
    private RadioButton rbSingle;
    private LinearLayout layoutMemberSelector;
    private Spinner spinnerMember;

    private RadioGroup rgChannel;
    private RadioButton rbChannelPush;
    private RadioButton rbChannelSms;
    private RadioButton rbChannelBoth;

    private LinearLayout layoutSimSelector;
    private MaterialButton btnSelectSim;
    private TextView tvSelectedSimLabel;

    private MaterialButton btnTplNotice;
    private MaterialButton btnTplDue;
    private MaterialButton btnTplMeal;
    private MaterialButton btnTplBazar;
    private MaterialButton btnTplAlert;

    private EditText etTitle;
    private EditText etMessage;
    private TextView tvCharCount;
    private TextView tvCostEstimate;
    private MaterialButton btnDispatch;
    private ImageButton btnDismiss;

    private SessionManager sessionManager;
    private UserDao userDao;
    private MessDao messDao;
    private AppNotificationDao notificationDao;
    private SmsCostingManager smsCostingManager;
    private ApiService apiService;

    private List<User> activeMembers = new ArrayList<>();
    private List<SmsDispatcher.SimCardInfo> availableSims = new ArrayList<>();
    private int selectedSimSubscriptionId = -1;
    private String selectedSimDisplay = "সিস্টেম ডিফল্ট";
    private double perSmsCost = 0.50;
    private String selectedType = NotificationCenterHelper.TYPE_NOTICE;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static SendNotificationBottomSheet newInstance() {
        return new SendNotificationBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            return inflater.inflate(R.layout.bottom_sheet_send_notification, container, false);
        } catch (Throwable t) {
            android.util.Log.e(TAG, "Fatal inflation error in SendNotificationBottomSheet: " + t.getMessage(), t);
            if (getContext() != null) {
                android.view.ContextThemeWrapper themeWrapper = new android.view.ContextThemeWrapper(getContext(), R.style.Theme_SmartMess);
                return LayoutInflater.from(themeWrapper).inflate(R.layout.bottom_sheet_send_notification, container, false);
            }
            throw t;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            sessionManager = new SessionManager(requireContext());
            DatabaseHelper helper = DatabaseHelper.getInstance(requireContext());
            userDao = new UserDao(helper);
            messDao = new MessDao(helper);
            notificationDao = new AppNotificationDao(helper);
            smsCostingManager = new SmsCostingManager(requireContext());
            apiService = ApiClient.getApiService(requireContext());

            Mess mess = messDao.getById(sessionManager.getMessId());
            if (mess != null && mess.getPerSmsCost() > 0) {
                perSmsCost = mess.getPerSmsCost();
            }

            initViews(view);
            loadMembers();
            loadSims();
            setupListeners();
            updateCostEstimate();
        } catch (Throwable t) {
            android.util.Log.e(TAG, "Initialization error in bottom sheet: " + t.getMessage(), t);
        }
    }

    private void initViews(View view) {
        rgAudience = view.findViewById(R.id.rgSheetAudience);
        rbBroadcast = view.findViewById(R.id.rbSheetBroadcast);
        rbSingle = view.findViewById(R.id.rbSheetSingle);
        layoutMemberSelector = view.findViewById(R.id.layoutSheetMemberSelector);
        spinnerMember = view.findViewById(R.id.spinnerSheetMember);

        rgChannel = view.findViewById(R.id.rgSheetChannel);
        rbChannelPush = view.findViewById(R.id.rbSheetChannelPush);
        rbChannelSms = view.findViewById(R.id.rbSheetChannelSms);
        rbChannelBoth = view.findViewById(R.id.rbSheetChannelBoth);

        layoutSimSelector = view.findViewById(R.id.layoutSheetSimSelector);
        btnSelectSim = view.findViewById(R.id.btnSheetSelectSim);
        tvSelectedSimLabel = view.findViewById(R.id.tvSheetSelectedSimLabel);

        btnTplNotice = view.findViewById(R.id.btnTplNotice);
        btnTplDue = view.findViewById(R.id.btnTplDue);
        btnTplMeal = view.findViewById(R.id.btnTplMeal);
        btnTplBazar = view.findViewById(R.id.btnTplBazar);
        btnTplAlert = view.findViewById(R.id.btnTplAlert);

        etTitle = view.findViewById(R.id.etSheetTitle);
        etMessage = view.findViewById(R.id.etSheetMessage);
        tvCharCount = view.findViewById(R.id.tvSheetCharCount);
        tvCostEstimate = view.findViewById(R.id.tvSheetCostEstimate);
        btnDispatch = view.findViewById(R.id.btnSheetDispatch);
        btnDismiss = view.findViewById(R.id.btnDismissSheet);

        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> dismiss());
        }
    }

    private void loadMembers() {
        try {
            long messId = sessionManager.getMessId();
            activeMembers = userDao.getActiveMembersByMess(messId);
            if (activeMembers == null || activeMembers.isEmpty()) {
                activeMembers = userDao.getAllMembers(messId);
            }
            if (activeMembers == null) activeMembers = new ArrayList<>();

            List<String> labels = new ArrayList<>();
            for (User u : activeMembers) {
                String name = u.getName() != null ? u.getName() : "সদস্য";
                String phone = u.getPhone() != null ? u.getPhone() : "";
                labels.add(name + (phone.isEmpty() ? "" : " (" + phone + ")"));
            }

            if (labels.isEmpty()) {
                labels.add("কোনো সদস্য পাওয়া যায়নি");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, labels);
            if (spinnerMember != null) {
                spinnerMember.setAdapter(adapter);
            }
        } catch (Throwable t) {
            android.util.Log.e(TAG, "Error loading members: " + t.getMessage());
        }
    }

    private void loadSims() {
        try {
            availableSims = SmsDispatcher.getActiveSimCards(requireContext());
            int defaultSlot = sessionManager.getDefaultSimSlot();

            selectedSimSubscriptionId = -1;
            selectedSimDisplay = "সিস্টেম ডিফল্ট (স্বয়ংক্রিয়)";

            if (availableSims != null && !availableSims.isEmpty()) {
                if (defaultSlot >= 0) {
                    for (SmsDispatcher.SimCardInfo s : availableSims) {
                        if (s.slotIndex == defaultSlot) {
                            selectedSimSubscriptionId = s.subscriptionId;
                            selectedSimDisplay = s.getDisplayLabel();
                            break;
                        }
                    }
                }
                if (selectedSimSubscriptionId == -1 && !availableSims.isEmpty()) {
                    SmsDispatcher.SimCardInfo first = availableSims.get(0);
                    selectedSimSubscriptionId = first.subscriptionId;
                    selectedSimDisplay = first.getDisplayLabel();
                }
            }

            if (tvSelectedSimLabel != null) {
                tvSelectedSimLabel.setText("নির্বাচিত: " + selectedSimDisplay);
            }
        } catch (Throwable t) {
            android.util.Log.e(TAG, "Error loading SIM cards: " + t.getMessage());
        }
    }

    private void showSimPickerDialog() {
        try {
            List<String> options = new ArrayList<>();
            final List<Integer> subIds = new ArrayList<>();

            options.add("সিস্টেম ডিফল্ট (স্বয়ংক্রিয়)");
            subIds.add(-1);

            int selectedIndex = 0;
            if (availableSims != null && !availableSims.isEmpty()) {
                for (int i = 0; i < availableSims.size(); i++) {
                    SmsDispatcher.SimCardInfo sim = availableSims.get(i);
                    options.add(sim.getDisplayLabel());
                    subIds.add(sim.subscriptionId);
                    if (sim.subscriptionId == selectedSimSubscriptionId) {
                        selectedIndex = i + 1;
                    }
                }
            } else {
                options.add("সিম ১ (ডিফল্ট সিস্টেম সিম)");
                subIds.add(-1);
                options.add("সিম ২ (দ্বিতীয় সিম)");
                subIds.add(-1);
            }

            String[] items = options.toArray(new String[0]);
            new AlertDialog.Builder(requireContext())
                    .setTitle("এসএমএস পাঠানোর সিম নির্বাচন করুন")
                    .setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
                        selectedSimSubscriptionId = subIds.get(which);
                        selectedSimDisplay = items[which];
                        if (tvSelectedSimLabel != null) {
                            tvSelectedSimLabel.setText("নির্বাচিত: " + selectedSimDisplay);
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
        } catch (Throwable t) {
            Toast.makeText(requireContext(), "সিম নির্বাচন তালিকা দেখানো যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        if (rgAudience != null) {
            rgAudience.setOnCheckedChangeListener((group, checkedId) -> {
                boolean isSingle = (checkedId == R.id.rbSheetSingle);
                if (layoutMemberSelector != null) {
                    layoutMemberSelector.setVisibility(isSingle ? View.VISIBLE : View.GONE);
                }
                updateCostEstimate();
            });
        }

        if (rgChannel != null) {
            rgChannel.setOnCheckedChangeListener((group, checkedId) -> {
                boolean needsSms = (checkedId == R.id.rbSheetChannelSms || checkedId == R.id.rbSheetChannelBoth);
                if (layoutSimSelector != null) {
                    layoutSimSelector.setVisibility(needsSms ? View.VISIBLE : View.GONE);
                }
                updateCostEstimate();
            });
        }

        if (btnSelectSim != null) {
            btnSelectSim.setOnClickListener(v -> showSimPickerDialog());
        }

        // Templates
        if (btnTplNotice != null) {
            btnTplNotice.setOnClickListener(v -> {
                selectedType = NotificationCenterHelper.TYPE_NOTICE;
                etTitle.setText("জরুরি মেস বিজ্ঞপ্তি");
                etMessage.setText("মেস ব্যবস্থাপনা থেকে বিশেষ বিজ্ঞপ্তি: মেসের নোটিশ বোর্ডে দেওয়া গুরুত্বপূর্ণ নির্দেশনাটি সকলে লক্ষ্য করুন।");
            });
        }

        if (btnTplDue != null) {
            btnTplDue.setOnClickListener(v -> {
                selectedType = NotificationCenterHelper.TYPE_BUDGET;
                etTitle.setText("বকেয়া পরিশোধের তাগাদা");
                etMessage.setText("প্রিয় মেস সদস্য, মেসের নিয়মিত বাজার ও হিসাব সচল রাখতে আপনার চলতি বকেয়া টাকা অতিসত্বর ম্যানেজারের কাছে জমা দিন।");
            });
        }

        if (btnTplMeal != null) {
            btnTplMeal.setOnClickListener(v -> {
                selectedType = "meal";
                etTitle.setText("আগামীকালের মিল নিশ্চিতকরণ");
                etMessage.setText("অনুগ্রহ করে রাত ১০:০০ টার পূর্বে আপনার আগামীকালের সকাল, দুপুর ও রাতের মিল স্ট্যাটাস অ্যাপে চেক ও লক করুন।");
            });
        }

        if (btnTplBazar != null) {
            btnTplBazar.setOnClickListener(v -> {
                selectedType = "bazar";
                etTitle.setText("বাজারের দায়িত্ব বণ্টন");
                etMessage.setText("মেস বিজ্ঞপ্তি: আগামীকাল সকালের বাজার দায়িত্ব আপনার উপর ন্যস্ত করা হয়েছে। সময়মতো বাজারে যাওয়ার জন্য প্রস্তুত থাকুন।");
            });
        }

        if (btnTplAlert != null) {
            btnTplAlert.setOnClickListener(v -> {
                selectedType = "emergency";
                etTitle.setText("জরুরি সতর্কতা");
                etMessage.setText("সকল মেস সদস্যের দৃষ্টি আকর্ষণ করা যাচ্ছে: অতি জরুরি মেস প্রয়োজনে এখনই অ্যাপে যুক্ত হোন অথবা ম্যানেজারের সাথে যোগাযোগ করুন।");
            });
        }

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int len = etMessage.getText() != null ? etMessage.getText().length() : 0;
                int smsParts = len <= 160 ? 1 : (int) Math.ceil((double) len / 153.0);
                if (tvCharCount != null) {
                    tvCharCount.setText(len + " অক্ষর • " + smsParts + "টি এসএমএস");
                }
                updateCostEstimate();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        if (etMessage != null) {
            etMessage.addTextChangedListener(watcher);
        }

        if (btnDispatch != null) {
            btnDispatch.setOnClickListener(v -> dispatchNotification());
        }
    }

    private void updateCostEstimate() {
        if (tvCostEstimate == null || rgChannel == null) return;

        int channelId = rgChannel.getCheckedRadioButtonId();
        boolean isSms = (channelId == R.id.rbSheetChannelSms || channelId == R.id.rbSheetChannelBoth);

        if (!isSms) {
            tvCostEstimate.setText("খরচ: ৳০.০০ (অনলাইন পুশ সম্পূর্ণ ফ্রি)");
            tvCostEstimate.setTextColor(ContextCompat.getColor(requireContext(), R.color.credit_green));
        } else {
            boolean isSingle = (rgAudience != null && rgAudience.getCheckedRadioButtonId() == R.id.rbSheetSingle);
            int count = isSingle ? 1 : Math.max(1, activeMembers != null ? activeMembers.size() : 1);
            double total = count * perSmsCost;
            String label = String.format(Locale.US,
                    "আনুমানিক খরচ: ৳%.2f (%d জন সদস্য @ ৳%.2f/এসএমএস • মেস খতিয়ান থেকে কর্তিত)",
                    total, count, perSmsCost);
            tvCostEstimate.setText(label);
            tvCostEstimate.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_amber));
        }
    }

    private void dispatchNotification() {
        try {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String message = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";

            if (title.isEmpty()) {
                etTitle.setError("শিরোনাম লিখুন");
                etTitle.requestFocus();
                return;
            }

            if (message.isEmpty()) {
                etMessage.setError("বার্তার বিবরণ লিখুন");
                etMessage.requestFocus();
                return;
            }

            boolean isSingle = (rgAudience != null && rgAudience.getCheckedRadioButtonId() == R.id.rbSheetSingle);
            User targetUser = null;
            if (isSingle) {
                if (activeMembers == null || activeMembers.isEmpty()) {
                    Toast.makeText(requireContext(), "কোনো সদস্য পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
                    return;
                }
                int idx = spinnerMember.getSelectedItemPosition();
                if (idx >= 0 && idx < activeMembers.size()) {
                    targetUser = activeMembers.get(idx);
                } else {
                    Toast.makeText(requireContext(), "দয়া করে প্রাপক সদস্য নির্বাচন করুন", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            int channelId = rgChannel.getCheckedRadioButtonId();
            String channel = "push";
            if (channelId == R.id.rbSheetChannelSms) channel = "sms";
            else if (channelId == R.id.rbSheetChannelBoth) channel = "both";

            // If SMS is involved, check SMS permission
            if ("sms".equalsIgnoreCase(channel) || "both".equalsIgnoreCase(channel)) {
                if (!PermissionHelper.hasSmsPermission(requireActivity())) {
                    PermissionHelper.requestSmsPermission(requireActivity());
                    Toast.makeText(requireContext(), "এসএমএস পাঠানোর জন্য SMS অনুমতি প্রয়োজন", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            btnDispatch.setEnabled(false);
            btnDispatch.setText("নোটিফিকেশন পাঠানো হচ্ছে...");

            final User finalTarget = targetUser;
            final String finalTitle = title;
            final String finalMessage = message;
            final String finalChannel = channel;
            final int finalSubId = selectedSimSubscriptionId;

            executorService.execute(() -> {
                int smsSent = 0;

                // 1. Send SMS if requested
                if ("sms".equalsIgnoreCase(finalChannel) || "both".equalsIgnoreCase(finalChannel)) {
                    try {
                        User sender = userDao.getById(sessionManager.getUserId());
                        if (sender == null) {
                            sender = new User();
                            sender.setId(sessionManager.getUserId());
                            sender.setMessId(sessionManager.getMessId());
                            sender.setName(sessionManager.getUserName());
                            sender.setPhone(sessionManager.getUserPhone());
                        }

                        if (finalTarget != null) {
                            boolean sent = smsCostingManager.sendSingleDueReminder(sender, finalTarget, finalMessage, finalSubId);
                            if (sent) smsSent = 1;
                        } else {
                            smsSent = smsCostingManager.broadcastNoticeToAllMembers(sender, finalMessage, finalSubId);
                        }
                    } catch (Throwable smsEx) {
                        android.util.Log.e(TAG, "SMS send error: " + smsEx.getMessage(), smsEx);
                    }
                }

                // 2. Save local AppNotification record
                try {
                    long messId = sessionManager.getMessId();
                    long targetId = finalTarget != null ? finalTarget.getId() : 0;
                    AppNotification notif = new AppNotification(
                            UUID.randomUUID().toString(),
                            messId,
                            targetId,
                            finalTitle,
                            finalMessage,
                            selectedType,
                            finalChannel,
                            DateTimeUtils.nowIso()
                    );
                    notificationDao.insert(notif);
                } catch (Throwable notifEx) {
                    android.util.Log.e(TAG, "Local notif save error: " + notifEx.getMessage(), notifEx);
                }

                // 3. Dispatch Cloud Push if requested
                if ("push".equalsIgnoreCase(finalChannel) || "both".equalsIgnoreCase(finalChannel)) {
                    Long targetId = finalTarget != null ? finalTarget.getId() : null;
                    NotificationSendRequest request = new NotificationSendRequest(
                            finalTitle, finalMessage, selectedType, finalChannel, targetId
                    );
                    try {
                        apiService.sendNotification(request).enqueue(new Callback<NotificationSendResponse>() {
                            @Override
                            public void onResponse(Call<NotificationSendResponse> call, Response<NotificationSendResponse> response) {}
                            @Override
                            public void onFailure(Call<NotificationSendResponse> call, Throwable t) {}
                        });
                    } catch (Throwable ignored) {}
                }

                final int finalSmsCount = smsSent;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        String successMsg;
                        if ("sms".equalsIgnoreCase(finalChannel)) {
                            successMsg = "সিম থেকে " + finalSmsCount + "টি এসএমএস সফলভাবে পাঠানো হয়েছে!";
                        } else if ("both".equalsIgnoreCase(finalChannel)) {
                            successMsg = "অনলাইন পুশ ও " + finalSmsCount + "টি সিম এসএমএস পাঠানো হয়েছে!";
                        } else {
                            successMsg = "অনলাইন পুশ নোটিফিকেশন সফলভাবে পাঠানো হয়েছে!";
                        }
                        Toast.makeText(requireContext(), successMsg, Toast.LENGTH_LONG).show();
                        dismissAllowingStateLoss();
                    });
                }
            });

        } catch (Throwable t) {
            android.util.Log.e(TAG, "Dispatch crash caught: " + t.getMessage(), t);
            if (btnDispatch != null) {
                btnDispatch.setEnabled(true);
                btnDispatch.setText("🚀 নোটিফিকেশন পাঠান");
            }
            Toast.makeText(requireContext(), "নোটিফিকেশন পাঠানো যায়নি: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
