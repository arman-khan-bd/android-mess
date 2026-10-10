package com.smartmess.android.ecommerce;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.smartmess.android.R;
import com.smartmess.android.data.remote.dto.EcommerceProduct;
import com.smartmess.android.utils.CurrencyUtils;

import java.util.List;

public class ActivityProductDetails extends AppCompatActivity implements CartManager.CartListener {

    public static final String EXTRA_PRODUCT = "extra_product";

    private ImageButton btnDetailBack;
    private FrameLayout btnDetailCart;
    private TextView tvDetailCartBadge;
    private ImageView ivDetailMainImage;
    private TextView tvDetailBadge;
    private TextView tvDetailDiscount;
    private TextView tvPhotoIndexIndicator;
    private View scrollThumbnails;
    private LinearLayout layoutThumbnails;
    private TextView tvDetailCategory;
    private TextView tvDetailTitle;
    private TextView tvDetailPrice;
    private TextView tvDetailOriginalPrice;
    private TextView tvDetailUnit;
    private TextView tvDetailStockStatus;
    private TextView tvDetailDescription;
    private ImageButton btnDetailMinusQty;
    private TextView tvDetailSelectedQty;
    private ImageButton btnDetailPlusQty;
    private View btnDetailAddToCart;
    private View btnDetailBuyNow;

    private EcommerceProduct product;
    private CartManager cartManager;
    private int selectedQuantity = 1;
    private int currentPhotoIndex = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        cartManager = CartManager.getInstance(this);

