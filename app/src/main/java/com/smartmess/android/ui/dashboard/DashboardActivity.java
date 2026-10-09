package com.smartmess.android.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.smartmess.android.R;
import com.smartmess.android.data.local.DatabaseHelper;
import com.smartmess.android.data.local.dao.MessDao;
import com.smartmess.android.data.local.dao.UserDao;
import com.smartmess.android.data.sync.SyncManager;
import com.smartmess.android.engine.AccountingEngine;
import com.smartmess.android.engine.CalculationModels.CycleSummary;
import com.smartmess.android.engine.CalculationModels.MemberBalanceSheet;
import com.smartmess.android.model.Mess;
import com.smartmess.android.ui.auth.LoginActivity;
import com.smartmess.android.ui.deposits.DepositListActivity;
import com.smartmess.android.ui.expenses.ExpenseListActivity;
import com.smartmess.android.ui.meals.DailyMealToggleActivity;
import com.smartmess.android.ui.meals.MealListActivity;
import com.smartmess.android.ui.members.MemberListActivity;
import com.smartmess.android.ui.reports.SummaryReportActivity;
import com.smartmess.android.ui.settings.SettingsActivity;
import com.smartmess.android.ui.sms.DueReminderActivity;
import com.smartmess.android.ui.sms.SmsBroadcastActivity;
import com.smartmess.android.ui.support.ActivitySupportTickets;
import com.smartmess.android.ui.telemetry.ActivityReportIssue;
import com.smartmess.android.utils.CurrencyUtils;
import com.smartmess.android.utils.DateTimeUtils;
import com.smartmess.android.utils.SessionManager;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvMessName;
    private TextView tvUserRole;
    private Chip chipSyncStatus;
    private TextView tvLiveMealRate;
    private TextView tvMessCashInHand;
    private TextView tvTotalMessMeals;
    private TextView tvCycleDates;

    private TextView tvRawMealCost;
    private TextView tvSharedFoodCost;
    private TextView tvUtilityCost;

    private TextView tvMyBalance;
    private TextView tvBalanceBadge;
    private TextView tvMyMeals;
    private TextView tvMyDeposits;
    private TextView tvMyTotalCost;
    private TextView tvActiveMemberCount;
    private TextView tvCutoffNotice;
    private SwipeRefreshLayout swipeRefresh;

    private SessionManager sessionManager;
    private MessDao messDao;
    private UserDao userDao;
    private AccountingEngine accountingEngine;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);
        DatabaseHelper helper = DatabaseHelper.getInstance(this);
        messDao = new MessDao(helper);
        userDao = new UserDao(helper);
        accountingEngine = new AccountingEngine(this);
        syncManager = new SyncManager(this);

        initViews();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initViews() {
        tvMessName = findViewById(R.id.tvMessName);
        tvUserRole = findViewById(R.id.tvUserRole);
        chipSyncStatus = findViewById(R.id.chipSyncStatus);
        tvLiveMealRate = findViewById(R.id.tvLiveMealRate);
        tvMessCashInHand = findViewById(R.id.tvMessCashInHand);
        tvTotalMessMeals = findViewById(R.id.tvTotalMessMeals);
        tvCycleDates = findViewById(R.id.tvCycleDates);

        tvRawMealCost = findViewById(R.id.tvRawMealCost);
        tvSharedFoodCost = findViewById(R.id.tvSharedFoodCost);
        tvUtilityCost = findViewById(R.id.tvUtilityCost);

        tvMyBalance = findViewById(R.id.tvMyBalance);
        tvBalanceBadge = findViewById(R.id.tvBalanceBadge);
        tvMyMeals = findViewById(R.id.tvMyMeals);
        tvMyDeposits = findViewById(R.id.tvMyDeposits);
        tvMyTotalCost = findViewById(R.id.tvMyTotalCost);
        tvActiveMemberCount = findViewById(R.id.tvActiveMemberCount);
        tvCutoffNotice = findViewById(R.id.tvCutoffNotice);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        swipeRefresh.setOnRefreshListener(this::performSync);
        chipSyncStatus.setOnClickListener(v -> performSync());
    }

    private void setupClickListeners() {
        findViewById(R.id.cardMeals).setOnClickListener(v ->
                startActivity(new Intent(this, MealListActivity.class)));

        findViewById(R.id.cardDailyToggle).setOnClickListener(v ->
                startActivity(new Intent(this, DailyMealToggleActivity.class)));

        findViewById(R.id.cardExpenses).setOnClickListener(v ->
                startActivity(new Intent(this, ExpenseListActivity.class)));

        findViewById(R.id.cardDeposits).setOnClickListener(v ->
                startActivity(new Intent(this, DepositListActivity.class)));

        findViewById(R.id.cardMembers).setOnClickListener(v ->
                startActivity(new Intent(this, MemberListActivity.class)));

        findViewById(R.id.cardSms).setOnClickListener(v -> showSmsOptionsDialog());

        findViewById(R.id.cardReports).setOnClickListener(v ->
                startActivity(new Intent(this, SummaryReportActivity.class)));

        findViewById(R.id.cardSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void loadDashboardData() {
        long messId = sessionManager.getMessId();
        Mess mess = messDao.getById(messId);

        tvMessName.setText(mess != null ? mess.getName() : sessionManager.getMessName());
        String roleStr = sessionManager.getUserRole().toUpperCase() + " • " + sessionManager.getUserName();
        tvUserRole.setText(roleStr);

        int startDay = mess != null ? mess.getCycleStartDay() : 1;
        String startDate = DateTimeUtils.currentMonthStart(startDay);
        String endDate = DateTimeUtils.currentMonthEnd(startDay);
        tvCycleDates.setText(DateTimeUtils.formatDisplayDate(startDate) + " - " + DateTimeUtils.formatDisplayDate(endDate));

        if (mess != null && mess.getMealCutoffTime() != null) {
            tvCutoffNotice.setText("Cut-off at " + mess.getMealCutoffTime());
        }

        // Run accounting engine for active cycle
        CycleSummary summary = accountingEngine.calculateCycleSummary(messId, startDate, endDate);

        tvLiveMealRate.setText(CurrencyUtils.format(summary.getMealRate()));
        tvMessCashInHand.setText(CurrencyUtils.format(summary.getMessCashInHand()));
        tvTotalMessMeals.setText(summary.getTotalMessMeals() + " Meals");

        // Dual Pool Expenses
        tvRawMealCost.setText(CurrencyUtils.format(summary.getTotalRawMealExpense()));
        tvSharedFoodCost.setText(CurrencyUtils.format(summary.getTotalSharedFoodExpense()));
        tvUtilityCost.setText(CurrencyUtils.format(summary.getTotalUtilityAssetExpense() + summary.getTotalSmsChargeExpense()));

        // Active members count
        int activeCount = userDao.countActiveMembers(messId);
        tvActiveMemberCount.setText(activeCount + " Active Members");

        // Personal Balance Snapshot
        MemberBalanceSheet myBalance = accountingEngine.calculateSingleMemberBalance(messId, sessionManager.getUserId(), startDate, endDate);
        tvMyMeals.setText(myBalance.getConsumedMeals() + " Meals");
        tvMyDeposits.setText(CurrencyUtils.format(myBalance.getTotalDeposit()));
        tvMyTotalCost.setText(CurrencyUtils.format(myBalance.getTotalCost()));
        tvMyBalance.setText(CurrencyUtils.format(myBalance.getNetBalance()));

        if (myBalance.isDue()) {
            tvBalanceBadge.setText("Due: " + CurrencyUtils.format(myBalance.getDueAmount()));
            tvBalanceBadge.setBackgroundResource(R.drawable.badge_due);
            tvBalanceBadge.setTextColor(getResources().getColor(R.color.due_red));
            tvMyBalance.setTextColor(getResources().getColor(R.color.due_red));
        } else {
            tvBalanceBadge.setText("Refundable: " + CurrencyUtils.format(myBalance.getNetBalance()));
            tvBalanceBadge.setBackgroundResource(R.drawable.badge_credit);
            tvBalanceBadge.setTextColor(getResources().getColor(R.color.credit_green));
            tvMyBalance.setTextColor(getResources().getColor(R.color.credit_green));
        }
    }

    private void performSync() {
        chipSyncStatus.setText("Syncing...");
        syncManager.triggerTwoWaySync(new SyncManager.SyncCallback() {
            @Override
            public void onSyncStarted() {
                swipeRefresh.setRefreshing(true);
            }

            @Override
            public void onSyncSuccess(String message) {
                swipeRefresh.setRefreshing(false);
                chipSyncStatus.setText("Cloud Synced");
                Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_SHORT).show();
                loadDashboardData();
            }

            @Override
            public void onSyncFailed(String error) {
                swipeRefresh.setRefreshing(false);
                chipSyncStatus.setText("Offline Mode");
                Toast.makeText(DashboardActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSmsOptionsDialog() {
        String[] options = {"Send Due Reminder to Member", "Broadcast Announcement Notice to All"};
        new AlertDialog.Builder(this)
                .setTitle("সিম এসএমএস ব্যবস্থাপনা")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(this, DueReminderActivity.class));
                    } else {
                        startActivity(new Intent(this, SmsBroadcastActivity.class));
                    }
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sync) {
            performSync();
            return true;
        } else if (id == R.id.action_support) {
            startActivity(new Intent(this, ActivitySupportTickets.class));
            return true;
        } else if (id == R.id.action_report_issue) {
            startActivity(new Intent(this, ActivityReportIssue.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            sessionManager.clearSession();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
