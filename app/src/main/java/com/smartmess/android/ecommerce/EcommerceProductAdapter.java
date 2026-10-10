package com.smartmess.android.ecommerce;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.remote.dto.EcommerceProduct;
import com.smartmess.android.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

public class EcommerceProductAdapter extends RecyclerView.Adapter<EcommerceProductAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(EcommerceProduct product);
        void onAddToCartClick(EcommerceProduct product);
    }

    private final Context context;
    private final List<EcommerceProduct> productList = new ArrayList<>();
    private final OnProductClickListener listener;

    public EcommerceProductAdapter(Context context, OnProductClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setProducts(List<EcommerceProduct> products) {
        this.productList.clear();
        if (products != null) {
            this.productList.addAll(products);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ecommerce_product, parent, false);
        return new ProductViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        EcommerceProduct p = productList.get(position);

        holder.tvProductTitle.setText(p.getTitle());
        holder.tvProductCategory.setText(p.getCategory());
        holder.tvProductPrice.setText(CurrencyUtils.format(p.getPrice()));
        holder.tvProductUnit.setText("/ " + p.getUnit());

        // Badge
        if (p.getBadge() != null && !p.getBadge().trim().isEmpty()) {
            holder.tvProductBadge.setVisibility(View.VISIBLE);
            holder.tvProductBadge.setText(p.getBadge().trim());
        } else {
            holder.tvProductBadge.setVisibility(View.GONE);
        }

        // Original / Discount price
        if (p.hasDiscount()) {
            holder.tvProductOriginalPrice.setVisibility(View.VISIBLE);
            holder.tvProductOriginalPrice.setText(CurrencyUtils.format(p.getOriginalPrice()));
            holder.tvProductOriginalPrice.setPaintFlags(holder.tvProductOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            holder.tvDiscountPercent.setVisibility(View.VISIBLE);
            holder.tvDiscountPercent.setText("-" + p.getDiscountPercent() + "%");
        } else {
            holder.tvProductOriginalPrice.setVisibility(View.GONE);
            holder.tvDiscountPercent.setVisibility(View.GONE);
        }

        // Photos count pill
        int photoCount = p.getImages() != null ? p.getImages().size() : 1;
        if (photoCount > 1) {
            holder.tvPhotoCountPill.setVisibility(View.VISIBLE);
            holder.tvPhotoCountPill.setText(photoCount + " photos");
        } else {
            holder.tvPhotoCountPill.setVisibility(View.GONE);
        }

        // Image loading with Glide
        Glide.with(context)
                .load(p.getMainImage())
                .placeholder(R.drawable.bg_voucher_placeholder)
                .error(R.drawable.bg_voucher_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.ivProductImage);

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(p);
        });

        holder.btnAddToCart.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToCartClick(p);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductBadge;
        TextView tvDiscountPercent;
        TextView tvPhotoCountPill;
        TextView tvProductCategory;
        TextView tvProductTitle;
        TextView tvProductPrice;
        TextView tvProductOriginalPrice;
        TextView tvProductUnit;
        MaterialButton btnAddToCart;

        public ProductViewHolder(@NonNull View v) {
            super(v);
            ivProductImage = v.findViewById(R.id.ivProductImage);
            tvProductBadge = v.findViewById(R.id.tvProductBadge);
            tvDiscountPercent = v.findViewById(R.id.tvDiscountPercent);
            tvPhotoCountPill = v.findViewById(R.id.tvPhotoCountPill);
            tvProductCategory = v.findViewById(R.id.tvProductCategory);
            tvProductTitle = v.findViewById(R.id.tvProductTitle);
            tvProductPrice = v.findViewById(R.id.tvProductPrice);
            tvProductOriginalPrice = v.findViewById(R.id.tvProductOriginalPrice);
            tvProductUnit = v.findViewById(R.id.tvProductUnit);
            btnAddToCart = v.findViewById(R.id.btnAddToCart);
        }
    }
}
