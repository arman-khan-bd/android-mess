package com.smartmess.android.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.SaasPlanDao;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.MessSettingsResponse;
import com.smartmess.android.engine.PlanGateManager;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.SaasPlan;
import com.smartmess.android.ui.auth.LoginActivity;
import com.smartmess.android.ui.dialogs.UpgradeProBottomSheet;
import com.smartmess.android.ui.support.ActivitySupportTickets;
import com.smartmess.android.ui.telemetry.ActivityReportIssue;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private EditText etCutoffTime;
    private EditText etPerSmsCost;
    private EditText etTargetMealBudget;
    private MaterialButton btnSaveMessSettings;

    private TextView tvPlanTitle;
    private TextView tvPlanFeaturesDescription;
    private MaterialButton btnUpgradePlan;
    private MaterialButton btnSupportTickets;
    private MaterialButton btnReportIssue;
    private MaterialButton btnLogout;

    private MessDao messDao;
    private SaasPlanDao planDao;
    private SessionManager sessionManager;
    private PlanGateManager planGateManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etCutoffTime = findViewById(R.id.etCutoffTime);
        etPerSmsCost = findViewById(R.id.etPerSmsCost);
        etTargetMealBudget = findViewById(R.id.etTargetMealBudget);
        btnSaveMessSettings = findViewById(R.id.btnSaveMessSettings);
        tvPlanTitle = findViewById(R.id.tvPlanTitle);
        tvPlanFeaturesDescription = findViewById(R.id.tvPlanFeaturesDescription);
        btnUpgradePlan = findViewById(R.id.btnUpgradePlan);
        btnSupportTickets = findViewById(R.id.btnSupportTickets);
        btnReportIssue = findViewById(R.id.btnReportIssue);
        btnLogout = findViewById(R.id.btnLogout);

        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        messDao = new MessDao(helper);
        planDao = new SaasPlanDao(helper);
        sessionManager = new SessionManager(this);
        planGateManager = new PlanGateManager(this);
        apiService = ApiClient.getApiService(this);

        loadSettings();

        btnSaveMessSettings.setOnClickListener(v -> saveSettings());
        btnUpgradePlan.setOnClickListener(v -> showDynamicPlansSheet());
        if (btnSupportTickets != null) {
            btnSupportTickets.setOnClickListener(v ->
                    startActivity(new Intent(this, ActivitySupportTickets.class)));
        }
        if (btnReportIssue != null) {
            btnReportIssue.setOnClickListener(v ->
                    startActivity(new Intent(this, ActivityReportIssue.class)));
        }
        btnLogout.setOnClickListener(v -> performLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
    }

    private void loadSettings() {
        long messId = sessionManager.getMessId();
        Mess mess = messDao.getById(messId);
        if (mess != null) {
            etCutoffTime.setText(mess.getMealCutoffTime() != null ? mess.getMealCutoffTime() : "22:00:00");
            etPerSmsCost.setText(String.valueOf(mess.getPerSmsCost()));
            etTargetMealBudget.setText(String.format(java.util.Locale.US, "%.2f", mess.getTargetMealBudget()));
        } else {
            etTargetMealBudget.setText("70.00");
        }

        SaasPlan activePlan = planGateManager.getActivePlanForMess(messId);
        tvPlanTitle.setText("Plan: " + activePlan.getName());
        StringBuilder sb = new StringBuilder();
        for (String feature : activePlan.getFeatureList()) {
            sb.append("• ").append(feature).append("\n");
        }
        tvPlanFeaturesDescription.setText(sb.toString().trim());
    }

    private void saveSettings() {
        long messId = sessionManager.getMessId();
        String cutoff = etCutoffTime.getText().toString().trim();
        String smsCostStr = etPerSmsCost.getText().toString().trim();
        String budgetStr = etTargetMealBudget.getText().toString().trim();

        if (cutoff.isEmpty()) cutoff = "22:00:00";
        double cost = 0.50;
        try {
            cost = Double.parseDouble(smsCostStr);
        } catch (NumberFormatException ignored) {}

        double budget = 70.00;
        try {
            budget = Double.parseDouble(budgetStr);
        } catch (NumberFormatException ignored) {}

        if (budget <= 0) {
            budget = 70.00;
        }

        // 1. Update SQLite locally
        messDao.updateCutoffTime(messId, cutoff);
        messDao.updateSmsCost(messId, cost);
        messDao.updateTargetBudget(messId, budget);

        // 2. Synchronize to Backend Cloud API
        Map<String, Object> body = new HashMap<>();
        body.put("meal_cutoff_time", cutoff);
        body.put("per_sms_cost", cost);
        body.put("target_meal_budget", budget);

        apiService.updateMessSettings(body).enqueue(new Callback<MessSettingsResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessSettingsResponse> call, @NonNull Response<MessSettingsResponse> response) {
                // Background sync succeeded
            }

            @Override
            public void onFailure(@NonNull Call<MessSettingsResponse> call, @NonNull Throwable t) {
                // Will sync upon next batch push
            }
        });

        Toast.makeText(this, "Target meal budget (৳" + budget + ") and mess rules saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showDynamicPlansSheet() {
        UpgradeProBottomSheet sheet = UpgradeProBottomSheet.newInstance(
                "Subscription Plans",
                "Choose a live subscription plan to upgrade your mess with zero downtime"
        );
        sheet.show(getSupportFragmentManager(), UpgradeProBottomSheet.TAG);
    }

    private void performLogout() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
