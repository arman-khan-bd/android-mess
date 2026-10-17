package com.smartmess.android.ui.telemetry;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.smartmess.android.R;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.data.remote.dto.TelemetryReportRequest;
import com.smartmess.android.data.remote.dto.TelemetryReportResponse;
import com.smartmess.android.engine.CrashTelemetryHandler;
import com.smartmess.android.ui.auth.SplashActivity;
import com.smartmess.android.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CrashReportActivity extends AppCompatActivity {

    public static final String EXTRA_CRASH_TITLE = "extra_crash_title";
    public static final String EXTRA_CRASH_MESSAGE = "extra_crash_message";
    public static final String EXTRA_CRASH_STACK = "extra_crash_stack";
    public static final String EXTRA_CRASH_THREAD = "extra_crash_thread";
    public static final String EXTRA_CRASH_TIME = "extra_crash_time";
    public static final String EXTRA_AUTO_SHOW_DIALOG = "extra_auto_show_dialog";

    private MaterialToolbar toolbarCrashReport;
    private TextView tvCrashTimestamp;
    private TextView tvReportStatusBadge;
    private TextView tvCrashTitle;
    private TextView tvCrashThread;
    private TextView tvDeviceSpecs;
    private EditText etUserNote;
    private TextView tvStackTrace;
    private HorizontalScrollView layoutStackTrace;
    private MaterialButton btnCopyStackTrace;
    private MaterialButton btnToggleStackTrace;
    private ProgressBar progressBarReport;
    private MaterialButton btnSendReport;
    private MaterialButton btnRestartApp;
    private MaterialButton btnCloseApp;

    private String crashTitle;
    private String crashMessage;
    private String crashStack;
    private String crashThread;
    private long crashTime;
    private boolean isCrashSent = false;
    private Map<String, String> diagnosticsMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_report);

        initViews();
        loadCrashData();
        populateDiagnostics();
        setupListeners();

        boolean autoShowDialog = getIntent().getBooleanExtra(EXTRA_AUTO_SHOW_DIALOG, false);
        if (autoShowDialog && !isCrashSent) {
            showSendReportDialog();
        }
    }

    private void initViews() {
        toolbarCrashReport = findViewById(R.id.toolbarCrashReport);
        tvCrashTimestamp = findViewById(R.id.tvCrashTimestamp);
        tvReportStatusBadge = findViewById(R.id.tvReportStatusBadge);
        tvCrashTitle = findViewById(R.id.tvCrashTitle);
        tvCrashThread = findViewById(R.id.tvCrashThread);
        tvDeviceSpecs = findViewById(R.id.tvDeviceSpecs);
        etUserNote = findViewById(R.id.etUserNote);
        tvStackTrace = findViewById(R.id.tvStackTrace);
        layoutStackTrace = findViewById(R.id.layoutStackTrace);
        btnCopyStackTrace = findViewById(R.id.btnCopyStackTrace);
        btnToggleStackTrace = findViewById(R.id.btnToggleStackTrace);
        progressBarReport = findViewById(R.id.progressBarReport);
        btnSendReport = findViewById(R.id.btnSendReport);
        btnRestartApp = findViewById(R.id.btnRestartApp);
        btnCloseApp = findViewById(R.id.btnCloseApp);
    }

    private void loadCrashData() {
        Intent intent = getIntent();
        crashTitle = intent.getStringExtra(EXTRA_CRASH_TITLE);
        crashMessage = intent.getStringExtra(EXTRA_CRASH_MESSAGE);
        crashStack = intent.getStringExtra(EXTRA_CRASH_STACK);
        crashThread = intent.getStringExtra(EXTRA_CRASH_THREAD);
        crashTime = intent.getLongExtra(EXTRA_CRASH_TIME, 0);

        // Fallback to SharedPreferences if extras were empty (e.g. opened manually from settings)
        if (crashTitle == null || crashTitle.isEmpty()) {
            SharedPreferences prefs = CrashTelemetryHandler.getCrashPrefs(this);
            if (prefs.getBoolean(CrashTelemetryHandler.KEY_HAS_CRASH, false)) {
                crashTitle = prefs.getString(CrashTelemetryHandler.KEY_CRASH_TITLE, "Uncaught Application Exception");
                crashMessage = prefs.getString(CrashTelemetryHandler.KEY_CRASH_MESSAGE, "");
                crashStack = prefs.getString(CrashTelemetryHandler.KEY_CRASH_STACK, "No stack trace recorded.");
                crashThread = prefs.getString(CrashTelemetryHandler.KEY_CRASH_THREAD, "main");
                crashTime = prefs.getLong(CrashTelemetryHandler.KEY_CRASH_TIME, System.currentTimeMillis());
                isCrashSent = prefs.getBoolean(CrashTelemetryHandler.KEY_CRASH_SENT, false);
            } else {
                crashTitle = "সিস্টেম স্বাভাবিক - কোনো সক্রিয় ক্র্যাশ শনাক্ত হয়নি";
                crashMessage = "SmartMess ডায়াগনস্টিক ইঞ্জিন সক্রিয় রয়েছে।";
                crashStack = "Active Process Diagnostics OK.\nThread: " + Thread.currentThread().getName();
                crashThread = Thread.currentThread().getName();
                crashTime = System.currentTimeMillis();
                isCrashSent = true;
            }
        }

        // Render Loaded Data
        tvCrashTitle.setText(crashTitle);
        tvCrashThread.setText("Crashing Thread: " + (crashThread != null ? crashThread : "main"));
        tvStackTrace.setText(crashStack != null ? crashStack : "No stack trace available.");

        if (crashTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            tvCrashTimestamp.setText("সংঘটিত সময়: " + sdf.format(new Date(crashTime)));
        } else {
            tvCrashTimestamp.setText("লাইভ সেশন ডায়াগনস্টিক");
        }

        updateStatusBadge(isCrashSent);
    }

    private void updateStatusBadge(boolean sent) {
        if (sent) {
            tvReportStatusBadge.setText("সার্ভারে পাঠানো হয়েছে");
            tvReportStatusBadge.setBackgroundResource(R.drawable.badge_credit);
            tvReportStatusBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.credit_green));
            btnSendReport.setText("পুনরায় রিপোর্ট পাঠান");
        } else {
            tvReportStatusBadge.setText("পেন্ডিং");
            tvReportStatusBadge.setBackgroundResource(R.drawable.badge_due);
            tvReportStatusBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.error_red));
            btnSendReport.setText("সার্ভারে ক্র্যাশ রিপোর্ট পাঠান");
        }
    }

    private void populateDiagnostics() {
        diagnosticsMap = CrashTelemetryHandler.getDeviceDiagnostics(this);
        StringBuilder sb = new StringBuilder();
        sb.append("ডিভাইস মডেল: ").append(diagnosticsMap.get("device_brand")).append(" ").append(diagnosticsMap.get("device_model")).append("\n");
        sb.append("অ্যান্ড্রয়েড ভার্সন: Android ").append(diagnosticsMap.get("android_release")).append(" (SDK ").append(diagnosticsMap.get("sdk_int")).append(")\n");
        sb.append("উপলব্ধ মেমোরি (RAM): ").append(diagnosticsMap.get("ram_free_mb")).append(" MB ফ্রি\n");
        sb.append("ফ্রি স্টোরেজ: ").append(diagnosticsMap.get("storage_free_mb")).append(" MB\n");
        sb.append("নেটওয়ার্ক সংযোগ: ").append(diagnosticsMap.get("network_type"));
        tvDeviceSpecs.setText(sb.toString());
    }

    private void setupListeners() {
        toolbarCrashReport.setNavigationOnClickListener(v -> finish());

        btnToggleStackTrace.setOnClickListener(v -> {
            if (layoutStackTrace.getVisibility() == View.VISIBLE) {
                layoutStackTrace.setVisibility(View.GONE);
                btnToggleStackTrace.setText("প্রদর্শন");
            } else {
                layoutStackTrace.setVisibility(View.VISIBLE);
                btnToggleStackTrace.setText("লুকান");
            }
        });

        btnCopyStackTrace.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && crashStack != null) {
                ClipData clip = ClipData.newPlainText("SmartMess Crash Stack", crashStack);
                cm.setPrimaryClip(clip);
                Toast.makeText(this, "স্ট্যাক ট্রেস ক্লিপবোর্ডে কপি করা হয়েছে।", Toast.LENGTH_SHORT).show();
            }
        });

        btnSendReport.setOnClickListener(v -> {
            String note = etUserNote.getText().toString().trim();
            sendReportToServer(note, null, null);
        });

        btnRestartApp.setOnClickListener(v -> restartSmartMess());

        btnCloseApp.setOnClickListener(v -> {
            finishAffinity();
            System.exit(0);
        });
    }

    /**
     * Display the auto-prompt dialog requesting permission to submit the crash report
     * along with an optional user note.
     */
    public void showSendReportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_send_crash_report, null);
        builder.setView(dialogView);
        builder.setCancelable(true);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvSummary = dialogView.findViewById(R.id.tvDialogErrorSummary);
        EditText etNote = dialogView.findViewById(R.id.etDialogUserNote);
        ProgressBar pb = dialogView.findViewById(R.id.dialogProgressBar);
        MaterialButton btnSend = dialogView.findViewById(R.id.btnDialogSendReport);
        MaterialButton btnViewDetails = dialogView.findViewById(R.id.btnDialogViewDetails);
        MaterialButton btnRestart = dialogView.findViewById(R.id.btnDialogRestartApp);

        tvSummary.setText(crashTitle != null ? crashTitle : "Application Exception");

        btnSend.setOnClickListener(v -> {
            String userNote = etNote.getText().toString().trim();
            sendReportToServer(userNote, dialog, pb);
        });

        btnViewDetails.setOnClickListener(v -> dialog.dismiss());

        btnRestart.setOnClickListener(v -> {
            dialog.dismiss();
            restartSmartMess();
        });

        dialog.show();
    }

    private void sendReportToServer(String userNote, AlertDialog dialog, ProgressBar dialogPb) {
        if (progressBarReport != null) progressBarReport.setVisibility(View.VISIBLE);
        if (btnSendReport != null) btnSendReport.setEnabled(false);
        if (dialogPb != null) dialogPb.setVisibility(View.VISIBLE);

        StringBuilder descBuilder = new StringBuilder();
        if (userNote != null && !userNote.isEmpty()) {
            descBuilder.append("User Note: ").append(userNote).append("\n\n");
        }
        descBuilder.append("Auto-detected crash on thread: ").append(crashThread != null ? crashThread : "main");

        TelemetryReportRequest request = new TelemetryReportRequest(
                "crash_auto",
                crashTitle != null ? crashTitle : "Uncaught Exception",
                descBuilder.toString(),
                crashStack,
                diagnosticsMap != null ? diagnosticsMap : CrashTelemetryHandler.getDeviceDiagnostics(this),
                "critical"
        );

        try {
            SessionManager session = new SessionManager(this);
            if (session.isLoggedIn()) {
                request.setMessId(session.getMessId());
                request.setUserId(session.getUserId());
            }
        } catch (Throwable ignored) {}

        ApiService apiService = ApiClient.getApiService(this);
        apiService.sendTelemetryReport(request).enqueue(new Callback<TelemetryReportResponse>() {
            @Override
            public void onResponse(Call<TelemetryReportResponse> call, Response<TelemetryReportResponse> response) {
                if (progressBarReport != null) progressBarReport.setVisibility(View.GONE);
                if (btnSendReport != null) btnSendReport.setEnabled(true);
                if (dialogPb != null) dialogPb.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    long reportId = response.body().getReportId();
                    CrashTelemetryHandler.markCrashSent(CrashReportActivity.this, reportId);
                    isCrashSent = true;
                    updateStatusBadge(true);

                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }

                    showSuccessAlert(reportId);
                } else {
                    Toast.makeText(CrashReportActivity.this, "রিপোর্ট আপলোড করতে সমস্যা হয়েছে। আবার চেষ্টা করুন।", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TelemetryReportResponse> call, Throwable t) {
                if (progressBarReport != null) progressBarReport.setVisibility(View.GONE);
                if (btnSendReport != null) btnSendReport.setEnabled(true);
                if (dialogPb != null) dialogPb.setVisibility(View.GONE);

                Toast.makeText(CrashReportActivity.this, "ইন্টারনেট ত্রুটি: রিপোর্টটি ডিভাইসে সংরক্ষিত আছে।", Toast.LENGTH_LONG).show();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        });
    }

    private void showSuccessAlert(long reportId) {
        new AlertDialog.Builder(this)
                .setTitle("ধন্যবাদ!")
                .setMessage("ক্র্যাশ রিপোর্ট সফলভাবে ডেভেলপার সার্ভারে জমা হয়েছে (রেফারেন্স নং: #" + reportId + ")। আমরা দ্রুত সমস্যাটি সমাধান করব।")
                .setPositiveButton("অ্যাপ পুনরায় চালু করুন", (d, which) -> restartSmartMess())
                .setNegativeButton("বন্ধ করুন", null)
                .show();
    }

    private void restartSmartMess() {
        Intent restartIntent = new Intent(this, SplashActivity.class);
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(restartIntent);
        finish();
    }
}
