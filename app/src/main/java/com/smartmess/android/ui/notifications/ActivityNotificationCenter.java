package com.smartmess.android.ui.notifications;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import com.google.android.material.button.MaterialButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.AppNotificationDao;
import com.smartmess.android.model.AppNotification;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.NotificationCenterHelper;
import com.smartmess.android.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityNotificationCenter extends AppCompatActivity {

    private ImageButton btnBackNotifications;
    private TextView btnMarkAllRead;
    private ImageButton btnClearNotifications;
    private TextView tvUnreadSummary;
    private TextView tvUnreadCountBadge;
    private View layoutEmptyNotifications;
    private RecyclerView rvNotifications;

    private AppNotificationDao notificationDao;
    private SessionManager sessionManager;
    private NotificationAdapter adapter;

    private View layoutManagerSendBanner;
    private MaterialButton btnOpenNotifManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        notificationDao = new AppNotificationDao(helper);
        sessionManager = new SessionManager(this);

        initViews();
        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void initViews() {
        btnBackNotifications = findViewById(R.id.btnBackNotifications);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        btnClearNotifications = findViewById(R.id.btnClearNotifications);
        tvUnreadSummary = findViewById(R.id.tvUnreadSummary);
        tvUnreadCountBadge = findViewById(R.id.tvUnreadCountBadge);
        layoutEmptyNotifications = findViewById(R.id.layoutEmptyNotifications);
        rvNotifications = findViewById(R.id.rvNotifications);

        layoutManagerSendBanner = findViewById(R.id.layoutManagerSendBanner);
        btnOpenNotifManager = findViewById(R.id.btnOpenNotifManager);

        boolean isManager = sessionManager.isManager() || sessionManager.isAssistant();
        if (layoutManagerSendBanner != null) {
            layoutManagerSendBanner.setVisibility(isManager ? View.VISIBLE : View.GONE);
        }
        if (btnOpenNotifManager != null) {
            btnOpenNotifManager.setOnClickListener(v ->
                    startActivity(new Intent(ActivityNotificationCenter.this, ActivityNotificationManager.class)));
        }

        btnBackNotifications.setOnClickListener(v -> finish());

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter();
        rvNotifications.setAdapter(adapter);

        btnMarkAllRead.setOnClickListener(v -> {
            long messId = sessionManager.getMessId();
            notificationDao.markAllAsRead(messId);
            Toast.makeText(this, "All notifications marked as read.", Toast.LENGTH_SHORT).show();
            loadNotifications();
        });

        btnClearNotifications.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear All Notifications?")
                    .setMessage("This will remove all stored notification records from your device.")
                    .setPositiveButton("Clear All", (dialog, which) -> {
                        long messId = sessionManager.getMessId();
                        notificationDao.clearAll(messId);
                        Toast.makeText(this, "Notification history cleared.", Toast.LENGTH_SHORT).show();
                        loadNotifications();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadNotifications() {
        long messId = sessionManager.getMessId();
        List<AppNotification> list = notificationDao.getNotificationsForMess(messId, 100);
        int unreadCount = notificationDao.getUnreadCount(messId);

        if (unreadCount > 0) {
            tvUnreadCountBadge.setVisibility(View.VISIBLE);
            tvUnreadCountBadge.setText(unreadCount + " Unread");
            tvUnreadSummary.setText(unreadCount + " unread alerts in your mess");
        } else {
            tvUnreadCountBadge.setVisibility(View.GONE);
            tvUnreadSummary.setText("All caught up • Stored offline in SQLite");
        }

        if (list.isEmpty()) {
            layoutEmptyNotifications.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            layoutEmptyNotifications.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
            adapter.setItems(list);
        }
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
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

            // Icon & Badge background styling based on notification type
            String type = n.getType();
            if (NotificationCenterHelper.TYPE_EXPENSE.equalsIgnoreCase(type)) {
                holder.ivIcon.setImageResource(R.drawable.ic_lucide_cart);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.credit_green)));
                holder.layoutIconBadge.setBackgroundResource(R.drawable.badge_credit);
            } else if (NotificationCenterHelper.TYPE_BUDGET.equalsIgnoreCase(type)) {
                holder.ivIcon.setImageResource(R.drawable.ic_lucide_alert_circle);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.due_red)));
                holder.layoutIconBadge.setBackgroundResource(R.drawable.badge_due);
            } else if (NotificationCenterHelper.TYPE_VACATION.equalsIgnoreCase(type)) {
                holder.ivIcon.setImageResource(R.drawable.ic_vacation);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent)));
                holder.layoutIconBadge.setBackgroundResource(R.drawable.badge_credit);
            } else if (NotificationCenterHelper.TYPE_ROLE.equalsIgnoreCase(type)) {
                holder.ivIcon.setImageResource(R.drawable.ic_lucide_shield_alert);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.warning_amber)));
                holder.layoutIconBadge.setBackgroundResource(R.drawable.badge_due);
            } else if (NotificationCenterHelper.TYPE_DEPOSIT.equalsIgnoreCase(type)) {
                holder.ivIcon.setImageResource(R.drawable.ic_deposit);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary)));
                holder.layoutIconBadge.setBackgroundResource(R.drawable.badge_credit);
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_lucide_bell);
                holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary)));
                holder.layoutIconBadge.setBackgroundResource(R.drawable.badge_credit);
            }

            holder.itemView.setOnClickListener(v -> {
                DialogNotificationFullView.show(ActivityNotificationCenter.this, n, () -> {
                    notifyItemChanged(holder.getAdapterPosition());
                    int unread = notificationDao.getUnreadCount(sessionManager.getMessId());
                    if (unread > 0) {
                        tvUnreadCountBadge.setVisibility(View.VISIBLE);
                        tvUnreadCountBadge.setText(unread + " Unread");
                        tvUnreadSummary.setText(unread + " unread alerts in your mess");
                    } else {
                        tvUnreadCountBadge.setVisibility(View.GONE);
                        tvUnreadSummary.setText("All caught up • Stored offline in SQLite");
                    }
                });
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
