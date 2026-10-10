package com.smartmess.android.ecommerce;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.smartmess.android.R;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.EcommerceOrder;
import com.smartmess.android.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityEcommerceOrders extends AppCompatActivity {

    private ImageButton btnOrdersBack;
    private View btnOrderNew;
    private SwipeRefreshLayout swipeRefreshOrders;
    private RecyclerView rvUserOrders;
    private ProgressBar pbOrdersLoading;
    private View layoutOrdersEmpty;
    private View btnEmptyGoStore;

    private UserOrderAdapter orderAdapter;
    private SessionManager sessionManager;
    private ApiService apiService;
    private Gson gson;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ecommerce_orders);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService(this);
        gson = new Gson();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadOrders(true);
    }

    private void initViews() {
        btnOrdersBack = findViewById(R.id.btnOrdersBack);
        btnOrderNew = findViewById(R.id.btnOrderNew);
        swipeRefreshOrders = findViewById(R.id.swipeRefreshOrders);
        rvUserOrders = findViewById(R.id.rvUserOrders);
        pbOrdersLoading = findViewById(R.id.pbOrdersLoading);
        layoutOrdersEmpty = findViewById(R.id.layoutOrdersEmpty);
        btnEmptyGoStore = findViewById(R.id.btnEmptyGoStore);
    }

    private void setupRecyclerView() {
        rvUserOrders.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new UserOrderAdapter(this);
        rvUserOrders.setAdapter(orderAdapter);
    }

    private void setupListeners() {
        btnOrdersBack.setOnClickListener(v -> finish());

        btnOrderNew.setOnClickListener(v -> {
            Intent intent = new Intent(ActivityEcommerceOrders.this, ActivityEcommerceStore.class);
            startActivity(intent);
        });

        btnEmptyGoStore.setOnClickListener(v -> {
            Intent intent = new Intent(ActivityEcommerceOrders.this, ActivityEcommerceStore.class);
            startActivity(intent);
            finish();
        });

        swipeRefreshOrders.setOnRefreshListener(() -> loadOrders(false));
    }

    private void loadOrders(boolean showProgress) {
        if (showProgress) {
            pbOrdersLoading.setVisibility(View.VISIBLE);
            layoutOrdersEmpty.setVisibility(View.GONE);
        }

        String phone = sessionManager.getUserPhone();
        Long messId = sessionManager.getMessId() > 0 ? sessionManager.getMessId() : null;

        apiService.getMyEcommerceOrders(phone, messId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pbOrdersLoading.setVisibility(View.GONE);
                swipeRefreshOrders.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().has("data")) {
                    List<EcommerceOrder> ordersList = new ArrayList<>();
                    JsonArray dataArr = response.body().getAsJsonArray("data");
                    for (JsonElement el : dataArr) {
                        try {
                            EcommerceOrder o = gson.fromJson(el, EcommerceOrder.class);
                            ordersList.add(o);
                        } catch (Exception ignored) {}
                    }

                    orderAdapter.setOrders(ordersList);
                    layoutOrdersEmpty.setVisibility(ordersList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    layoutOrdersEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pbOrdersLoading.setVisibility(View.GONE);
                swipeRefreshOrders.setRefreshing(false);
                layoutOrdersEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}
