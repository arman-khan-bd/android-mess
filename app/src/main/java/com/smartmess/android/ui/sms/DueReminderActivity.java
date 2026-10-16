package com.smartmess.android.ui.sms;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.engine.AccountingEngine;
import com.smartmess.android.engine.CalculationModels.MemberBalanceSheet;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.User;
import com.smartmess.android.sms.SmsCostingManager;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.PermissionHelper;
import com.smartmess.android.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class DueReminderActivity extends AppCompatActivity {

    private Spinner spReminderMember;
    private TextView tvCurrentDueAmount;
    private TextView tvPerSmsCostInfo;
    private EditText etMessageContent;
    private MaterialButton btnSendSmsNow;

    private TextView tvDueReminderSimLabel;
    private MaterialButton btnDueReminderSelectSim;
    private List<com.smartmess.android.sms.SmsDispatcher.SimCardInfo> availableSims = new ArrayList<>();
    private int selectedSubscriptionId = -1;

    private UserDao userDao;
    private MessDao messDao;
    private SessionManager sessionManager;
    private AccountingEngine accountingEngine;
    private SmsCostingManager smsCostingManager;
    private List<User> activeMembers = new ArrayList<>();
    private long selectedUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_due_reminder);

        spReminderMember = findViewById(R.id.spReminderMember);
        tvCurrentDueAmount = findViewById(R.id.tvCurrentDueAmount);
        tvPerSmsCostInfo = findViewById(R.id.tvPerSmsCostInfo);
        etMessageContent = findViewById(R.id.etMessageContent);
        btnSendSmsNow = findViewById(R.id.btnSendSmsNow);
        tvDueReminderSimLabel = findViewById(R.id.tvDueReminderSimLabel);
        btnDueReminderSelectSim = findViewById(R.id.btnDueReminderSelectSim);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        userDao = new UserDao(helper);
        messDao = new MessDao(helper);
        sessionManager = new SessionManager(this);
        accountingEngine = new AccountingEngine(this);
        smsCostingManager = new SmsCostingManager(this);

        selectedUserId = getIntent().getLongExtra("TARGET_USER_ID", -1);

        loadMembersAndSetup();
        loadSims();

        if (btnDueReminderSelectSim != null) {
            btnDueReminderSelectSim.setOnClickListener(v -> showSimSelectorDialog());
        }
        btnSendSmsNow.setOnClickListener(v -> dispatchDueReminder());
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
            if (tvDueReminderSimLabel != null) {
                tvDueReminderSimLabel.setText(label);
            }
        } catch (Throwable t) {
            android.util.Log.e("DueReminderActivity", "Error loading SIMs: " + t.getMessage());
        }
    }

    private void showSimSelectorDialog() {
        try {
            List<String> options = new ArrayList<>();
            final List<Integer> subIds = new ArrayList<>();

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
                        if (tvDueReminderSimLabel != null) {
                            tvDueReminderSimLabel.setText(items[which]);
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
        } catch (Throwable t) {
            Toast.makeText(this, "সিম তালিকা দেখানো যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMembersAndSetup() {
        long messId = sessionManager.getMessId();
        Mess mess = messDao.getById(messId);
        double perSmsCost = mess != null ? mess.getPerSmsCost() : 0.50;
        tvPerSmsCostInfo.setText("Configured SMS Cost: " + CurrencyUtils.format(perSmsCost) + " (will be debited to recipient)");

        activeMembers = userDao.getActiveMembersByMess(messId);
        List<String> names = new ArrayList<>();
        int defaultPos = 0;
        for (int i = 0; i < activeMembers.size(); i++) {
            User u = activeMembers.get(i);
            names.add(u.getName() + " (" + u.getPhone() + ")");
            if (u.getId() == selectedUserId) {
                defaultPos = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        spReminderMember.setAdapter(adapter);
        spReminderMember.setSelection(defaultPos);

        spReminderMember.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateMemberDueInfo(activeMembers.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (!activeMembers.isEmpty()) {
            updateMemberDueInfo(activeMembers.get(defaultPos));
        }
    }

    private void updateMemberDueInfo(User member) {
        long messId = sessionManager.getMessId();
        Mess mess = messDao.getById(messId);
        int startDay = mess != null ? mess.getCycleStartDay() : 1;
        String start = DateTimeUtils.currentMonthStart(startDay);
        String end = DateTimeUtils.currentMonthEnd(startDay);

        MemberBalanceSheet balance = accountingEngine.calculateSingleMemberBalance(messId, member.getId(), start, end);
        if (balance.isDue()) {
            tvCurrentDueAmount.setText("Current Due Amount: " + CurrencyUtils.format(balance.getDueAmount()));
            tvCurrentDueAmount.setTextColor(getResources().getColor(R.color.due_red));
        } else {
            tvCurrentDueAmount.setText("Current Balance: " + CurrencyUtils.format(balance.getNetBalance()) + " (In Credit)");
            tvCurrentDueAmount.setTextColor(getResources().getColor(R.color.credit_green));
        }

        String template = "Dear " + member.getName() + ", your mess balance is due " + CurrencyUtils.format(balance.getDueAmount())
                + ". Please pay your advance deposit soon. - " + sessionManager.getMessName();
        etMessageContent.setText(template);
    }

    private void dispatchDueReminder() {
        if (!PermissionHelper.hasSmsPermission(this)) {
            PermissionHelper.requestSmsPermission(this);
            return;
        }

        if (activeMembers.isEmpty()) return;

        User targetMember = activeMembers.get(spReminderMember.getSelectedItemPosition());
        String msg = etMessageContent.getText().toString().trim();
        if (msg.isEmpty()) {
            Toast.makeText(this, "বার্তা খালি রাখা যাবে না", Toast.LENGTH_SHORT).show();
            return;
        }

        User sender = userDao.getById(sessionManager.getUserId());
        if (sender == null) {
            sender = new User("sender", sessionManager.getMessId(), sessionManager.getUserName(), sessionManager.getUserPhone(), sessionManager.getUserRole(), "active");
            sender.setId(sessionManager.getUserId());
        }

        boolean sent = smsCostingManager.sendSingleDueReminder(sender, targetMember, msg, selectedSubscriptionId);
        if (sent) {
            Toast.makeText(this, "সিম দিয়ে বকেয়া এসএমএস পাঠানো হয়েছে! খরচ খতিয়ানে যুক্ত হয়েছে: " + targetMember.getName(), Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "সিম দিয়ে এসএমএস পাঠানো যায়নি। নেটওয়ার্ক ও ব্যালেন্স চেক করুন।", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQ_SMS) {
            if (PermissionHelper.hasSmsPermission(this)) {
                dispatchDueReminder();
            } else {
                Toast.makeText(this, "সিম দিয়ে এসএমএস পাঠাতে এসএমএস পারমিশন দেওয়া প্রয়োজন", Toast.LENGTH_LONG).show();
            }
        }
    }
}
