package com.smartmess.android.ecommerce;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.smartmess.android.R;
import com.smartmess.android.data.remote.dto.EcommerceOrder;
import com.smartmess.android.data.remote.dto.OrderItemDto;
import com.smartmess.android.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

public class UserOrderAdapter extends RecyclerView.Adapter<UserOrderAdapter.OrderViewHolder> {

    private final Context context;
    private final List<EcommerceOrder> orders = new ArrayList<>();

    public UserOrderAdapter(Context context) {
        this.context = context;
    }

    public void setOrders(List<EcommerceOrder> newOrders) {
        this.orders.clear();
        if (newOrders != null) {
            this.orders.addAll(newOrders);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_order, parent, false);
        return new OrderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        EcommerceOrder order = orders.get(position);

        holder.tvOrderNumber.setText("#" + order.getOrderNumber());
        holder.tvOrderDate.setText("তারিখ: " + (order.getCreatedAt() != null ? order.getCreatedAt() : "আজকে"));
        holder.tvOrderAddress.setText("ঠিকানা: " + order.getDeliveryAddress());
        holder.tvOrderPaymentMethod.setText("পেমেন্ট: " + ("cash_on_delivery".equalsIgnoreCase(order.getPaymentMethod()) ? "ক্যাশ অন ডেলিভারি" : "বিকাশ/অনলাইন"));
        holder.tvOrderTotalAmount.setText(CurrencyUtils.format(order.getTotalAmount()));

        // Status Badge
        setupStatusBadge(holder.tvOrderStatusBadge, order.getStatus());

        // Items summary string
        StringBuilder sb = new StringBuilder();
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (int i = 0; i < order.getItems().size(); i++) {
                OrderItemDto item = order.getItems().get(i);
                if (i > 0) sb.append(", ");
                sb.append(item.getQuantity()).append(" × ").append(item.getTitle());
            }
        } else {
            sb.append("মেস নিত্যপ্রয়োজনীয় খাদ্যপণ্য");
        }
        holder.tvOrderItemsSummary.setText(sb.toString());
    }

    private void setupStatusBadge(TextView badge, String status) {
        if (status == null) status = "pending";
        switch (status.toLowerCase()) {
            case "delivered":
                badge.setText("ডেলিভার্ড ✓");
                badge.setBackgroundResource(R.drawable.bg_mint_pill);
                badge.setTextColor(Color.parseColor("#10B981"));
                break;
            case "processing":
            case "confirmed":
                badge.setText("প্রসেসিং...");
                badge.setBackgroundResource(R.drawable.bg_dark_pill);
                badge.setTextColor(Color.WHITE);
                break;
            case "shipped":
                badge.setText("অন দ্য ওয়ে 🚚");
                badge.setBackgroundResource(R.drawable.bg_dark_pill);
                badge.setTextColor(Color.parseColor("#A855F7"));
                break;
            case "cancelled":
                badge.setText("বাতিল");
                badge.setBackgroundResource(R.drawable.badge_due);
                badge.setTextColor(Color.parseColor("#DC2626"));
                break;
            case "pending":
            default:
                badge.setText("পেন্ডিং");
                badge.setBackgroundResource(R.drawable.badge_due);
                badge.setTextColor(Color.parseColor("#D97706"));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderNumber;
        TextView tvOrderStatusBadge;
        TextView tvOrderDate;
        TextView tvOrderAddress;
        TextView tvOrderItemsSummary;
        TextView tvOrderPaymentMethod;
        TextView tvOrderTotalAmount;

        public OrderViewHolder(@NonNull View v) {
            super(v);
            tvOrderNumber = v.findViewById(R.id.tvOrderNumber);
            tvOrderStatusBadge = v.findViewById(R.id.tvOrderStatusBadge);
            tvOrderDate = v.findViewById(R.id.tvOrderDate);
            tvOrderAddress = v.findViewById(R.id.tvOrderAddress);
            tvOrderItemsSummary = v.findViewById(R.id.tvOrderItemsSummary);
            tvOrderPaymentMethod = v.findViewById(R.id.tvOrderPaymentMethod);
            tvOrderTotalAmount = v.findViewById(R.id.tvOrderTotalAmount);
        }
    }
}
