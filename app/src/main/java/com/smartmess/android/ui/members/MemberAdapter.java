package com.smartmess.android.ui.members;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.model.User;
import com.smartmess.android.ui.sms.DueReminderActivity;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private final List<User> members;
    private final Context context;

    public MemberAdapter(Context context, List<User> members) {
        this.context = context;
        this.members = members;
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
        holder.tvMemberPhone.setText(member.getPhone());

        // Role Badge
        String role = member.getRole() != null ? member.getRole() : "member";
        if (member.isManager()) {
            holder.tvRoleBadge.setText("Manager");
            holder.tvRoleBadge.setTextColor(context.getResources().getColor(R.color.primary));
        } else if (member.isAssistant()) {
            holder.tvRoleBadge.setText("Bazar Boy");
            holder.tvRoleBadge.setTextColor(context.getResources().getColor(R.color.accent));
        } else {
            holder.tvRoleBadge.setText("Member");
            holder.tvRoleBadge.setTextColor(context.getResources().getColor(R.color.text_secondary));
        }

        holder.btnSendDueSms.setOnClickListener(v -> {
            Intent intent = new Intent(context, DueReminderActivity.class);
            intent.putExtra("TARGET_USER_ID", member.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvMemberName, tvRoleBadge, tvMemberPhone;
        MaterialButton btnSendDueSms;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
            tvMemberPhone = itemView.findViewById(R.id.tvMemberPhone);
            btnSendDueSms = itemView.findViewById(R.id.btnSendDueSms);
        }
    }
}
