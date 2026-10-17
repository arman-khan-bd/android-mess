package com.smartmess.android.ui.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartmess.android.R;
import com.smartmess.android.engine.CalculationModels.MemberBalanceSheet;
import com.smartmess.android.utils.CurrencyUtils;

import java.util.List;

public class MemberSummaryAdapter extends RecyclerView.Adapter<MemberSummaryAdapter.SummaryViewHolder> {

    public interface OnMemberSummaryClickListener {
        void onMemberClick(MemberBalanceSheet member);
    }

    private final List<MemberBalanceSheet> balances;
    private final OnMemberSummaryClickListener listener;

    public MemberSummaryAdapter(List<MemberBalanceSheet> balances) {
        this(balances, null);
    }

    public MemberSummaryAdapter(List<MemberBalanceSheet> balances, OnMemberSummaryClickListener listener) {
        this.balances = balances;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_summary_member, parent, false);
        return new SummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SummaryViewHolder holder, int position) {
        MemberBalanceSheet item = balances.get(position);
        holder.tvSummaryMemberName.setText(item.getUserName());
        holder.tvSummaryMealInfo.setText("মিল: " + item.getConsumedMeals() + " (" + CurrencyUtils.format(item.getMealCost()) + ")");
        holder.tvSummarySharedInfo.setText("শেয়ার ও ইউটিলিটি: " + CurrencyUtils.format(item.getSharedFoodCost() + item.getUtilityAssetCost() + item.getIndividualCost()));
        holder.tvSummaryDeposits.setText("জমা: " + CurrencyUtils.format(item.getTotalDeposit()));
        holder.tvSummaryTotalCost.setText("মোট খরচ: " + CurrencyUtils.format(item.getTotalCost()));

        if (item.getFlexibleMealCost() > 0) {
            holder.layoutSummaryFlexibleRow.setVisibility(View.VISIBLE);
            holder.tvSummaryFlexibleCost.setText("নমনীয় মিল: " + CurrencyUtils.format(item.getFlexibleMealCost()));
        } else {
            holder.layoutSummaryFlexibleRow.setVisibility(View.GONE);
        }

        if (item.isDue()) {
            holder.tvSummaryStatusBadge.setText("- " + CurrencyUtils.format(item.getDueAmount()) + " বকেয়া");
            holder.tvSummaryStatusBadge.setBackgroundResource(R.drawable.badge_due);
            holder.tvSummaryStatusBadge.setTextColor(holder.itemView.getResources().getColor(R.color.due_red));
        } else {
            holder.tvSummaryStatusBadge.setText("+ " + CurrencyUtils.format(item.getNetBalance()) + " উদ্বৃত্ত");
            holder.tvSummaryStatusBadge.setBackgroundResource(R.drawable.badge_credit);
            holder.tvSummaryStatusBadge.setTextColor(holder.itemView.getResources().getColor(R.color.credit_green));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMemberClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return balances != null ? balances.size() : 0;
    }

    static class SummaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvSummaryMemberName, tvSummaryStatusBadge, tvSummaryMealInfo, tvSummarySharedInfo;
        TextView tvSummaryDeposits, tvSummaryTotalCost, tvSummaryFlexibleCost, tvSummaryFlexibleTag;
        View layoutSummaryFlexibleRow;

        public SummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSummaryMemberName = itemView.findViewById(R.id.tvSummaryMemberName);
            tvSummaryStatusBadge = itemView.findViewById(R.id.tvSummaryStatusBadge);
            tvSummaryMealInfo = itemView.findViewById(R.id.tvSummaryMealInfo);
            tvSummarySharedInfo = itemView.findViewById(R.id.tvSummarySharedInfo);
            tvSummaryDeposits = itemView.findViewById(R.id.tvSummaryDeposits);
            tvSummaryTotalCost = itemView.findViewById(R.id.tvSummaryTotalCost);
            tvSummaryFlexibleCost = itemView.findViewById(R.id.tvSummaryFlexibleCost);
            tvSummaryFlexibleTag = itemView.findViewById(R.id.tvSummaryFlexibleTag);
            layoutSummaryFlexibleRow = itemView.findViewById(R.id.layoutSummaryFlexibleRow);
        }
    }
}
