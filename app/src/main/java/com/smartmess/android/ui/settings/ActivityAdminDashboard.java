package com.smartmess.android.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.AppNotificationDao;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.remote.ApiClient;
import com.smartmess.android.data.remote.ApiService;
import com.smartmess.android.model.AppNotification;
import com.smartmess.android.model.Mess;
import com.smartmess.android.ui.members.ActivityMemberManage;
import com.smartmess.android.ui.reports.SummaryReportActivity;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.SessionManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Manager-Only Administrative Hub & Mess Operational Data Reset Dashboard.
 * Features comprehensive metrics overview and a protected Danger Zone
 * with a mandatory writing confirmation dialog ("RESET").
 */
public class ActivityAdminDashboard extends AppCompatActivity {

    private static final String TAG = "ActivityAdminDashboard";

    private MaterialToolbar toolbarAdminDashboard;
    private TextView tvAdminMessName;
    private TextView tvAdminManagerName;
    private TextView tvAdminMessCode;
    private TextView tvAdminBillingCycle;
    private ImageButton btnCopyMessCode;

    private TextView tvStatMembers;
    private TextView tvStatMeals;
    private TextView tvStatBazar;
    private TextView tvStatDeposits;

    private View btnAdminManageMembers;
    private View btnAdminMessSettings;
    private View btnAdminReports;
    private MaterialButton btnResetMessData;

    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;
    private MessDao messDao;
    private UserDao userDao;
    private ApiService apiService;

    private String currentInviteCode = "";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Security check: only mess manager or admin can access
        if (!sessionManager.isManager() && !"admin".equalsIgnoreCase(sessionManager.getUserRole())) {
            Toast.makeText(this, "শুধুমাত্র মেস ম্যানেজার এই ড্যাশবোর্ড ব্যবহার করতে পারবেন", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_dashboard);

        databaseHelper = DatabaseHelper.getInstance(this);
        messDao = new MessDao(databaseHelper);
        userDao = new UserDao(databaseHelper);
        apiService = ApiClient.getApiService(this);

        initViews();
        setupListeners();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initViews() {
        toolbarAdminDashboard = findViewById(R.id.toolbarAdminDashboard);
        tvAdminMessName = findViewById(R.id.tvAdminMessName);
        tvAdminManagerName = findViewById(R.id.tvAdminManagerName);
        tvAdminMessCode = findViewById(R.id.tvAdminMessCode);
        tvAdminBillingCycle = findViewById(R.id.tvAdminBillingCycle);
        btnCopyMessCode = findViewById(R.id.btnCopyMessCode);

        tvStatMembers = findViewById(R.id.tvStatMembers);
        tvStatMeals = findViewById(R.id.tvStatMeals);
        tvStatBazar = findViewById(R.id.tvStatBazar);
        tvStatDeposits = findViewById(R.id.tvStatDeposits);

        btnAdminManageMembers = findViewById(R.id.btnAdminManageMembers);
        btnAdminMessSettings = findViewById(R.id.btnAdminMessSettings);
        btnAdminReports = findViewById(R.id.btnAdminReports);
        btnResetMessData = findViewById(R.id.btnResetMessData);
    }

    private void setupListeners() {
        toolbarAdminDashboard.setNavigationOnClickListener(v -> finish());

        btnCopyMessCode.setOnClickListener(v -> {
            if (currentInviteCode != null && !currentInviteCode.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Mess Invite Code", currentInviteCode);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "মেস ইনভাইট কোড কপি করা হয়েছে: " + currentInviteCode, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAdminManageMembers.setOnClickListener(v ->
                startActivity(new Intent(this, ActivityMemberManage.class)));

        btnAdminMessSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        btnAdminReports.setOnClickListener(v ->
                startActivity(new Intent(this, SummaryReportActivity.class)));

        btnResetMessData.setOnClickListener(v -> showWritingConfirmResetDialog());
    }

    private void loadDashboardData() {
        long messId = sessionManager.getMessId();
        Mess mess = messDao.getById(messId);

        if (mess != null) {
            tvAdminMessName.setText(mess.getName() != null ? mess.getName() : "স্মার্টমেস হোস্টেল");
            currentInviteCode = mess.getInviteCode() != null ? mess.getInviteCode() : "N/A";
            tvAdminMessCode.setText(currentInviteCode);
            int startDay = mess.getCycleStartDay();
            tvAdminBillingCycle.setText("প্রতি মাসের " + startDay + " তারিখ থেকে");
        } else {
            tvAdminMessName.setText("স্মার্টমেস");
            tvAdminMessCode.setText("N/A");
            tvAdminBillingCycle.setText("প্রতি মাসের ১ তারিখ থেকে");
        }

        tvAdminManagerName.setText(sessionManager.getUserName());

        // Read SQLite counts and sums
        Executors.newSingleThreadExecutor().execute(() -> {
            int memberCount = userDao.countActiveMembers(messId);

            double totalMeals = 0.0;
            double totalBazar = 0.0;
            double totalDeposits = 0.0;

            try {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();

                Cursor cMeals = db.rawQuery(
                        "SELECT SUM(breakfast_count + lunch_count + dinner_count + guest_meal_count) FROM meals WHERE mess_id = ?",
                        new String[]{String.valueOf(messId)});
                if (cMeals != null) {
                    if (cMeals.moveToFirst()) {
                        totalMeals = cMeals.getDouble(0);
                    }
                    cMeals.close();
                }

                Cursor cExp = db.rawQuery(
                        "SELECT SUM(amount) FROM expenses WHERE mess_id = ?",
                        new String[]{String.valueOf(messId)});
                if (cExp != null) {
                    if (cExp.moveToFirst()) {
                        totalBazar = cExp.getDouble(0);
                    }
                    cExp.close();
                }

                Cursor cDep = db.rawQuery(
                        "SELECT SUM(amount) FROM deposits WHERE mess_id = ?",
                        new String[]{String.valueOf(messId)});
                if (cDep != null) {
                    if (cDep.moveToFirst()) {
                        totalDeposits = cDep.getDouble(0);
                    }
                    cDep.close();
                }

            } catch (Exception e) {
                Log.e(TAG, "Error calculating dashboard stats: " + e.getMessage(), e);
            }

            final int finalMembers = memberCount;
            final double finalMeals = totalMeals;
            final double finalBazar = totalBazar;
            final double finalDeposits = totalDeposits;

            mainHandler.post(() -> {
                tvStatMembers.setText(finalMembers + " জন");
                tvStatMeals.setText(String.format(Locale.US, "%.1f", finalMeals) + " টি");
                tvStatBazar.setText(CurrencyUtils.format(finalBazar));
                tvStatDeposits.setText(CurrencyUtils.format(finalDeposits));
            });
        });
    }

    /**
     * Writing Confirmation Dialog:
     * Requires the user to type "RESET" into an input field before the destructive button is enabled.
     */
    private void showWritingConfirmResetDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_confirm_reset, null);

        TextInputEditText etConfirmResetInput = dialogView.findViewById(R.id.etConfirmResetInput);
        MaterialButton btnConfirmResetSubmit = dialogView.findViewById(R.id.btnConfirmResetSubmit);
        MaterialButton btnCancelReset = dialogView.findViewById(R.id.btnCancelReset);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Enforce exact typing of "RESET"
        etConfirmResetInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s != null ? s.toString().trim() : "";
                boolean matches = "RESET".equals(input);
                btnConfirmResetSubmit.setEnabled(matches);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCancelReset.setOnClickListener(v -> dialog.dismiss());

        btnConfirmResetSubmit.setOnClickListener(v -> {
            dialog.dismiss();
            executeMessDataReset();
        });

        dialog.show();
    }

    /**
     * Executes the atomic mess operational data wipe:
     * 1. Local SQLite transaction clearing meals, expenses, deposits, sessions, consumption logs.
     * 2. Local audit notification insertion.
     * 3. Remote API call to backend /mess/reset.
     */
    private void executeMessDataReset() {
        // Show progress dialog
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(this, 24), dpToPx(this, 16), dpToPx(this, 24), dpToPx(this, 16));

        TextView tvStatus = new TextView(this);
        tvStatus.setText("মেসের সমস্ত মিল, বাজার ও জমা ডাটা রিসেট করা হচ্ছে...\nঅনুগ্রহ করে অপেক্ষা করুন");
        tvStatus.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
        tvStatus.setTextSize(13);
        layout.addView(tvStatus);

        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        android.widget.LinearLayout.LayoutParams pbLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(this, 16));
        pbLp.topMargin = dpToPx(this, 12);
        progressBar.setLayoutParams(pbLp);
        layout.addView(progressBar);

