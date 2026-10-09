package com.smartmess.android.ui.members;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.model.User;
import com.smartmess.android.ui.sms.DueReminderActivity;
import com.smartmess.android.utils.SessionManager;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private final List<User> members;
    private final Context context;
    private final SessionManager sessionManager;

    public MemberAdapter(Context context, List<User> members) {
        this.context = context;
        this.members = members;
        this.sessionManager = new SessionManager(context);
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User member = members.get(position);
        holder.tvMemberName.setText(member.getName());

        // Phone masking for non-managers
        boolean isManager = sessionManager.isManager();
        boolean isSelf = (sessionManager.getUserId() == member.getId());
        if (isManager || isSelf) {
            holder.tvMemberPhone.setText(member.getPhone());
        } else {
            holder.tvMemberPhone.setText(maskPhoneNumber(member.getPhone()));
        }

        // Avatar
        if (member.getAvatarUrl() != null && !member.getAvatarUrl().trim().isEmpty()) {
            Glide.with(context).load(member.getAvatarUrl()).circleCrop().into(holder.ivItemMemberAvatar);
        } else {
            holder.ivItemMemberAvatar.setImageResource(R.drawable.ic_member);
        }

        // Role Badge
        if (member.isManager()) {
            holder.tvRoleBadge.setText("Manager");
            holder.tvRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.primary));
        } else if (member.isAssistant()) {
            holder.tvRoleBadge.setText("Bazar Boy");
            holder.tvRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.accent));
        } else {
            holder.tvRoleBadge.setText("Member");
            holder.tvRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        }

        // Status Badge
        if (member.isOnLeave()) {
            holder.tvStatusBadge.setText("On Leave");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.warning_amber));
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
        } else if (member.hasLeft()) {
            holder.tvStatusBadge.setText("Left");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.error_red));
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatusBadge.setText("Active");
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.credit_green));
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
        }

        // Open Profile on item click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ActivityUserProfile.class);
            intent.putExtra(ActivityUserProfile.EXTRA_USER_ID, member.getId());
            context.startActivity(intent);
        });

        // Send Due SMS
        if (isManager) {
            holder.btnSendDueSms.setVisibility(View.VISIBLE);
            holder.btnSendDueSms.setOnClickListener(v -> {
                Intent intent = new Intent(context, DueReminderActivity.class);
                intent.putExtra("TARGET_USER_ID", member.getId());
                context.startActivity(intent);
            });
        } else {
            holder.btnSendDueSms.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) return phone != null ? phone : "";
        int len = phone.length();
        String start = phone.substring(0, Math.min(4, len));
        String end = phone.substring(Math.max(len - 4, 0));
        return start + "****" + end;
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemMemberAvatar;
        TextView tvMemberName, tvRoleBadge, tvStatusBadge, tvMemberPhone;
        MaterialButton btnSendDueSms;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemMemberAvatar = itemView.findViewById(R.id.ivItemMemberAvatar);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvMemberPhone = itemView.findViewById(R.id.tvMemberPhone);
            btnSendDueSms = itemView.findViewById(R.id.btnSendDueSms);
        }
    }
}
