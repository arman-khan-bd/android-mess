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

    private final List<MemberBalanceSheet> balances;

    public MemberSummaryAdapter(List<MemberBalanceSheet> balances) {
        this.balances = balances;
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
        holder.tvSummaryMealInfo.setText("Meals: " + item.getConsumedMeals() + " (" + CurrencyUtils.format(item.getMealCost()) + ")");
        holder.tvSummarySharedInfo.setText("Shared & Util: " + CurrencyUtils.format(item.getSharedFoodCost() + item.getUtilityAssetCost() + item.getIndividualCost()));
        holder.tvSummaryDeposits.setText("Deposits: " + CurrencyUtils.format(item.getTotalDeposit()));
        holder.tvSummaryTotalCost.setText("Total Cost: " + CurrencyUtils.format(item.getTotalCost()));

        if (item.isDue()) {
            holder.tvSummaryStatusBadge.setText("- " + CurrencyUtils.format(item.getDueAmount()) + " Due");
            holder.tvSummaryStatusBadge.setBackgroundResource(R.drawable.badge_due);
            holder.tvSummaryStatusBadge.setTextColor(holder.itemView.getResources().getColor(R.color.due_red));
        } else {
            holder.tvSummaryStatusBadge.setText("+ " + CurrencyUtils.format(item.getNetBalance()) + " Credit");
            holder.tvSummaryStatusBadge.setBackgroundResource(R.drawable.badge_credit);
            holder.tvSummaryStatusBadge.setTextColor(holder.itemView.getResources().getColor(R.color.credit_green));
        }
    }

    @Override
    public int getItemCount() {
        return balances.size();
    }

    static class SummaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvSummaryMemberName, tvSummaryStatusBadge, tvSummaryMealInfo, tvSummarySharedInfo, tvSummaryDeposits, tvSummaryTotalCost;

        public SummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSummaryMemberName = itemView.findViewById(R.id.tvSummaryMemberName);
            tvSummaryStatusBadge = itemView.findViewById(R.id.tvSummaryStatusBadge);
            tvSummaryMealInfo = itemView.findViewById(R.id.tvSummaryMealInfo);
            tvSummarySharedInfo = itemView.findViewById(R.id.tvSummarySharedInfo);
            tvSummaryDeposits = itemView.findViewById(R.id.tvSummaryDeposits);
            tvSummaryTotalCost = itemView.findViewById(R.id.tvSummaryTotalCost);
        }
    }
}