        AlertDialog loadingDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("ডাটা রিসেট চলমান")
                .setView(layout)
                .setCancelable(false)
                .create();

        loadingDialog.show();

        final long messId = sessionManager.getMessId();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. Clear local operational database tables
                databaseHelper.resetMessOperationalData(messId);

                // 2. Insert audit notification log
                try {
                    AppNotificationDao notifDao = new AppNotificationDao(databaseHelper);
                    AppNotification n = new AppNotification();
                    n.setMessId(messId);
                    n.setUserId(sessionManager.getUserId());
                    n.setTitle("⚠️ মেসের হিসাব রিসেট করা হয়েছে");
                    n.setMessage("ম্যানেজার (" + sessionManager.getUserName() + ") মেসের পূর্ববর্তী সমস্ত মিল, বাজার ও জমা রেকর্ড রিসেট করেছেন। নতুন হিসাব শুরু হয়েছে।");
                    n.setType("mess_reset");
                    notifDao.insert(n);
                } catch (Exception notifEx) {
                    Log.w(TAG, "Notification insert warning: " + notifEx.getMessage());
                }

                // 3. Remote Server Reset call
                Map<String, String> body = new HashMap<>();
                body.put("confirm_code", "RESET");
                apiService.resetMessData(body).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Log.d(TAG, "Remote resetMessData HTTP code: " + response.code());
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.w(TAG, "Remote reset delayed (offline or network): " + t.getMessage());
                    }
                });

                mainHandler.post(() -> {
                    if (loadingDialog.isShowing()) loadingDialog.dismiss();
                    Toast.makeText(ActivityAdminDashboard.this,
                            "মেসের সমস্ত মিল, বাজার ও জমার ডাটা সফলভাবে রিসেট করা হয়েছে।",
                            Toast.LENGTH_LONG).show();
                    loadDashboardData();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error resetting mess data: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    if (loadingDialog.isShowing()) loadingDialog.dismiss();
                    Toast.makeText(ActivityAdminDashboard.this,
                            "রিসেট করতে ব্যর্থ হয়েছে: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    loadDashboardData();
                });
            }
        });
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
