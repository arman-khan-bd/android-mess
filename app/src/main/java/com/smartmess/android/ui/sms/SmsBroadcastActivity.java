package com.smartmess.android.ui.sms;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.engine.PlanGateManager;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.User;
import com.smartmess.android.sms.SmsCostingManager;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.PermissionHelper;
import com.smartmess.android.utils.SessionManager;

public class SmsBroadcastActivity extends AppCompatActivity {

    public static final String EXTRA_TEMPLATE_TYPE = "EXTRA_TEMPLATE_TYPE";

    private TextView tvActiveMemberCount;
    private TextView tvTotalBroadcastCost;
    private EditText etBroadcastMessage;
    private MaterialButton btnDispatchBroadcast;

    private TextView tvBroadcastSimLabel;
    private MaterialButton btnBroadcastSelectSim;
    private List<com.smartmess.android.sms.SmsDispatcher.SimCardInfo> availableSims = new java.util.ArrayList<>();
    private int selectedSubscriptionId = -1;

    private MaterialButton btnTplSmsMeeting;
    private MaterialButton btnTplSmsMoney;
    private MaterialButton btnTplSmsDue;
    private MaterialButton btnTplSmsMeal;
    private MaterialButton btnTplSmsBazar;
    private MaterialButton btnTplSmsNotice;

