package com.smartmess.android.ui.notifications;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.AppNotificationDao;
import com.smartmess.android.model.AppNotification;
import com.smartmess.android.ui.deposits.AddDepositActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DialogNotificationFullView {

    public static void show(Context context, AppNotification notification, Runnable onDismiss) {
        if (context == null || notification == null) return;

        // Auto mark as read in SQLite
        try {
            AppNotificationDao dao = new AppNotificationDao(DatabaseHelper.getInstance(context));
            dao.markAsRead(notification.getId());
            notification.setRead(true);
        } catch (Exception ignored) {}

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_notification_full_view, null);

        TextView tvCategoryBadge = view.findViewById(R.id.tvFullNotifCategoryBadge);
        TextView tvChannelBadge = view.findViewById(R.id.tvFullNotifChannelBadge);
        ImageButton btnClose = view.findViewById(R.id.btnFullNotifClose);
        TextView tvTitle = view.findViewById(R.id.tvFullNotifTitle);
        TextView tvTime = view.findViewById(R.id.tvFullNotifTime);
        TextView tvMessage = view.findViewById(R.id.tvFullNotifMessage);
        TextView tvSenderMeta = view.findViewById(R.id.tvFullNotifSenderMeta);
        TextView tvChannelDetail = view.findViewById(R.id.tvFullNotifChannelDetail);
        MaterialButton btnCopy = view.findViewById(R.id.btnFullNotifCopy);
        MaterialButton btnPayDue = view.findViewById(R.id.btnFullNotifPayDue);
        MaterialButton btnDismiss = view.findViewById(R.id.btnFullNotifDismiss);

        // Category formatting
        String type = notification.getType() != null ? notification.getType().toLowerCase() : "notice";
        if ("reminder".contains(type) || "due".contains(type) || "budget".contains(type)) {
            tvCategoryBadge.setText("💰 DUE REMINDER");
            tvCategoryBadge.setBackgroundResource(R.drawable.badge_due);
            tvCategoryBadge.setTextColor(ContextCompat.getColor(context, R.color.due_red));
            btnPayDue.setVisibility(View.VISIBLE);
        } else if ("meal".contains(type) || "vacation".contains(type)) {
            tvCategoryBadge.setText("🍽️ MEAL ALERT");
            tvCategoryBadge.setBackgroundResource(R.drawable.badge_credit);
            tvCategoryBadge.setTextColor(ContextCompat.getColor(context, R.color.primary_dark));
            btnPayDue.setVisibility(View.GONE);
        } else if ("bazar".contains(type) || "expense".contains(type)) {
            tvCategoryBadge.setText("🛒 BAZAR DUTY");
            tvCategoryBadge.setBackgroundResource(R.drawable.badge_credit);
            tvCategoryBadge.setTextColor(ContextCompat.getColor(context, R.color.credit_green));
            btnPayDue.setVisibility(View.GONE);
        } else if ("emergency".contains(type)) {
            tvCategoryBadge.setText("⚠️ URGENT ALERT");
            tvCategoryBadge.setBackgroundResource(R.drawable.badge_due);
            tvCategoryBadge.setTextColor(ContextCompat.getColor(context, R.color.due_red));
            btnPayDue.setVisibility(View.GONE);
        } else {
            tvCategoryBadge.setText("📢 MESS NOTICE");
            tvCategoryBadge.setBackgroundResource(R.drawable.badge_credit);
            tvCategoryBadge.setTextColor(ContextCompat.getColor(context, R.color.primary_dark));
            btnPayDue.setVisibility(View.GONE);
        }

        // Channel Badge
        String channel = notification.getChannel() != null ? notification.getChannel().toLowerCase() : "push";
        if ("both".equalsIgnoreCase(channel)) {
            tvChannelBadge.setText("🚀 PUSH + SMS");
            tvChannelDetail.setText("Delivered via Dual Channels: Instant Online Push & SIM SMS Dispatch");
        } else if ("sms".equalsIgnoreCase(channel)) {
            tvChannelBadge.setText("💬 SIM SMS");
            tvChannelDetail.setText("Delivered via Cellular SIM SMS (Debited from mess ledger)");
        } else {
            tvChannelBadge.setText("🔔 ONLINE PUSH");
            tvChannelDetail.setText("Delivered via Online Push Notification & System Tray Banner");
        }

        tvTitle.setText(notification.getTitle() != null ? notification.getTitle() : "Notification");
        tvMessage.setText(notification.getMessage() != null ? notification.getMessage() : "");

        // Time
        tvTime.setText(formatDateTime(notification.getCreatedAt()));

        // Sender Meta
        if (notification.getUserId() == 0) {
            tvSenderMeta.setText("📢 Broadcast to All Mess Members");
        } else {
            tvSenderMeta.setText("👤 Direct Notification to You");
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            if (onDismiss != null) onDismiss.run();
        });

        btnDismiss.setOnClickListener(v -> {
            dialog.dismiss();
            if (onDismiss != null) onDismiss.run();
        });

        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                ClipData clip = ClipData.newPlainText("SmartMess Notification",
                        notification.getTitle() + "\n" + notification.getMessage());
                cm.setPrimaryClip(clip);
                Toast.makeText(context, "নোটিফিকেশন ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show();
            }
        });

        btnPayDue.setOnClickListener(v -> {
            dialog.dismiss();
            context.startActivity(new Intent(context, AddDepositActivity.class));
            if (onDismiss != null) onDismiss.run();
        });

        dialog.show();
    }

    private static String formatDateTime(String iso) {
        if (iso == null || iso.isEmpty()) return "Recently";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date d = sdf.parse(iso.replace("Z", ""));
            if (d == null) return "Recently";
            return new SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.US).format(d);
        } catch (Exception e) {
            return "Recently";
        }
    }
}
