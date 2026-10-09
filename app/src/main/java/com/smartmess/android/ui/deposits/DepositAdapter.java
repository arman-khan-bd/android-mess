package com.smartmess.android.ui.deposits;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartmess.android.R;
import com.smartmess.android.model.Deposit;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;

import java.util.List;

public class DepositAdapter extends RecyclerView.Adapter<DepositAdapter.DepositViewHolder> {

    private final List<Deposit> deposits;

    public DepositAdapter(List<Deposit> deposits) {
        this.deposits = deposits;
    }

    @NonNull
    @Override
    public DepositViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deposit, parent, false);
        return new DepositViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DepositViewHolder holder, int position) {
        Deposit deposit = deposits.get(position);
        holder.tvDepositMember.setText(deposit.getUserName() != null ? deposit.getUserName() : "Member #" + deposit.getUserId());
        holder.tvDepositNote.setText(deposit.getNote() != null && !deposit.getNote().isEmpty() ? deposit.getNote() : "Deposit Credit");
        holder.tvDepositDate.setText(DateTimeUtils.formatDisplayDate(deposit.getDepositDate()));
        holder.tvDepositAmount.setText("+ " + CurrencyUtils.format(deposit.getAmount()));
    }

    @Override
    public int getItemCount() {
        return deposits.size();
    }

    static class DepositViewHolder extends RecyclerView.ViewHolder {
        TextView tvDepositMember, tvDepositNote, tvDepositDate, tvDepositAmount;

        public DepositViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDepositMember = itemView.findViewById(R.id.tvDepositMember);
            tvDepositNote = itemView.findViewById(R.id.tvDepositNote);
            tvDepositDate = itemView.findViewById(R.id.tvDepositDate);
            tvDepositAmount = itemView.findViewById(R.id.tvDepositAmount);
        }
    }
}