        product = (EcommerceProduct) getIntent().getSerializableExtra(EXTRA_PRODUCT);
        if (product == null) {
            Toast.makeText(this, "পণ্যটি লোড করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        populateProductData();
        setupListeners();
        updateCartBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cartManager.addListener(this);
        updateCartBadge();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cartManager.removeListener(this);
    }

    private void initViews() {
        btnDetailBack = findViewById(R.id.btnDetailBack);
        btnDetailCart = findViewById(R.id.btnDetailCart);
        tvDetailCartBadge = findViewById(R.id.tvDetailCartBadge);
        ivDetailMainImage = findViewById(R.id.ivDetailMainImage);
        tvDetailBadge = findViewById(R.id.tvDetailBadge);
        tvDetailDiscount = findViewById(R.id.tvDetailDiscount);
        tvPhotoIndexIndicator = findViewById(R.id.tvPhotoIndexIndicator);
        scrollThumbnails = findViewById(R.id.scrollThumbnails);
        layoutThumbnails = findViewById(R.id.layoutThumbnails);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailPrice = findViewById(R.id.tvDetailPrice);
        tvDetailOriginalPrice = findViewById(R.id.tvDetailOriginalPrice);
        tvDetailUnit = findViewById(R.id.tvDetailUnit);
        tvDetailStockStatus = findViewById(R.id.tvDetailStockStatus);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        btnDetailMinusQty = findViewById(R.id.btnDetailMinusQty);
        tvDetailSelectedQty = findViewById(R.id.tvDetailSelectedQty);
        btnDetailPlusQty = findViewById(R.id.btnDetailPlusQty);
        btnDetailAddToCart = findViewById(R.id.btnDetailAddToCart);
        btnDetailBuyNow = findViewById(R.id.btnDetailBuyNow);
    }

    private void populateProductData() {
        tvDetailTitle.setText(product.getTitle());
        tvDetailCategory.setText(product.getCategory());
        tvDetailPrice.setText(CurrencyUtils.format(product.getPrice()));
        tvDetailUnit.setText("/ " + product.getUnit());

        if (product.getDescription() != null && !product.getDescription().trim().isEmpty()) {
            tvDetailDescription.setText(product.getDescription().trim());
        } else {
            tvDetailDescription.setText("মেসের রান্নার জন্য সেরা মানের পাইকারি পণ্য। দ্রুত ডেলিভারি সুবিধা।");
        }

        // Badge
        if (product.getBadge() != null && !product.getBadge().trim().isEmpty()) {
            tvDetailBadge.setVisibility(View.VISIBLE);
            tvDetailBadge.setText(product.getBadge().trim());
        } else {
            tvDetailBadge.setVisibility(View.GONE);
        }

        // Discount
        if (product.hasDiscount()) {
            tvDetailOriginalPrice.setVisibility(View.VISIBLE);
            tvDetailOriginalPrice.setText(CurrencyUtils.format(product.getOriginalPrice()));
            tvDetailOriginalPrice.setPaintFlags(tvDetailOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            tvDetailDiscount.setVisibility(View.VISIBLE);
            tvDetailDiscount.setText("-" + product.getDiscountPercent() + "%");
        } else {
            tvDetailOriginalPrice.setVisibility(View.GONE);
            tvDetailDiscount.setVisibility(View.GONE);
        }

        // Stock status
        if (product.getStockQuantity() > 0) {
            tvDetailStockStatus.setText("স্টকে আছে (" + product.getStockQuantity() + " " + product.getUnit() + ")");
            tvDetailStockStatus.setBackgroundResource(R.drawable.badge_credit);
        } else {
            tvDetailStockStatus.setText("স্টক শেষ");
            tvDetailStockStatus.setBackgroundResource(R.drawable.badge_due);
        }

        // Photo Gallery setup
        setupPhotoGallery();
    }

    private void setupPhotoGallery() {
        List<String> images = product.getImages();
        if (images == null || images.isEmpty()) {
            displayPhoto(product.getMainImage(), 0, 1);
            scrollThumbnails.setVisibility(View.GONE);
            return;
        }

        displayPhoto(images.get(0), 0, images.size());

        if (images.size() > 1) {
            scrollThumbnails.setVisibility(View.VISIBLE);
            layoutThumbnails.removeAllViews();

            for (int i = 0; i < images.size(); i++) {
                final int idx = i;
                final String imgUrl = images.get(i);

                ImageView thumb = new ImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(120, 120);
                lp.setMargins(0, 0, 16, 0);
                thumb.setLayoutParams(lp);
                thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                thumb.setBackgroundResource(R.drawable.bg_ticket_item);

                Glide.with(this)
                        .load(imgUrl)
                        .placeholder(R.drawable.bg_voucher_placeholder)
                        .centerCrop()
                        .into(thumb);

                thumb.setOnClickListener(v -> displayPhoto(imgUrl, idx, images.size()));
                layoutThumbnails.addView(thumb);
            }
        } else {
            scrollThumbnails.setVisibility(View.GONE);
        }
    }

    private void displayPhoto(String url, int index, int total) {
        currentPhotoIndex = index;
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.bg_voucher_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(ivDetailMainImage);

        if (total > 1) {
            tvPhotoIndexIndicator.setVisibility(View.VISIBLE);
            tvPhotoIndexIndicator.setText((index + 1) + " / " + total);
        } else {
            tvPhotoIndexIndicator.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnDetailBack.setOnClickListener(v -> finish());

        btnDetailCart.setOnClickListener(v -> {
            if (cartManager.getTotalItemCount() == 0) {
                Toast.makeText(this, "আপনার কার্ট খালি", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(ActivityProductDetails.this, ActivityEcommerceCheckout.class));
        });

        btnDetailMinusQty.setOnClickListener(v -> {
            if (selectedQuantity > 1) {
                selectedQuantity--;
                tvDetailSelectedQty.setText(String.valueOf(selectedQuantity));
            }
        });

        btnDetailPlusQty.setOnClickListener(v -> {
            selectedQuantity++;
            tvDetailSelectedQty.setText(String.valueOf(selectedQuantity));
        });

        btnDetailAddToCart.setOnClickListener(v -> {
            cartManager.addToCart(product, selectedQuantity);
            Toast.makeText(this, selectedQuantity + " " + product.getUnit() + " " + product.getTitle() + " কার্টে যোগ করা হয়েছে!", Toast.LENGTH_SHORT).show();
            updateCartBadge();
        });

        btnDetailBuyNow.setOnClickListener(v -> {
            cartManager.addToCart(product, selectedQuantity);
            startActivity(new Intent(ActivityProductDetails.this, ActivityEcommerceCheckout.class));
        });
    }

    private void updateCartBadge() {
        int count = cartManager.getTotalItemCount();
        if (count > 0) {
            tvDetailCartBadge.setVisibility(View.VISIBLE);
            tvDetailCartBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            tvDetailCartBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCartChanged() {
        runOnUiThread(this::updateCartBadge);
    }
}
