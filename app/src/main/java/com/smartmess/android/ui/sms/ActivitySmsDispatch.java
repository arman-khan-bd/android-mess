package com.smartmess.android.ui.sms;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.sync.SyncManager;
import com.smartmess.android.engine.AccountingEngine;
import com.smartmess.android.engine.CalculationModels.MemberBalanceSheet;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.User;
import com.smartmess.android.sms.SmsCostingManager;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.PermissionHelper;
import com.smartmess.android.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivitySmsDispatch extends AppCompatActivity {

    private CheckBox cbSelectAll;
    private MaterialButton btnSelectSim;
    private TextView tvCostPreviewBanner;
    private RecyclerView rvOverdueMembers;
    private MaterialButton btnDispatchSms;

    private SessionManager sessionManager;
    private UserDao userDao;
    private MessDao messDao;
    private AccountingEngine accountingEngine;
    private SmsCostingManager smsCostingManager;

    private OverdueMemberAdapter adapter;
    private final List<OverdueMemberItem> overdueList = new ArrayList<>();
    private final Set<Long> selectedMemberIds = new HashSet<>();

    private double perSmsCost = 0.50;
    private int selectedSimSlot = -1; // -1 for auto, 0 for SIM 1, 1 for SIM 2
    private int selectedSubscriptionId = -1;
    private List<com.smartmess.android.sms.SmsDispatcher.SimCardInfo> availableSims = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_dispatch);

        sessionManager = new SessionManager(this);
        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        userDao = new UserDao(helper);
        messDao = new MessDao(helper);
        accountingEngine = new AccountingEngine(this);
        smsCostingManager = new SmsCostingManager(this);

        Mess mess = messDao.getById(sessionManager.getMessId());
        if (mess != null && mess.getPerSmsCost() > 0) {
            perSmsCost = mess.getPerSmsCost();
        }

        initViews();
        loadSims();
        loadOverdueMembers();
    }

    private void loadSims() {
        try {
            availableSims = com.smartmess.android.sms.SmsDispatcher.getActiveSimCards(this);
            int defaultSlot = sessionManager.getDefaultSimSlot();
            selectedSimSlot = defaultSlot;
            selectedSubscriptionId = -1;

            String label = "সিস্টেম ডিফল্ট";
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
            if (btnSelectSim != null) {
                btnSelectSim.setText(label);
            }
        } catch (Throwable t) {
            android.util.Log.e("ActivitySmsDispatch", "Error loading SIMs: " + t.getMessage());
        }
    }

    private void initViews() {
        cbSelectAll = findViewById(R.id.cbSelectAll);
        btnSelectSim = findViewById(R.id.btnSelectSim);
        tvCostPreviewBanner = findViewById(R.id.tvCostPreviewBanner);
        rvOverdueMembers = findViewById(R.id.rvOverdueMembers);
        btnDispatchSms = findViewById(R.id.btnDispatchSms);

        rvOverdueMembers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OverdueMemberAdapter();
        rvOverdueMembers.setAdapter(adapter);

        cbSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectedMemberIds.clear();
            if (isChecked) {
                for (OverdueMemberItem item : overdueList) {
                    selectedMemberIds.add(item.user.getId());
                }
            }
            adapter.notifyDataSetChanged();
            updatePreviewBanner();
        });

        btnSelectSim.setOnClickListener(v -> showSimSelectorDialog());
        btnDispatchSms.setOnClickListener(v -> handleSmsDispatch());
    }

    private void loadOverdueMembers() {
        long messId = sessionManager.getMessId();
        List<User> allMembers = userDao.getAllMembers(messId);
        overdueList.clear();
        selectedMemberIds.clear();

        for (User u : allMembers) {
            MemberBalanceSheet sheet = accountingEngine.calculateMemberBalance(messId, u.getId());
            if (sheet.isOverdue()) {
                overdueList.add(new OverdueMemberItem(u, sheet.getNetBalance()));
                selectedMemberIds.add(u.getId()); // Default: select all overdue members
            }
        }

        cbSelectAll.setChecked(!overdueList.isEmpty());
        adapter.setItems(overdueList);
        updatePreviewBanner();
    }

    private void updatePreviewBanner() {
        int count = selectedMemberIds.size();
        double estCost = count * perSmsCost;
        String bannerText = String.format(Locale.US,
                "Sending to %d members. Estimated cost: ৳%.2f (Deducted from your mess ledger as credit)",
                count, estCost);
        tvCostPreviewBanner.setText(bannerText);
        btnDispatchSms.setEnabled(count > 0);
    }

    private void showSimSelectorDialog() {
        try {
            List<String> options = new ArrayList<>();
            final List<Integer> subIds = new ArrayList<>();
            final List<Integer> slots = new ArrayList<>();

            options.add("সিস্টেম ডিফল্ট (স্বয়ংক্রিয়)");
            subIds.add(-1);
            slots.add(-1);

            int checkedPos = 0;
            if (availableSims != null && !availableSims.isEmpty()) {
                for (int i = 0; i < availableSims.size(); i++) {
                    com.smartmess.android.sms.SmsDispatcher.SimCardInfo sim = availableSims.get(i);
                    options.add(sim.getDisplayLabel());
                    subIds.add(sim.subscriptionId);
                    slots.add(sim.slotIndex);
                    if (sim.subscriptionId == selectedSubscriptionId) {
                        checkedPos = i + 1;
                    }
                }
            } else {
                options.add("সিম ১ (স্লট ১)");
                subIds.add(0);
                slots.add(0);
                options.add("সিম ২ (স্লট ২)");
                subIds.add(1);
                slots.add(1);
            }

            String[] items = options.toArray(new String[0]);
            new AlertDialog.Builder(this)
                    .setTitle("এসএমএস পাঠানোর সিম নির্বাচন করুন")
                    .setSingleChoiceItems(items, checkedPos, (dialog, which) -> {
                        selectedSubscriptionId = subIds.get(which);
                        selectedSimSlot = slots.get(which);
                        btnSelectSim.setText(items[which]);
                        dialog.dismiss();
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
        } catch (Throwable t) {
            Toast.makeText(this, "সিম তালিকা দেখানো যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSmsDispatch() {
        if (!PermissionHelper.hasSmsPermission(this)) {
            PermissionHelper.requestSmsPermission(this);
            return;
        }

        if (selectedMemberIds.isEmpty()) {
            Toast.makeText(this, "দয়া করে অন্তত একজন সদস্য নির্বাচন করুন", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDispatchSms.setEnabled(false);
        btnDispatchSms.setText("Dispatching SMS in Background...");

        final List<OverdueMemberItem> targetsToSend = new ArrayList<>();
        for (OverdueMemberItem item : overdueList) {
            if (selectedMemberIds.contains(item.user.getId())) {
                targetsToSend.add(item);
            }
        }

        final User sender = userDao.getById(sessionManager.getUserId());
        final String messName = sessionManager.getMessName() != null ? sessionManager.getMessName() : "SmartMess";

        // Non-blocking background thread execution
        executorService.execute(() -> {
            int successCount = 0;
            for (OverdueMemberItem target : targetsToSend) {
                String message = String.format(Locale.US,
                        "Notice from %s: Brother %s, your current mess due is ৳%.2f. Please deposit soon to keep bazaar active.",
                        messName, target.user.getName(), Math.abs(target.dueAmount));

                boolean sent = smsCostingManager.sendSingleDueReminder(sender, target.user, message, selectedSubscriptionId);
                if (sent) successCount++;
            }

            final int finalSuccess = successCount;
            mainHandler.post(() -> {
                btnDispatchSms.setEnabled(true);
                btnDispatchSms.setText("Send SMS Reminders");
                Toast.makeText(ActivitySmsDispatch.this,
                        "Dispatched " + finalSuccess + " SMS reminders successfully!",
                        Toast.LENGTH_LONG).show();

                // Trigger background cloud sync for newly created SMS debit/credit ledgers
                SyncManager.triggerSync(getApplicationContext());
                finish();
            });
        });
    }

    // ==========================================
    // DATA MODEL & ADAPTER
    // ==========================================
    public static class OverdueMemberItem {
        public final User user;
        public final double dueAmount;

        public OverdueMemberItem(User user, double dueAmount) {
            this.user = user;
            this.dueAmount = dueAmount;
        }
    }

    private class OverdueMemberAdapter extends RecyclerView.Adapter<OverdueMemberAdapter.ViewHolder> {
        private final List<OverdueMemberItem> items = new ArrayList<>();

        public void setItems(List<OverdueMemberItem> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sms_member, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OverdueMemberItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbSelectMember;
            TextView tvAvatar;
            TextView tvMemberName;
            TextView tvPhone;
            TextView tvDueAmount;

            public ViewHolder(@NonNull View v) {
                super(v);
                cbSelectMember = v.findViewById(R.id.cbSelectMember);
                tvAvatar = v.findViewById(R.id.tvAvatar);
                tvMemberName = v.findViewById(R.id.tvMemberName);
                tvPhone = v.findViewById(R.id.tvPhone);
                tvDueAmount = v.findViewById(R.id.tvDueAmount);
            }

            public void bind(OverdueMemberItem item) {
                User user = item.user;
                tvMemberName.setText(user.getName());
                tvPhone.setText(user.getPhone());
                tvDueAmount.setText("Due: " + CurrencyUtils.format(item.dueAmount));

                String initial = (user.getName() != null && !user.getName().isEmpty())
                        ? String.valueOf(user.getName().charAt(0)).toUpperCase(Locale.ROOT)
                        : "M";
                tvAvatar.setText(initial);

                cbSelectMember.setOnCheckedChangeListener(null);
                cbSelectMember.setChecked(selectedMemberIds.contains(user.getId()));

                cbSelectMember.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedMemberIds.add(user.getId());
                    } else {
                        selectedMemberIds.remove(user.getId());
                    }
                    cbSelectAll.setChecked(selectedMemberIds.size() == items.size());
                    updatePreviewBanner();
                });
            }
        }
    }
}
