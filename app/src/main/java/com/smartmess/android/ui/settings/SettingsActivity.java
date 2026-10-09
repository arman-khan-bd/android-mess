package com.smartmess.android.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.SaasPlanDao;
import com.smartmess.android.engine.PlanGateManager;
import com.smartmess.android.model.Mess;
import com.smartmess.android.model.SaasPlan;
import com.smartmess.android.ui.auth.LoginActivity;
import com.smartmess.android.ui.support.ActivitySupportTickets;
import com.smartmess.android.ui.telemetry.ActivityReportIssue;
import com.smartmess.android.utils.SessionManager;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private EditText etCutoffTime;
    private EditText etPerSmsCost;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etCutoffTime = findViewById(R.id.etCutoffTime);
        etPerSmsCost = findViewById(R.id.etPerSmsCost);
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

        loadSettings();

        btnSaveMessSettings.setOnClickListener(v -> saveSettings());
        btnUpgradePlan.setOnClickListener(v -> showPlanPicker());
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

    private void loadSettings() {
        long messId = sessionManager.getMessId();
        Mess mess = messDao.getById(messId);
        if (mess != null) {
            etCutoffTime.setText(mess.getMealCutoffTime() != null ? mess.getMealCutoffTime() : "22:00:00");
            etPerSmsCost.setText(String.valueOf(mess.getPerSmsCost()));
        }

        SaasPlan activePlan = planGateManager.getActivePlanForMess(messId);
        tvPlanTitle.setText("Plan: " + activePlan.getName());
        String features = "• Max Members: " + activePlan.getMaxMembers() + "\n"
                + "• Device SIM SMS: " + (activePlan.isSmsSim() ? "Enabled" : "Disabled") + "\n"
                + "• Cloud SMS Gateway: " + (activePlan.isSmsCloud() ? "Enabled" : "Enterprise Only") + "\n"
                + "• Receipt OCR Scanner: " + (activePlan.isOcrReceipt() ? "Enabled" : "Enterprise Only") + "\n"
                + "• Branded PDF Export: " + (activePlan.isPdfBranding() ? "Enabled" : "Enterprise Only") + "\n"
                + "• Ad-Free Experience: " + (activePlan.isAdFree() ? "Yes" : "No");
        tvPlanFeaturesDescription.setText(features);
    }

    private void saveSettings() {
        long messId = sessionManager.getMessId();
        String cutoff = etCutoffTime.getText().toString().trim();
        String smsCostStr = etPerSmsCost.getText().toString().trim();

        if (cutoff.isEmpty()) cutoff = "22:00:00";
        double cost = 0.50;
        try {
            cost = Double.parseDouble(smsCostStr);
        } catch (NumberFormatException ignored) {}

        messDao.updateCutoffTime(messId, cutoff);
        messDao.updateSmsCost(messId, cost);

        Toast.makeText(this, "Mess automation rules updated successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showPlanPicker() {
        List<SaasPlan> plans = planDao.getAllActivePlans();
        if (plans.isEmpty()) {
            Toast.makeText(this, "No other plans currently available offline", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] planNames = new String[plans.size()];
        for (int i = 0; i < plans.size(); i++) {
            planNames[i] = plans.get(i).getName() + " (৳ " + plans.get(i).getPrice() + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Select SaaS Subscription Plan")
                .setItems(planNames, (dialog, which) -> {
                    SaasPlan selected = plans.get(which);
                    Mess mess = messDao.getById(sessionManager.getMessId());
                    if (mess != null) {
                        mess.setCurrentPlanId(selected.getId());
                        messDao.insertOrUpdate(mess);
                        Toast.makeText(this, "Switched to " + selected.getName() + "!", Toast.LENGTH_LONG).show();
                        loadSettings();
                    }
                })
                .show();
    }

    private void performLogout() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
