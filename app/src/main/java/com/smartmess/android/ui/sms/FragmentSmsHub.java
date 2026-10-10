package com.smartmess.android.ui.sms;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.SmsLogDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.SmsLog;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.List;
import java.util.Locale;

public class FragmentSmsHub extends Fragment {

    private TextView tvPerSmsCostBadge;
    private TextView tvSmsSummarySubtitle;
    private View cardActionDueReminder;
    private View cardActionBroadcast;
    private View cardActionBulkDispatch;
    private TextView tvSmsHistoryCount;
    private TextView tvEmptySmsLogs;
    private LinearLayout layoutSmsLogsContainer;

    private SessionManager sessionManager;
    private MessDao messDao;
    private SmsLogDao smsLogDao;
    private UserDao userDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sms_hub, container, false);

        sessionManager = new SessionManager(requireContext());
        DatabaseHelper helper = DatabaseHelper.getInstance(requireContext());
        messDao = new MessDao(helper);
        smsLogDao = new SmsLogDao(helper);
        userDao = new UserDao(helper);

        tvPerSmsCostBadge = v.findViewById(R.id.tvPerSmsCostBadge);
        tvSmsSummarySubtitle = v.findViewById(R.id.tvSmsSummarySubtitle);
        cardActionDueReminder = v.findViewById(R.id.cardActionDueReminder);
        cardActionBroadcast = v.findViewById(R.id.cardActionBroadcast);
        cardActionBulkDispatch = v.findViewById(R.id.cardActionBulkDispatch);
        tvSmsHistoryCount = v.findViewById(R.id.tvSmsHistoryCount);
        tvEmptySmsLogs = v.findViewById(R.id.tvEmptySmsLogs);
        layoutSmsLogsContainer = v.findViewById(R.id.layoutSmsLogsContainer);

        cardActionDueReminder.setOnClickListener(view ->
                startActivity(new Intent(requireContext(), DueReminderActivity.class)));

        cardActionBroadcast.setOnClickListener(view ->
                startActivity(new Intent(requireContext(), SmsBroadcastActivity.class)));

        cardActionBulkDispatch.setOnClickListener(view ->
                startActivity(new Intent(requireContext(), ActivitySmsDispatch.class)));

        loadSmsData();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSmsData();
    }

    private void loadSmsData() {
        try {
            long messId = sessionManager.getMessId();
            Mess mess = messDao.getById(messId);
            double cost = (mess != null && mess.getPerSmsCost() > 0) ? mess.getPerSmsCost() : 0.50;
            if (tvPerSmsCostBadge != null) {
                tvPerSmsCostBadge.setText(String.format(Locale.US, "প্রতি এসএমএস: %s", CurrencyUtils.format(cost)));
            }

            int memberCount = userDao.getActiveMembersByMess(messId).size();
            if (tvSmsSummarySubtitle != null) {
                tvSmsSummarySubtitle.setText(String.format(Locale.US,
                        "বর্তমান মেস সদস্য সংখ্যা: %d জন। জরুরি বিজ্ঞপ্তি ও বকেয়া তাগাদা সরাসরি মোবাইলে এসএমএস হিসেবে পাঠান।",
                        memberCount));
            }

            // Load Recent Logs
            List<SmsLog> logs = smsLogDao.getLogsByMess(messId, 25);
            if (layoutSmsLogsContainer != null) {
                layoutSmsLogsContainer.removeAllViews();
            }

            if (logs == null || logs.isEmpty()) {
                if (tvEmptySmsLogs != null) tvEmptySmsLogs.setVisibility(View.VISIBLE);
                if (tvSmsHistoryCount != null) tvSmsHistoryCount.setText("০ টি লগ");
            } else {
                if (tvEmptySmsLogs != null) tvEmptySmsLogs.setVisibility(View.GONE);
                if (tvSmsHistoryCount != null) tvSmsHistoryCount.setText(logs.size() + " টি সাম্প্রতিক লগ");

                LayoutInflater inflater = LayoutInflater.from(requireContext());
                for (SmsLog log : logs) {
                    View itemView = inflater.inflate(R.layout.item_sms_history_card, layoutSmsLogsContainer, false);
                    TextView tvRecipient = itemView.findViewById(R.id.tvSmsRecipient);
                    TextView tvCost = itemView.findViewById(R.id.tvSmsCostTag);
                    TextView tvSnippet = itemView.findViewById(R.id.tvSmsMessageSnippet);
                    TextView tvTime = itemView.findViewById(R.id.tvSmsTimestamp);
                    TextView tvStatus = itemView.findViewById(R.id.tvSmsDeliveryStatus);

                    String recipient = log.getRecipientPhone();
                    String targetName = log.getTargetName();
                    if (targetName != null && !targetName.trim().isEmpty()) {
                        if (recipient != null && !recipient.trim().isEmpty()) {
                            recipient = targetName.trim() + " (" + recipient + ")";
                        } else {
                            recipient = targetName.trim();
                        }
                    }
                    tvRecipient.setText(recipient != null && !recipient.trim().isEmpty() ? recipient : "সদস্য");

                    tvCost.setText(CurrencyUtils.format(log.getCostApplied()));
                    tvSnippet.setText(log.getMessageContent() != null ? log.getMessageContent() : "এসএমএস বিজ্ঞপ্তি");
                    tvTime.setText(DateTimeUtils.formatDisplayDate(log.getCreatedAt()));

                    String status = log.getDeliveryStatus();
                    if ("delivered".equalsIgnoreCase(status) || "sent".equalsIgnoreCase(status)) {
                        tvStatus.setText("সফলভাবে প্রেরিত");
                        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.credit_green));
                    } else if ("failed".equalsIgnoreCase(status)) {
                        tvStatus.setText("ব্যর্থ হয়েছে");
                        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.due_red));
                    } else {
                        tvStatus.setText("প্রক্রিয়াধীন");
                        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_amber));
                    }

                    if (layoutSmsLogsContainer != null) {
                        layoutSmsLogsContainer.addView(itemView);
                    }
                }
            }
        } catch (Throwable t) {
            android.util.Log.e("FragmentSmsHub", "Error loading SMS hub data: " + t.getMessage(), t);
        }
    }
}
