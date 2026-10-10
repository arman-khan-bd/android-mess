package com.smartmess.android.ecommerce;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.smartmess.android.R;
import com.smartmess.android.data.remote.dto.OrderItemDto;
import com.smartmess.android.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartViewHolder> {

    public interface OnCartItemChangeListener {
        void onQuantityChanged(long productId, int newQuantity);
    }

    private final Context context;
    private final List<OrderItemDto> items = new ArrayList<>();
    private final OnCartItemChangeListener listener;

    public CartItemAdapter(Context context, OnCartItemChangeListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<OrderItemDto> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart_product, parent, false);
        return new CartViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        OrderItemDto item = items.get(position);

        holder.tvCartItemTitle.setText(item.getTitle());
        holder.tvCartItemUnitPrice.setText(CurrencyUtils.format(item.getPrice()) + " / " + item.getUnit());
        holder.tvCartItemSubtotal.setText("মোট: " + CurrencyUtils.format(item.getSubtotal()));
        holder.tvCartItemQty.setText(String.valueOf(item.getQuantity()));

        Glide.with(context)
                .load(item.getImage())
                .placeholder(R.drawable.bg_voucher_placeholder)
                .error(R.drawable.bg_voucher_placeholder)
                .centerCrop()
                .into(holder.ivCartItemThumb);

        holder.btnMinusQty.setOnClickListener(v -> {
            if (listener != null && item.getProductId() != null) {
                listener.onQuantityChanged(item.getProductId(), item.getQuantity() - 1);
            }
        });

        holder.btnPlusQty.setOnClickListener(v -> {
            if (listener != null && item.getProductId() != null) {
                listener.onQuantityChanged(item.getProductId(), item.getQuantity() + 1);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCartItemThumb;
        TextView tvCartItemTitle;
        TextView tvCartItemUnitPrice;
        TextView tvCartItemSubtotal;
        TextView tvCartItemQty;
        ImageButton btnMinusQty;
        ImageButton btnPlusQty;

        public CartViewHolder(@NonNull View v) {
            super(v);
            ivCartItemThumb = v.findViewById(R.id.ivCartItemThumb);
            tvCartItemTitle = v.findViewById(R.id.tvCartItemTitle);
            tvCartItemUnitPrice = v.findViewById(R.id.tvCartItemUnitPrice);
            tvCartItemSubtotal = v.findViewById(R.id.tvCartItemSubtotal);
            tvCartItemQty = v.findViewById(R.id.tvCartItemQty);
            btnMinusQty = v.findViewById(R.id.btnMinusQty);
            btnPlusQty = v.findViewById(R.id.btnPlusQty);
        }
    }
}
