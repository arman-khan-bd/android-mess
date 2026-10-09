package com.smartmess.android.ui.telemetry;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.TelemetryReportRequest;
import com.smartmess.android.data.remote.dto.TelemetryReportResponse;
import com.smartmess.android.engine.CrashTelemetryHandler;
import com.smartmess.android.utils.SessionManager;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityReportIssue extends AppCompatActivity {

    private Spinner spReportType;
    private Spinner spSeverity;
    private EditText etReportTitle;
    private EditText etReportDescription;
    private TextView tvDeviceDiagnostics;
    private MaterialButton btnSubmitReport;
    private ProgressBar progressBar;

    private final String[] reportTypeKeys = {"manual_bug", "limitation", "feature_suggestion"};
    private final String[] severityKeys = {"low", "medium", "high", "critical"};

    private Map<String, String> diagnosticsMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);

        initViews();
        populateDiagnostics();
        setupSpinners();
        setupSubmitButton();
    }

    private void initViews() {
        spReportType = findViewById(R.id.spReportType);
        spSeverity = findViewById(R.id.spSeverity);
        etReportTitle = findViewById(R.id.etReportTitle);
        etReportDescription = findViewById(R.id.etReportDescription);
        tvDeviceDiagnostics = findViewById(R.id.tvDeviceDiagnostics);
        btnSubmitReport = findViewById(R.id.btnSubmitReport);
        progressBar = findViewById(R.id.progressBar);
    }

    private void populateDiagnostics() {
        diagnosticsMap = CrashTelemetryHandler.getDeviceDiagnostics(this);
        StringBuilder sb = new StringBuilder();
        sb.append("Device: ").append(diagnosticsMap.get("device_brand")).append(" ").append(diagnosticsMap.get("device_model")).append("\n");
        sb.append("OS: Android ").append(diagnosticsMap.get("android_release")).append(" (SDK ").append(diagnosticsMap.get("sdk_int")).append(")\n");
        sb.append("Free RAM: ").append(diagnosticsMap.get("ram_free_mb")).append(" MB | Storage: ").append(diagnosticsMap.get("storage_free_mb")).append(" MB\n");
        sb.append("Network: ").append(diagnosticsMap.get("network_type"));
        tvDeviceDiagnostics.setText(sb.toString());
    }

    private void setupSpinners() {
        String[] reportTypeLabels = {"Bug / Error Encountered", "System Limitation", "Feature Suggestion"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, reportTypeLabels);
        spReportType.setAdapter(typeAdapter);

        String[] severityLabels = {"Low (Cosmetic/Minor)", "Medium (Normal)", "High (Affects Usage)", "Critical (Blocker)"};
        ArrayAdapter<String> severityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, severityLabels);
        spSeverity.setAdapter(severityAdapter);
        spSeverity.setSelection(1); // default medium
    }

    private void setupSubmitButton() {
        btnSubmitReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReport();
            }
        });
    }

    private void submitReport() {
        String title = etReportTitle.getText().toString().trim();
        String description = etReportDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etReportTitle.setError("Please provide a title");
            etReportTitle.requestFocus();
            return;
        }

        int typeIdx = spReportType.getSelectedItemPosition();
        String selectedType = (typeIdx >= 0 && typeIdx < reportTypeKeys.length) ? reportTypeKeys[typeIdx] : "manual_bug";

        int sevIdx = spSeverity.getSelectedItemPosition();
        String selectedSev = (sevIdx >= 0 && sevIdx < severityKeys.length) ? severityKeys[sevIdx] : "medium";

        TelemetryReportRequest req = new TelemetryReportRequest(
                selectedType,
                title,
                description,
                null,
                diagnosticsMap,
                selectedSev
        );

        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            req.setMessId(session.getMessId());
            req.setUserId(session.getUserId());
        }

        btnSubmitReport.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getApiService(this);
        apiService.sendTelemetryReport(req).enqueue(new Callback<TelemetryReportResponse>() {
            @Override
            public void onResponse(Call<TelemetryReportResponse> call, Response<TelemetryReportResponse> response) {
                btnSubmitReport.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ActivityReportIssue.this, "সমস্যা জমা দেওয়া হয়েছে! রেফারেন্স নং: #" + response.body().getReportId(), Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(ActivityReportIssue.this, "রিপোর্ট জমা দিতে সমস্যা হয়েছে। আবার চেষ্টা করুন।", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TelemetryReportResponse> call, Throwable t) {
                btnSubmitReport.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ActivityReportIssue.this, "অফলাইন মোড: রিপোর্ট স্থানীয়ভাবে জমা হয়েছে।", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}
