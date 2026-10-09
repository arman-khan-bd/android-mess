package com.smartmess.android.ui.expenses;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartmess.android.R;
import com.smartmess.android.model.Expense;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final List<Expense> expenses;

    public ExpenseAdapter(List<Expense> expenses) {
        this.expenses = expenses;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.tvExpenseTitle.setText(expense.getTitle());
        String buyer = expense.getBuyerName() != null ? expense.getBuyerName() : "Buyer #" + expense.getBuyerUserId();
        holder.tvBuyerAndDate.setText("Paid by " + buyer + " • " + DateTimeUtils.formatDisplayDate(expense.getExpenseDate()));
        holder.tvAmount.setText(CurrencyUtils.format(expense.getAmount()));

        // Pool Category Badge
        if (expense.isRawMeal()) {
            holder.tvCategoryBadge.setText("Meal Pool");
            holder.tvCategoryBadge.setTextColor(holder.itemView.getResources().getColor(R.color.credit_green));
            holder.tvCategoryBadge.setBackgroundResource(R.drawable.badge_credit);
            holder.tvSplitType.setText("• Meal Dependent");
        } else if (expense.isSharedFood()) {
            holder.tvCategoryBadge.setText("Shared Food");
            holder.tvCategoryBadge.setTextColor(holder.itemView.getResources().getColor(R.color.accent));
            holder.tvCategoryBadge.setBackgroundResource(R.drawable.badge_credit);
            holder.tvSplitType.setText("• Split All Equal");
        } else if (expense.isUtilityAsset()) {
            holder.tvCategoryBadge.setText("Asset / Utility");
            holder.tvCategoryBadge.setTextColor(holder.itemView.getResources().getColor(R.color.info_blue));
            holder.tvCategoryBadge.setBackgroundResource(R.drawable.badge_credit);
            holder.tvSplitType.setText("• Split All Equal");
        } else {
            holder.tvCategoryBadge.setText("SMS Charge");
            holder.tvCategoryBadge.setTextColor(holder.itemView.getResources().getColor(R.color.warning_amber));
            holder.tvCategoryBadge.setBackgroundResource(R.drawable.badge_credit);
            if (Expense.SPLIT_INDIVIDUAL.equals(expense.getSplitType())) {
                String target = expense.getTargetUserName() != null ? expense.getTargetUserName() : "Member #" + expense.getTargetUserId();
                holder.tvSplitType.setText("• Charged to " + target);
            } else {
                holder.tvSplitType.setText("• Shared Equal");
            }
        }

        if (expense.getVoucherImageUrl() != null && !expense.getVoucherImageUrl().isEmpty()) {
            holder.tvVoucherIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.tvVoucherIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView tvExpenseTitle, tvBuyerAndDate, tvAmount, tvCategoryBadge, tvSplitType, tvVoucherIndicator;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExpenseTitle = itemView.findViewById(R.id.tvExpenseTitle);
            tvBuyerAndDate = itemView.findViewById(R.id.tvBuyerAndDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvSplitType = itemView.findViewById(R.id.tvSplitType);
            tvVoucherIndicator = itemView.findViewById(R.id.tvVoucherIndicator);
        }
    }
}