    private UserDao userDao;
    private MessDao messDao;
    private SessionManager sessionManager;
    private SmsCostingManager smsCostingManager;
    private PlanGateManager planGateManager;
    private List<User> activeMembers;
    private double perSmsCost = 0.50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_broadcast);

        tvActiveMemberCount = findViewById(R.id.tvActiveMemberCount);
        tvTotalBroadcastCost = findViewById(R.id.tvTotalBroadcastCost);
        etBroadcastMessage = findViewById(R.id.etBroadcastMessage);
        btnDispatchBroadcast = findViewById(R.id.btnDispatchBroadcast);
        tvBroadcastSimLabel = findViewById(R.id.tvBroadcastSimLabel);
        btnBroadcastSelectSim = findViewById(R.id.btnBroadcastSelectSim);

        btnTplSmsMeeting = findViewById(R.id.btnTplSmsMeeting);
        btnTplSmsMoney = findViewById(R.id.btnTplSmsMoney);
        btnTplSmsDue = findViewById(R.id.btnTplSmsDue);
        btnTplSmsMeal = findViewById(R.id.btnTplSmsMeal);
        btnTplSmsBazar = findViewById(R.id.btnTplSmsBazar);
        btnTplSmsNotice = findViewById(R.id.btnTplSmsNotice);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        userDao = new UserDao(helper);
        messDao = new MessDao(helper);
        sessionManager = new SessionManager(this);
        smsCostingManager = new SmsCostingManager(this);
        planGateManager = new PlanGateManager(this);

        loadBroadcastInfo();
        loadSims();
        setupTemplateButtons();

        // Check if opened with a pre-requested template
        String incomingTemplate = getIntent().getStringExtra(EXTRA_TEMPLATE_TYPE);
        if (incomingTemplate != null && !incomingTemplate.isEmpty()) {
            applyTemplate(incomingTemplate);
        }

        if (btnBroadcastSelectSim != null) {
            btnBroadcastSelectSim.setOnClickListener(v -> showSimSelectorDialog());
        }
        btnDispatchBroadcast.setOnClickListener(v -> dispatchBroadcast());
    }

    private void setupTemplateButtons() {
        if (btnTplSmsMeeting != null) btnTplSmsMeeting.setOnClickListener(v -> applyTemplate("meeting"));
        if (btnTplSmsMoney != null) btnTplSmsMoney.setOnClickListener(v -> applyTemplate("money_request"));
        if (btnTplSmsDue != null) btnTplSmsDue.setOnClickListener(v -> applyTemplate("due"));
        if (btnTplSmsMeal != null) btnTplSmsMeal.setOnClickListener(v -> applyTemplate("meal"));
        if (btnTplSmsBazar != null) btnTplSmsBazar.setOnClickListener(v -> applyTemplate("bazar"));
        if (btnTplSmsNotice != null) btnTplSmsNotice.setOnClickListener(v -> applyTemplate("notice"));
    }

    private void applyTemplate(String type) {
        String messName = sessionManager.getMessName();
        if (messName == null || messName.isEmpty()) messName = "SmartMess";

        String text;
        if ("meeting".equalsIgnoreCase(type)) {
            text = "মেস মিটিং নোটিশ: আগামী [তারিখ/সময়] মেসের সকল সদস্যের উপস্থিতিতে জরুরি সাধারণ সভা অনুষ্ঠিত হবে। সবার উপস্থিতি বাধ্যতামূলক। - " + messName;
        } else if ("money_request".equalsIgnoreCase(type)) {
            text = "মানি রিকোয়েস্ট: মেসের চলতি মাসের মিল ও বাজার পরিচালনার জন্য নির্ধারিত অগ্রিম টাকা দ্রুত জমা দেওয়ার অনুরোধ করা হচ্ছে। - " + messName;
        } else if ("due".equalsIgnoreCase(type)) {
            text = "বকেয়া তাগাদা: মেসের চলতি হিসাব অনুযায়ী আপনার বকেয়া টাকা দ্রুত পরিশোধের জন্য অনুরোধ জানানো হচ্ছে। বিস্তারিত দেখতে অ্যাপ চেক করুন। - " + messName;
        } else if ("meal".equalsIgnoreCase(type)) {
            text = "মিল বন্ধ/লক নোটিশ: প্রতিদিন রাত ১০টার মধ্যে পরের দিনের মিল অন/অফ নিশ্চিত করুন। নির্ধারিত সময়ের পর পরিবর্তন গ্রহণযোগ্য নয়। - " + messName;
        } else if ("bazar".equalsIgnoreCase(type)) {
            text = "বাজার ডিউটি নোটিশ: মেসের শিডিউল অনুযায়ী আগামীকালের বাজার করার দায়িত্ব যাদের রয়েছে তারা যথাসময়ে প্রস্তুতি নিন। - " + messName;
        } else {
            text = "জরুরি মেস নোটিশ: মেসের নিয়ম ও পরিচ্ছন্নতা বজায় রাখতে সকল সদস্যের আন্তরিক সহযোগিতা কামনা করা হচ্ছে। - " + messName;
        }

        if (etBroadcastMessage != null) {
            etBroadcastMessage.setText(text);
            etBroadcastMessage.setSelection(text.length());
            etBroadcastMessage.requestFocus();
            Toast.makeText(this, "টেমপ্লেট লোড করা হয়েছে", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSims() {
        try {
            availableSims = com.smartmess.android.sms.SmsDispatcher.getActiveSimCards(this);
            int defaultSlot = sessionManager.getDefaultSimSlot();
            selectedSubscriptionId = -1;

            String label = "সিম: সিস্টেম ডিফল্ট";
            if (availableSims != null && !availableSims.isEmpty()) {
                if (defaultSlot >= 0) {
                    for (com.smartmess.android.sms.SmsDispatcher.SimCardInfo s : availableSims) {
                        if (s.slotIndex == defaultSlot) {
                            selectedSubscriptionId = s.subscriptionId;
                            label = s.getDisplayLabel();
                            break;
                        }
                    }
                }
                if (selectedSubscriptionId == -1 && !availableSims.isEmpty()) {
                    com.smartmess.android.sms.SmsDispatcher.SimCardInfo first = availableSims.get(0);
                    selectedSubscriptionId = first.subscriptionId;
                    label = first.getDisplayLabel();
                }
            }
            if (tvBroadcastSimLabel != null) {
                tvBroadcastSimLabel.setText(label);
            }
        } catch (Throwable t) {
            android.util.Log.e("SmsBroadcastActivity", "Error loading SIMs: " + t.getMessage());
        }
    }

    private void showSimSelectorDialog() {
        try {
            List<String> options = new java.util.ArrayList<>();
            final List<Integer> subIds = new java.util.ArrayList<>();

            options.add("সিস্টেম ডিফল্ট (স্বয়ংক্রিয়)");
            subIds.add(-1);

            int checkedPos = 0;
            if (availableSims != null && !availableSims.isEmpty()) {
                for (int i = 0; i < availableSims.size(); i++) {
                    com.smartmess.android.sms.SmsDispatcher.SimCardInfo sim = availableSims.get(i);
                    options.add(sim.getDisplayLabel());
                    subIds.add(sim.subscriptionId);
                    if (sim.subscriptionId == selectedSubscriptionId) {
                        checkedPos = i + 1;
                    }
                }
            } else {
                options.add("সিম ১ (স্লট ১)");
                subIds.add(0);
                options.add("সিম ২ (স্লট ২)");
                subIds.add(1);
            }

            String[] items = options.toArray(new String[0]);
            new android.app.AlertDialog.Builder(this)
                    .setTitle("এসএমএস পাঠানোর সিম নির্বাচন করুন")
                    .setSingleChoiceItems(items, checkedPos, (dialog, which) -> {
                        selectedSubscriptionId = subIds.get(which);
                        if (tvBroadcastSimLabel != null) {
                            tvBroadcastSimLabel.setText(items[which]);
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
        } catch (Throwable t) {
            Toast.makeText(this, "সিম তালিকা দেখানো যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadBroadcastInfo() {
        long messId = sessionManager.getMessId();
        Mess mess = messDao.getById(messId);
        perSmsCost = mess != null ? mess.getPerSmsCost() : 0.50;

        activeMembers = userDao.getActiveMembersByMess(messId);
        int count = activeMembers.size();
        double totalCost = count * perSmsCost;

        tvActiveMemberCount.setText("Target: " + count + " Active Members");
        tvTotalBroadcastCost.setText("Total SMS Cost: " + CurrencyUtils.format(totalCost) + " (Split equally among all members)");

        etBroadcastMessage.setText("Mess Notice: Please review your meal and bazar status on the SmartMess app. - " + sessionManager.getMessName());
    }

    private void dispatchBroadcast() {
        if (!planGateManager.canUseBulkSms()) {
            planGateManager.showUpgradeBottomSheet(getSupportFragmentManager(), "Bulk SMS Dispatcher",
                    "Bulk due reminders and automated ledger debits are a Pro subscription feature.");
            return;
        }

        if (!PermissionHelper.hasSmsPermission(this)) {
            PermissionHelper.requestSmsPermission(this);
            return;
        }

        String msg = etBroadcastMessage.getText().toString().trim();
        if (msg.isEmpty()) {
            Toast.makeText(this, "বার্তা খালি রাখা যাবে না", Toast.LENGTH_SHORT).show();
            return;
        }

        User sender = userDao.getById(sessionManager.getUserId());
        if (sender == null) {
            sender = new User("sender", sessionManager.getMessId(), sessionManager.getUserName(), sessionManager.getUserPhone(), sessionManager.getUserRole(), "active");
            sender.setId(sessionManager.getUserId());
        }

        int sent = smsCostingManager.broadcastNoticeToAllMembers(sender, msg, selectedSubscriptionId);
        if (sent > 0) {
            Toast.makeText(this, "ব্রডকাস্ট এসএমএস পাঠানো হয়েছে: " + sent + " members! Total cost split equally across mess members.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "ব্রডকাস্ট পাঠানো যায়নি। সিম নেটওয়ার্ক চেক করুন।", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQ_SMS && PermissionHelper.hasSmsPermission(this)) {
            dispatchBroadcast();
        }
    }
}
