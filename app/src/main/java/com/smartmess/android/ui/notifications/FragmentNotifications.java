package com.smartmess.android.ui.notifications;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.AppNotificationDao;
import com.smartmess.android.model.AppNotification;
import com.smartmess.android.notification.PushNotificationSyncWorker;
import com.smartmess.android.utils.BatteryOptimizationHelper;
import com.smartmess.android.utils.NotificationCenterHelper;
import com.smartmess.android.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FragmentNotifications extends Fragment {

    private TextView tvFragUnreadBadge;
    private TextView tvFragSubSummary;
    private TextView btnFragMarkAllRead;
    private ImageButton btnFragClear;

    private ChipGroup chipGroupFilter;
    private LinearLayout cardFragManagerDispatch;
    private MaterialButton btnFragOpenDispatcher;

    private LinearLayout cardFragBatteryPrompt;
    private MaterialButton btnFragEnableBg;

    private LinearLayout layoutFragEmptyNotifs;
    private RecyclerView rvFragNotifications;

    private AppNotificationDao notificationDao;
    private SessionManager sessionManager;
    private NotificationRowAdapter adapter;

    private final List<AppNotification> allNotifications = new ArrayList<>();
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        DatabaseHelper helper = DatabaseHelper.getInstance(requireContext());
        notificationDao = new AppNotificationDao(helper);
        sessionManager = new SessionManager(requireContext());

        initViews(view);
        loadNotifications();

        // Run immediate background sync in case server has fresh notifications
        PushNotificationSyncWorker.runImmediateSync(requireContext());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotifications();
        updateBatteryPromptCard();
    }

    private void initViews(View view) {
        tvFragUnreadBadge = view.findViewById(R.id.tvFragUnreadBadge);
        tvFragSubSummary = view.findViewById(R.id.tvFragSubSummary);
        btnFragMarkAllRead = view.findViewById(R.id.btnFragMarkAllRead);
        btnFragClear = view.findViewById(R.id.btnFragClear);

        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        cardFragManagerDispatch = view.findViewById(R.id.cardFragManagerDispatch);
        btnFragOpenDispatcher = view.findViewById(R.id.btnFragOpenDispatcher);

        cardFragBatteryPrompt = view.findViewById(R.id.cardFragBatteryPrompt);
        btnFragEnableBg = view.findViewById(R.id.btnFragEnableBg);

        layoutFragEmptyNotifs = view.findViewById(R.id.layoutFragEmptyNotifs);
        rvFragNotifications = view.findViewById(R.id.rvFragNotifications);

        rvFragNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationRowAdapter();
        rvFragNotifications.setAdapter(adapter);

        // Manager Controls
        boolean isManager = sessionManager.isManager() || sessionManager.isAssistant();
        if (cardFragManagerDispatch != null) {
            cardFragManagerDispatch.setVisibility(isManager ? View.VISIBLE : View.GONE);
        }
        if (btnFragOpenDispatcher != null) {
            btnFragOpenDispatcher.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), ActivityNotificationManager.class)));
        }

        // Battery optimization banner
        updateBatteryPromptCard();
        if (btnFragEnableBg != null) {
            btnFragEnableBg.setOnClickListener(v ->
                    BatteryOptimizationHelper.showBatteryOptimizationDialog(requireActivity(), this::updateBatteryPromptCard));
        }

        // Mark All Read
        btnFragMarkAllRead.setOnClickListener(v -> {
            long messId = sessionManager.getMessId();
            notificationDao.markAllAsRead(messId);
            Toast.makeText(requireContext(), "All notifications marked as read.", Toast.LENGTH_SHORT).show();
            loadNotifications();
        });

        // Clear All
        btnFragClear.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Clear Notification History?")
                    .setMessage("This will remove all notification records stored on your device.")
                    .setPositiveButton("Clear All", (dialog, which) -> {
                        long messId = sessionManager.getMessId();
                        notificationDao.clearAll(messId);
                        Toast.makeText(requireContext(), "Notification history cleared.", Toast.LENGTH_SHORT).show();
                        loadNotifications();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Filter chips
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipFilterNotices) {
                currentFilter = "notice";
            } else if (checkedId == R.id.chipFilterDues) {
                currentFilter = "due";
            } else if (checkedId == R.id.chipFilterMeals) {
                currentFilter = "meal";
            } else if (checkedId == R.id.chipFilterBazar) {
                currentFilter = "bazar";
            } else {
                currentFilter = "all";
            }
            applyFilter();
        });
    }

    private void updateBatteryPromptCard() {
        if (cardFragBatteryPrompt == null || getContext() == null) return;
        boolean isIgnored = BatteryOptimizationHelper.isBatteryOptimizationIgnored(requireContext());
        cardFragBatteryPrompt.setVisibility(isIgnored ? View.GONE : View.VISIBLE);
    }

    public void loadNotifications() {
        if (getContext() == null) return;
        long messId = sessionManager.getMessId();
        List<AppNotification> list = notificationDao.getNotificationsForMess(messId, 100);
        int unreadCount = notificationDao.getUnreadCount(messId);

        allNotifications.clear();
        allNotifications.addAll(list);

        if (unreadCount > 0) {
            tvFragUnreadBadge.setVisibility(View.VISIBLE);
            tvFragUnreadBadge.setText(unreadCount + " Unread");
            tvFragSubSummary.setText(unreadCount + " unread notification" + (unreadCount > 1 ? "s" : "") + " in your mess");
        } else {
            tvFragUnreadBadge.setVisibility(View.GONE);
            tvFragSubSummary.setText("All caught up • Offline SQLite storage verified");
        }

        applyFilter();
    }

    private void applyFilter() {
        List<AppNotification> filtered = new ArrayList<>();
        for (AppNotification n : allNotifications) {
            String type = (n.getType() != null ? n.getType().toLowerCase() : "");
            if ("all".equals(currentFilter)) {
                filtered.add(n);
            } else if ("notice".equals(currentFilter) && (type.contains("notice") || type.contains("role") || type.contains("system"))) {
                filtered.add(n);
            } else if ("due".equals(currentFilter) && (type.contains("due") || type.contains("budget") || type.contains("deposit") || type.contains("reminder"))) {
                filtered.add(n);
            } else if ("meal".equals(currentFilter) && (type.contains("meal") || type.contains("vacation"))) {
                filtered.add(n);
            } else if ("bazar".equals(currentFilter) && (type.contains("bazar") || type.contains("expense"))) {
                filtered.add(n);
            }
        }

        if (filtered.isEmpty()) {
            layoutFragEmptyNotifs.setVisibility(View.VISIBLE);
            rvFragNotifications.setVisibility(View.GONE);
        } else {
            layoutFragEmptyNotifs.setVisibility(View.GONE);
            rvFragNotifications.setVisibility(View.VISIBLE);
            adapter.setItems(filtered);
        }
    }

    private class NotificationRowAdapter extends RecyclerView.Adapter<NotificationRowAdapter.ViewHolder> {
        private final List<AppNotification> items = new ArrayList<>();

        public void setItems(List<AppNotification> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_row, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppNotification n = items.get(position);
            holder.tvTitle.setText(n.getTitle());
            holder.tvMessage.setText(n.getMessage());
            holder.tvTime.setText(formatRelativeTime(n.getCreatedAt()));

            holder.viewUnreadDot.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);

            String type = n.getType() != null ? n.getType().toLowerCase() : "notice";
            if (type.contains("expense") || type.contains("bazar")) {
                holder.ivIcon.setImageResource(R.drawable.ic_lucide_cart);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.credit_green)));
                holder.layoutIconBadge.setBackgroundResource(R.drawable.badge_credit);
            } else if (type.contains("budget") || type.contains("due") || type.contains("reminder")) {
                holder.ivIcon.setImageResource(R.drawable.ic_lucide_alert_circle);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.due_red)));
                holder.layoutIconBadge.setBackgroundResource(R.drawable.badge_due);
            } else if (type.contains("vacation") || type.contains("meal")) {
                holder.ivIcon.setImageResource(R.drawable.ic_vacation);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent)));
                holder.layoutIconBadge.setBackgroundResource(R.drawable.badge_credit);
            } else if (type.contains("role")) {
                holder.ivIcon.setImageResource(R.drawable.ic_lucide_shield_alert);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.warning_amber)));
                holder.layoutIconBadge.setBackgroundResource(R.drawable.badge_due);
            } else if (type.contains("deposit")) {
                holder.ivIcon.setImageResource(R.drawable.ic_deposit);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary)));
                holder.layoutIconBadge.setBackgroundResource(R.drawable.badge_credit);
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_lucide_bell);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary)));
                holder.layoutIconBadge.setBackgroundResource(R.drawable.badge_credit);
            }

            // Tapping any row opens the rich Full View Notification System
            holder.itemView.setOnClickListener(v -> {
                DialogNotificationFullView.show(requireContext(), n, FragmentNotifications.this::loadNotifications);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private String formatRelativeTime(String createdAtIso) {
            if (createdAtIso == null || createdAtIso.isEmpty()) return "Recently";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date date = sdf.parse(createdAtIso.replace("Z", ""));
                if (date == null) return "Recently";

                long diff = System.currentTimeMillis() - date.getTime();
                long minutes = diff / (60 * 1000);
                long hours = diff / (60 * 60 * 1000);
                long days = diff / (24 * 60 * 60 * 1000);

                if (minutes < 1) return "Just now";
                if (minutes < 60) return minutes + "m ago";
                if (hours < 24) return hours + "h ago";
                if (days == 1) return "Yesterday";
                if (days < 7) return days + "d ago";

                return new SimpleDateFormat("dd MMM", Locale.US).format(date);
            } catch (Exception e) {
                return "Recently";
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            FrameLayout layoutIconBadge;
            ImageView ivIcon;
            TextView tvTitle;
            TextView tvTime;
            TextView tvMessage;
            View viewUnreadDot;

            ViewHolder(View itemView) {
                super(itemView);
                layoutIconBadge = itemView.findViewById(R.id.layoutIconBadge);
                ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
                tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
                tvTime = itemView.findViewById(R.id.tvNotificationTime);
                tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
                viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
            }
        }
    }
}
