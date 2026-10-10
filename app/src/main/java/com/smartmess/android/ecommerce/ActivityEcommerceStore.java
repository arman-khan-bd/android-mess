package com.smartmess.android.ecommerce;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.smartmess.android.R;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.EcommerceProduct;
import com.smartmess.android.data.remote.dto.EcommerceResponse;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityEcommerceStore extends AppCompatActivity implements CartManager.CartListener {

    private ImageButton btnBack;
    private TextView btnMyOrders;
    private FrameLayout btnTopCart;
    private TextView tvTopCartBadge;
    private EditText etSearchStore;
    private ImageButton btnClearSearch;
    private LinearLayout layoutCategoryChips;
    private SwipeRefreshLayout swipeRefreshStore;
    private RecyclerView rvStoreProducts;
    private ProgressBar pbStoreLoading;
    private View layoutStoreEmpty;
    private View btnResetFilters;
    private View layoutBottomCartBar;
    private TextView tvBottomCartSummary;
    private View btnProceedToCheckout;

    private EcommerceProductAdapter productAdapter;
    private CartManager cartManager;
    private SessionManager sessionManager;
    private ApiService apiService;

    private String selectedCategory = "সব";
    private String currentSearchQuery = "";
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final List<String> defaultCategories = Arrays.asList(
            "সব", "চাল ও খাদ্যশস্য", "তেল ও মসলা", "কাঁচাবাজার ও সবজি", "মেস সামগ্রী ও পরিষ্কারক"
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecommerce_store);

        sessionManager = new SessionManager(this);
        cartManager = CartManager.getInstance(this);
        apiService = ApiClient.getApiService(this);

        initViews();
        setupCategoryChips(defaultCategories);
        setupRecyclerView();
        setupListeners();

        loadProducts(true);
        updateCartUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cartManager.addListener(this);
        updateCartUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cartManager.removeListener(this);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnMyOrders = findViewById(R.id.btnMyOrders);
        btnTopCart = findViewById(R.id.btnTopCart);
        tvTopCartBadge = findViewById(R.id.tvTopCartBadge);
        etSearchStore = findViewById(R.id.etSearchStore);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        layoutCategoryChips = findViewById(R.id.layoutCategoryChips);
        swipeRefreshStore = findViewById(R.id.swipeRefreshStore);
        rvStoreProducts = findViewById(R.id.rvStoreProducts);
        pbStoreLoading = findViewById(R.id.pbStoreLoading);
        layoutStoreEmpty = findViewById(R.id.layoutStoreEmpty);
        btnResetFilters = findViewById(R.id.btnResetFilters);
        layoutBottomCartBar = findViewById(R.id.layoutBottomCartBar);
        tvBottomCartSummary = findViewById(R.id.tvBottomCartSummary);
        btnProceedToCheckout = findViewById(R.id.btnProceedToCheckout);
    }

    private void setupRecyclerView() {
        GridLayoutManager glm = new GridLayoutManager(this, 2);
        rvStoreProducts.setLayoutManager(glm);

        productAdapter = new EcommerceProductAdapter(this, new EcommerceProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(EcommerceProduct product) {
                Intent intent = new Intent(ActivityEcommerceStore.this, ActivityProductDetails.class);
                intent.putExtra(ActivityProductDetails.EXTRA_PRODUCT, product);
                startActivity(intent);
            }

            @Override
            public void onAddToCartClick(EcommerceProduct product) {
                cartManager.addToCart(product, 1);
                Toast.makeText(ActivityEcommerceStore.this, product.getTitle() + " কার্টে যোগ হয়েছে", Toast.LENGTH_SHORT).show();
            }
        });

        rvStoreProducts.setAdapter(productAdapter);
    }

    private void setupCategoryChips(List<String> categories) {
        layoutCategoryChips.removeAllViews();

        for (String cat : categories) {
            TextView chip = new TextView(this);
            chip.setText(cat);
            chip.setTextSize(11f);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.setPadding(32, 14, 32, 14);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(lp);

            boolean isSelected = cat.equals(selectedCategory);
            updateChipStyle(chip, isSelected);

            chip.setOnClickListener(v -> {
                selectedCategory = cat;
                setupCategoryChips(categories);
                loadProducts(true);
            });

            layoutCategoryChips.addView(chip);
        }
    }

    private void updateChipStyle(TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_dark_pill);
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundResource(R.drawable.bg_mint_pill);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnMyOrders.setOnClickListener(v -> {
            startActivity(new Intent(ActivityEcommerceStore.this, ActivityEcommerceOrders.class));
        });

        btnTopCart.setOnClickListener(v -> openCheckout());
        btnProceedToCheckout.setOnClickListener(v -> openCheckout());

        swipeRefreshStore.setOnRefreshListener(() -> loadProducts(false));

        btnResetFilters.setOnClickListener(v -> {
            selectedCategory = "সব";
            etSearchStore.setText("");
            currentSearchQuery = "";
            setupCategoryChips(defaultCategories);
            loadProducts(true);
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearchStore.setText("");
            btnClearSearch.setVisibility(View.GONE);
            currentSearchQuery = "";
            loadProducts(true);
        });

        etSearchStore.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> {
                    currentSearchQuery = s.toString().trim();
                    loadProducts(false);
                };
                searchHandler.postDelayed(searchRunnable, 400);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void openCheckout() {
        if (cartManager.getTotalItemCount() == 0) {
            Toast.makeText(this, "আপনার কার্ট বর্তমানে খালি। কেনাকাটা করতে পণ্য যোগ করুন।", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(ActivityEcommerceStore.this, ActivityEcommerceCheckout.class));
    }

    private void loadProducts(boolean showProgress) {
        if (showProgress) {
            pbStoreLoading.setVisibility(View.VISIBLE);
            layoutStoreEmpty.setVisibility(View.GONE);
        }

        String catParam = "সব".equals(selectedCategory) ? null : selectedCategory;
        String searchParam = currentSearchQuery.isEmpty() ? null : currentSearchQuery;

        apiService.getEcommerceProducts(catParam, searchParam).enqueue(new Callback<EcommerceResponse>() {
            @Override
            public void onResponse(Call<EcommerceResponse> call, Response<EcommerceResponse> response) {
                pbStoreLoading.setVisibility(View.GONE);
                swipeRefreshStore.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<EcommerceProduct> list = response.body().getData();
                    productAdapter.setProducts(list);

                    // Update dynamic categories if provided
                    if (response.body().getCategories() != null && !response.body().getCategories().isEmpty()) {
                        List<String> dynamicCats = new ArrayList<>();
                        dynamicCats.add("সব");
                        for (String c : response.body().getCategories()) {
                            if (!dynamicCats.contains(c)) dynamicCats.add(c);
                        }
                        if (dynamicCats.size() > defaultCategories.size()) {
                            setupCategoryChips(dynamicCats);
                        }
                    }

                    layoutStoreEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    useFallbackProducts();
                }
            }

            @Override
            public void onFailure(Call<EcommerceResponse> call, Throwable t) {
                pbStoreLoading.setVisibility(View.GONE);
                swipeRefreshStore.setRefreshing(false);
                useFallbackProducts();
            }
        });
    }

    private void useFallbackProducts() {
        List<EcommerceProduct> fallbacks = generateStarterProducts();
        List<EcommerceProduct> filtered = new ArrayList<>();

        for (EcommerceProduct p : fallbacks) {
            boolean matchesCat = "সব".equals(selectedCategory) || p.getCategory().equalsIgnoreCase(selectedCategory);
            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    p.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase()) ||
                    p.getCategory().toLowerCase().contains(currentSearchQuery.toLowerCase());

            if (matchesCat && matchesSearch) {
                filtered.add(p);
            }
        }

        productAdapter.setProducts(filtered);
        layoutStoreEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private List<EcommerceProduct> generateStarterProducts() {
        List<EcommerceProduct> list = new ArrayList<>();

        EcommerceProduct p1 = new EcommerceProduct();
        p1.setId(1);
        p1.setTitle("প্রিমিয়াম মিনিকেট চাল (৫০ কেজি বস্তা)");
        p1.setCategory("চাল ও খাদ্যশস্য");
        p1.setPrice(3450);
        p1.setOriginalPrice(3650.0);
        p1.setUnit("বস্তা (৫০ কেজি)");
        p1.setBadge("মেস স্পেশাল");
        p1.setMainImage("https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=600&q=80");
        p1.setDescription("মেসের জন্য সর্বোচ্চ মানের ঝরঝরে ও সুগন্ধি মিনিকেট চাল। সরাসরি মিল থেকে সংগৃহীত। দ্রুত ফুটন্ত এবং ভাতের পরিমাণ বেশি হয়।");
        list.add(p1);

        EcommerceProduct p2 = new EcommerceProduct();
        p2.setId(2);
        p2.setTitle("তীর ফর্টিফাইড সয়াবিন তেল (৫ লিটার জার)");
        p2.setCategory("তেল ও মসলা");
        p2.setPrice(890);
        p2.setOriginalPrice(930.0);
        p2.setUnit("৫ লিটার জার");
        p2.setBadge("হট ডিল");
        p2.setMainImage("https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=600&q=80");
        p2.setDescription("১০০% বিশুদ্ধ ভিটামিন এ সমৃদ্ধ সয়াবিন তেল। মেসের রান্নার জন্য সাশ্রয়ী ৫ লিটার পারিবারিক জার। কোলেস্টেরল মুক্ত ও স্বাস্থ্যকর।");
        list.add(p2);

        EcommerceProduct p3 = new EcommerceProduct();
        p3.setId(3);
        p3.setTitle("দেশি মসুর ডাল প্রিমিয়াম (৫ কেজি প্যাক)");
        p3.setCategory("চাল ও খাদ্যশস্য");
        p3.setPrice(670);
        p3.setOriginalPrice(720.0);
        p3.setUnit("৫ কেজি প্যাক");
        p3.setBadge("পাইকারি দর");
        p3.setMainImage("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80");
        p3.setDescription("ভালোভাবে পরিশোধিত ছোট দানার দেশি মসুর ডাল। দ্রুত সেদ্ধ হয় এবং ঘ্রাণ অতুলনীয়।");
        list.add(p3);

        EcommerceProduct p4 = new EcommerceProduct();
        p4.setId(4);
        p4.setTitle("মেস হাইজিন ক্লিনিং মেগা প্যাক");
        p4.setCategory("মেস সামগ্রী ও পরিষ্কারক");
        p4.setPrice(520);
        p4.setOriginalPrice(580.0);
        p4.setUnit("কম্বো সেট");
        p4.setBadge("কম্বো অফার");
        p4.setMainImage("https://images.unsplash.com/photo-1583947215259-38e31be8751f?auto=format&fit=crop&w=600&q=80");
        p4.setDescription("মেসের ডাইনিং ও ওয়াশরুমের জন্য কমপ্লিট ক্লিনিং কিট: ১টি হারপিক ১ লিটার + ১টি ভিম লিকুইড ১ লিটার + ২টি হুইল সাবান + ১টি ফ্লোর ক্লিনার।");
        list.add(p4);

        EcommerceProduct p5 = new EcommerceProduct();
        p5.setId(5);
        p5.setTitle("দেশি লাল আলু (২০ কেজি বস্তা)");
        p5.setCategory("কাঁচাবাজার ও সবজি");
        p5.setPrice(780);
        p5.setOriginalPrice(850.0);
        p5.setUnit("২০ কেজি বস্তা");
        p5.setBadge("টাটকা স্টক");
        p5.setMainImage("https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=600&q=80");
        p5.setDescription("টাটকা বগুড়ার লাল গোল আলু। রান্নায় সুস্বাদু ও দীর্ঘস্থায়ী। মেসের মাসিক বা পাক্ষিক মজুদের জন্য পারফেক্ট।");
        list.add(p5);

        return list;
    }

    private void updateCartUi() {
        int count = cartManager.getTotalItemCount();
        double subtotal = cartManager.getSubtotal();

        // Top Badge
        if (count > 0) {
            tvTopCartBadge.setVisibility(View.VISIBLE);
            tvTopCartBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            layoutBottomCartBar.setVisibility(View.VISIBLE);
            tvBottomCartSummary.setText(count + " টি পণ্য • মোট: " + CurrencyUtils.format(subtotal));
        } else {
            tvTopCartBadge.setVisibility(View.GONE);
            layoutBottomCartBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCartChanged() {
        runOnUiThread(this::updateCartUi);
    }
}
